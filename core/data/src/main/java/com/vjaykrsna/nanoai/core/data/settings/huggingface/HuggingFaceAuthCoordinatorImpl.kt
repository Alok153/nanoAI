package com.vjaykrsna.nanoai.core.data.settings.huggingface

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.data.library.huggingface.network.HuggingFaceAccountService
import com.vjaykrsna.nanoai.core.data.library.huggingface.network.HuggingFaceOAuthService
import com.vjaykrsna.nanoai.core.data.library.huggingface.network.dto.HuggingFaceOAuthErrorResponse
import com.vjaykrsna.nanoai.core.data.library.huggingface.network.dto.HuggingFaceTokenResponse
import com.vjaykrsna.nanoai.core.domain.settings.huggingface.HuggingFaceAuthCoordinator
import com.vjaykrsna.nanoai.core.domain.settings.huggingface.HuggingFaceAuthState
import com.vjaykrsna.nanoai.core.domain.settings.huggingface.HuggingFaceDeviceAuthState
import com.vjaykrsna.nanoai.core.domain.settings.huggingface.HuggingFaceTokenSource
import com.vjaykrsna.nanoai.core.security.HuggingFaceCredentialRepository
import com.vjaykrsna.nanoai.core.security.model.SecretCredential
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.plus
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import retrofit2.HttpException

private const val METADATA_KEY_SOURCE = "source"
private const val METADATA_KEY_ISSUER = "issuer"
private const val DEFAULT_ISSUER = "huggingface"
private const val DEFAULT_DEVICE_POLL_SECONDS = 5
private const val MAX_DEVICE_POLL_SECONDS = 30
private const val SLOW_DOWN_BACKOFF_SECONDS = 5
private const val ERROR_AUTHORIZATION_PENDING = "authorization_pending"
private const val ERROR_SLOW_DOWN = "slow_down"
private const val ERROR_EXPIRED = "expired_token"
private const val ERROR_ACCESS_DENIED = "access_denied"
private const val SLOW_DOWN_USER_MESSAGE =
  "Hugging Face asked us to slow down. We'll retry in a moment."
private const val DEVICE_CODE_EXPIRED_MESSAGE =
  "Device code expired before confirmation. Start a new Hugging Face sign-in."
private const val DEVICE_CODE_DENIED_MESSAGE =
  "Authorization was denied on Hugging Face. Try again if this was unintentional."
private const val DEVICE_CODE_GENERIC_ERROR =
  "Failed to complete Hugging Face sign-in. Check your connection and try again."
private const val OFFLINE_DEVICE_MESSAGE =
  "Device appears offline. Check your connection and try again before retrying the sign-in."
private const val TOKEN_BLANK_ERROR = "Hugging Face API token must not be blank"
private const val TOKEN_SAVE_FAILURE = "Unable to save Hugging Face API token"
private const val CLIENT_ID_MISSING_ERROR = "Hugging Face OAuth client ID is not configured"

/**
 * Central coordinator for Hugging Face authentication. Manages credential persistence,
 * verification, and state exposure for UI consumers.
 */
@Singleton
class HuggingFaceAuthCoordinatorImpl
@Inject
constructor(
  private val credentialRepository: HuggingFaceCredentialRepository,
  private val accountService: HuggingFaceAccountService,
  private val oauthService: HuggingFaceOAuthService,
  private val clock: Clock,
  private val json: Json,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : HuggingFaceAuthCoordinator {
  private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
  private val mutex = Mutex()
  private val _state = MutableStateFlow(HuggingFaceAuthState.unauthenticated())
  private val _deviceAuthState = MutableStateFlow<HuggingFaceDeviceAuthState?>(null)
  override val state: StateFlow<HuggingFaceAuthState> = _state.asStateFlow()
  override val deviceAuthState: StateFlow<HuggingFaceDeviceAuthState?> =
    _deviceAuthState.asStateFlow()

  private var deviceAuthJob: Job? = null
  private var deviceSession: DeviceFlowSession? = null

  init {
    scope.launch { refreshAccount() }
  }

  /** Persist a user-supplied personal access token and re-verify the account. */
  override suspend fun savePersonalAccessToken(token: String): NanoAIResult<HuggingFaceAuthState> =
    withContext(ioDispatcher) {
      mutex.withLock {
        val normalized = token.trim()
        if (normalized.isEmpty()) {
          return@withLock NanoAIResult.recoverable(
            message = TOKEN_BLANK_ERROR,
            context = mapOf("source" to HuggingFaceTokenSource.API_TOKEN.id),
          )
        }

        runCatching {
            credentialRepository.saveAccessToken(
              token = normalized,
              rotatesAfter = null,
              metadata =
                mapOf(
                  METADATA_KEY_ISSUER to DEFAULT_ISSUER,
                  METADATA_KEY_SOURCE to HuggingFaceTokenSource.API_TOKEN.id,
                ),
            )
            refreshAccountInternal()
          }
          .fold(
            onSuccess = { state -> NanoAIResult.success(state) },
            onFailure = { throwable ->
              credentialRepository.clearAccessToken()
              NanoAIResult.recoverable(
                message = throwable.message ?: TOKEN_SAVE_FAILURE,
                cause = throwable,
                context = mapOf("operation" to "savePersonalAccessToken"),
              )
            },
          )
      }
    }

  override suspend fun beginDeviceAuthorization(
    clientId: String,
    scope: String,
  ): NanoAIResult<HuggingFaceDeviceAuthState> =
    withContext(ioDispatcher) {
      mutex.withLock {
        if (clientId.isBlank()) {
          return@withLock NanoAIResult.recoverable(
            message = CLIENT_ID_MISSING_ERROR,
            context = mapOf("operation" to "beginDeviceAuthorization"),
          )
        }

        cancelDeviceSessionLocked()

        runCatching { oauthService.requestDeviceCode(clientId, scope) }
          .fold(
            onSuccess = { deviceResponse ->
              val interval =
                deviceResponse.interval?.coerceAtLeast(1) ?: DEFAULT_DEVICE_POLL_SECONDS
              val session =
                DeviceFlowSession(
                  clientId = clientId,
                  deviceCode = deviceResponse.deviceCode,
                  userCode = deviceResponse.userCode,
                  verificationUri = deviceResponse.verificationUri,
                  verificationUriComplete = deviceResponse.verificationUriComplete,
                  expiresAt = clock.now().plus(deviceResponse.expiresIn.seconds),
                  pollIntervalSeconds = interval,
                )

              deviceSession = session
              deviceAuthJob = startDevicePolling(session)
              val uiState = session.toUiState(isPolling = true)
              _deviceAuthState.value = uiState
              NanoAIResult.success(uiState)
            },
            onFailure = { throwable ->
              NanoAIResult.recoverable(
                message = throwable.message ?: DEVICE_CODE_GENERIC_ERROR,
                cause = throwable,
                context = mapOf("operation" to "beginDeviceAuthorization"),
              )
            },
          )
      }
    }

  override suspend fun cancelDeviceAuthorization() {
    withContext(ioDispatcher) { mutex.withLock { cancelDeviceSessionLocked() } }
  }

  /** Clear any stored credential and reset the auth state. */
  override suspend fun clearCredentials(): Unit =
    withContext(ioDispatcher) {
      mutex.withLock {
        credentialRepository.clearAccessToken()
        cancelDeviceSessionLocked()
        _state.value = HuggingFaceAuthState.unauthenticated()
      }
    }

  /** Force a verification of the stored credential (if any). */
  override suspend fun refreshAccount(): HuggingFaceAuthState =
    withContext(ioDispatcher) { mutex.withLock { refreshAccountInternal() } }

  private suspend fun refreshAccountInternal(): HuggingFaceAuthState {
    val credential = credentialRepository.credential()
    if (credential == null || credential.encryptedValue.isBlank()) {
      val unauthenticated = HuggingFaceAuthState.unauthenticated()
      _state.value = unauthenticated
      return unauthenticated
    }

    _state.update { it.copy(isVerifying = true, lastError = null) }

    return runCatching { accountService.getCurrentUser() }
      .map { userDto ->
        cancelDeviceSessionLocked()
        HuggingFaceAuthState(
          isAuthenticated = true,
          username = userDto.name,
          displayName = userDto.displayName,
          avatarUrl = userDto.avatarUrl,
          tokenSource = credential.tokenSource(),
          lastVerifiedAt = clock.now(),
          isVerifying = false,
          lastError = null,
        )
      }
      .getOrElse { throwable ->
        val handled = handleVerificationFailure(throwable, credential)
        handled
      }
      .also { newState -> _state.value = newState }
  }

  private fun startDevicePolling(session: DeviceFlowSession): Job =
    scope.launch {
      var currentInterval = session.pollIntervalSeconds
      while (isActive) {
        val now = clock.now()
        if (now >= session.expiresAt) {
          _deviceAuthState.update {
            it?.copy(isPolling = false, lastError = DEVICE_CODE_EXPIRED_MESSAGE)
          }
          deviceSession = null
          deviceAuthJob = null
          break
        }

        delay(currentInterval.seconds)

        val result = runCatching {
          oauthService.exchangeDeviceCode(session.clientId, session.deviceCode)
        }

        result.onSuccess { tokenResponse ->
          mutex.withLock {
            persistOAuthToken(tokenResponse)
            deviceSession = null
            _deviceAuthState.value = null
            refreshAccountInternal()
          }
          deviceAuthJob = null
          return@launch
        }

        result.onFailure { throwable ->
          val decision = handleDevicePollingFailure(throwable, currentInterval)
          when (decision) {
            is PollingDecision.Continue -> {
              currentInterval = decision.nextIntervalSeconds
              _deviceAuthState.update { state ->
                state?.copy(
                  pollIntervalSeconds = currentInterval,
                  isPolling = true,
                  lastError = decision.message ?: state.lastError,
                  lastErrorAnnouncement = decision.announcement ?: state.lastErrorAnnouncement,
                )
              }
            }
            is PollingDecision.Stop -> {
              _deviceAuthState.update { state ->
                state?.copy(
                  isPolling = false,
                  lastError = decision.message,
                  lastErrorAnnouncement = decision.announcement ?: decision.message,
                )
              }
              deviceSession = null
              deviceAuthJob = null
              return@launch
            }
          }
        }
      }
    }

  private suspend fun persistOAuthToken(response: HuggingFaceTokenResponse) {
    credentialRepository.saveAccessToken(
      token = response.accessToken,
      rotatesAfter = null,
      metadata =
        mapOf(
          METADATA_KEY_ISSUER to DEFAULT_ISSUER,
          METADATA_KEY_SOURCE to HuggingFaceTokenSource.OAUTH.id,
        ),
    )
  }

  private fun handleVerificationFailure(
    throwable: Throwable,
    credential: SecretCredential,
  ): HuggingFaceAuthState {
    val isUnauthorized = (throwable as? HttpException)?.code() == UNAUTHORIZED
    if (isUnauthorized) {
      credentialRepository.clearAccessToken()
    }

    val errorMessage =
      when {
        isUnauthorized -> "The saved Hugging Face credential is no longer valid."
        throwable.message.isNullOrBlank() -> "Unable to verify Hugging Face authentication."
        else -> throwable.message!!
      }

    return HuggingFaceAuthState(
      isAuthenticated = false,
      username = null,
      displayName = null,
      avatarUrl = null,
      tokenSource = if (isUnauthorized) HuggingFaceTokenSource.NONE else credential.tokenSource(),
      lastVerifiedAt = credential.storedAt,
      isVerifying = false,
      lastError = errorMessage,
    )
  }

  private fun SecretCredential.tokenSource(): HuggingFaceTokenSource =
    HuggingFaceTokenSource.fromId(metadata[METADATA_KEY_SOURCE])

  private fun handleDevicePollingFailure(
    throwable: Throwable,
    currentInterval: Int,
  ): PollingDecision {
    val oauthError = parseOAuthError(throwable)
    val errorCode = oauthError?.error

    if (throwable is IOException) {
      return offlinePollingDecision()
    }

    return when (errorCode) {
      ERROR_AUTHORIZATION_PENDING -> retryPolling(currentInterval)
      ERROR_SLOW_DOWN -> slowDownPolling(currentInterval)
      ERROR_EXPIRED -> expiredPollingDecision()
      ERROR_ACCESS_DENIED -> accessDeniedPollingDecision()
      else -> genericPollingFailure(oauthError, throwable)
    }
  }

  private fun retryPolling(currentInterval: Int): PollingDecision =
    PollingDecision.Continue(nextIntervalSeconds = currentInterval)

  private fun slowDownPolling(currentInterval: Int): PollingDecision {
    val nextIntervalSeconds =
      min(MAX_DEVICE_POLL_SECONDS, currentInterval + SLOW_DOWN_BACKOFF_SECONDS)
    val countdownMessage = "Retrying in $nextIntervalSeconds seconds."
    val message = "$SLOW_DOWN_USER_MESSAGE $countdownMessage"
    return PollingDecision.Continue(
      nextIntervalSeconds = nextIntervalSeconds,
      message = message,
      announcement = message,
    )
  }

  private fun expiredPollingDecision(): PollingDecision =
    PollingDecision.Stop(
      message = DEVICE_CODE_EXPIRED_MESSAGE,
      announcement = DEVICE_CODE_EXPIRED_MESSAGE,
    )

  private fun accessDeniedPollingDecision(): PollingDecision =
    PollingDecision.Stop(
      message = DEVICE_CODE_DENIED_MESSAGE,
      announcement = DEVICE_CODE_DENIED_MESSAGE,
    )

  private fun offlinePollingDecision(): PollingDecision =
    PollingDecision.Stop(message = OFFLINE_DEVICE_MESSAGE, announcement = OFFLINE_DEVICE_MESSAGE)

  private fun genericPollingFailure(
    oauthError: HuggingFaceOAuthErrorResponse?,
    throwable: Throwable,
  ): PollingDecision {
    val description = oauthError?.errorDescription?.takeIf { it.isNotBlank() }
    val message = description ?: throwable.message ?: DEVICE_CODE_GENERIC_ERROR
    return PollingDecision.Stop(message = message, announcement = message)
  }

  private fun parseOAuthError(throwable: Throwable): HuggingFaceOAuthErrorResponse? {
    val rawBody = (throwable as? HttpException)?.response()?.errorBody()?.string() ?: return null
    return runCatching { json.decodeFromString<HuggingFaceOAuthErrorResponse>(rawBody) }.getOrNull()
  }

  private fun cancelDeviceSessionLocked() {
    deviceAuthJob?.cancel()
    deviceAuthJob = null
    deviceSession = null
    _deviceAuthState.value = null
  }

  private fun DeviceFlowSession.toUiState(
    isPolling: Boolean,
    lastError: String? = null,
    lastErrorAnnouncement: String? = null,
  ): HuggingFaceDeviceAuthState =
    HuggingFaceDeviceAuthState(
      userCode = userCode,
      verificationUri = verificationUri,
      verificationUriComplete = verificationUriComplete,
      expiresAt = expiresAt,
      pollIntervalSeconds = pollIntervalSeconds,
      isPolling = isPolling,
      lastError = lastError,
      lastErrorAnnouncement = lastErrorAnnouncement,
    )

  companion object {
    private const val UNAUTHORIZED = 401
  }
}

private data class DeviceFlowSession(
  val clientId: String,
  val deviceCode: String,
  val userCode: String,
  val verificationUri: String,
  val verificationUriComplete: String?,
  val expiresAt: Instant,
  val pollIntervalSeconds: Int,
)

private sealed interface PollingDecision {
  data class Continue(
    val nextIntervalSeconds: Int,
    val message: String? = null,
    val announcement: String? = null,
  ) : PollingDecision

  data class Stop(val message: String, val announcement: String? = null) : PollingDecision
}
