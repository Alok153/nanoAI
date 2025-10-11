package com.vjaykrsna.nanoai.feature.settings.domain.huggingface

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.model.huggingface.network.HuggingFaceAccountService
import com.vjaykrsna.nanoai.model.huggingface.network.HuggingFaceOAuthService
import com.vjaykrsna.nanoai.model.huggingface.network.dto.HuggingFaceOAuthErrorResponse
import com.vjaykrsna.nanoai.model.huggingface.network.dto.HuggingFaceTokenResponse
import com.vjaykrsna.nanoai.security.HuggingFaceCredentialRepository
import com.vjaykrsna.nanoai.security.model.SecretCredential
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

/**
 * Central coordinator for Hugging Face authentication. Manages credential persistence,
 * verification, and state exposure for UI consumers.
 */
@Singleton
class HuggingFaceAuthCoordinator
@Inject
constructor(
  private val credentialRepository: HuggingFaceCredentialRepository,
  private val accountService: HuggingFaceAccountService,
  private val oauthService: HuggingFaceOAuthService,
  private val clock: Clock,
  private val json: Json,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
  private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
  private val mutex = Mutex()
  private val _state = MutableStateFlow(HuggingFaceAuthState.unauthenticated())
  private val _deviceAuthState = MutableStateFlow<HuggingFaceDeviceAuthState?>(null)
  val state: StateFlow<HuggingFaceAuthState> = _state.asStateFlow()
  val deviceAuthState: StateFlow<HuggingFaceDeviceAuthState?> = _deviceAuthState.asStateFlow()

  private var deviceAuthJob: Job? = null
  private var deviceSession: DeviceFlowSession? = null

  init {
    scope.launch { refreshAccount() }
  }

  /** Persist a user-supplied personal access token and re-verify the account. */
  suspend fun savePersonalAccessToken(token: String): Result<HuggingFaceAuthState> =
    withContext(ioDispatcher) {
      mutex.withLock {
        val normalized = token.trim()
        if (normalized.isEmpty()) {
          return@withLock Result.failure(IllegalArgumentException("API token must not be blank"))
        }

        credentialRepository.saveAccessToken(
          token = normalized,
          metadata =
            mapOf(
              METADATA_KEY_ISSUER to DEFAULT_ISSUER,
              METADATA_KEY_SOURCE to HuggingFaceTokenSource.API_TOKEN.id,
            ),
        )

        Result.success(refreshAccountInternal())
      }
    }

  suspend fun beginDeviceAuthorization(
    clientId: String,
    scope: String,
  ): Result<HuggingFaceDeviceAuthState> =
    withContext(ioDispatcher) {
      mutex.withLock {
        if (clientId.isBlank()) {
          return@withLock Result.failure(IllegalStateException("OAuth client id is not configured"))
        }

        // Cancel any in-progress session before starting a new one.
        cancelDeviceSessionLocked()

        val response = runCatching { oauthService.requestDeviceCode(clientId, scope) }
        val deviceResponse =
          response.getOrElse { error ->
            return@withLock Result.failure(error)
          }

        val interval = deviceResponse.interval?.coerceAtLeast(1) ?: DEFAULT_DEVICE_POLL_SECONDS
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
        Result.success(uiState)
      }
    }

  suspend fun cancelDeviceAuthorization() {
    withContext(ioDispatcher) { mutex.withLock { cancelDeviceSessionLocked() } }
  }

  /** Clear any stored credential and reset the auth state. */
  suspend fun clearCredentials(): Unit =
    withContext(ioDispatcher) {
      mutex.withLock {
        credentialRepository.clearAccessToken()
        cancelDeviceSessionLocked()
        _state.value = HuggingFaceAuthState.unauthenticated()
      }
    }

  /** Force a verification of the stored credential (if any). */
  suspend fun refreshAccount(): HuggingFaceAuthState =
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
                )
              }
            }
            is PollingDecision.Stop -> {
              _deviceAuthState.update { state ->
                state?.copy(isPolling = false, lastError = decision.message)
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

    return when (errorCode) {
      ERROR_AUTHORIZATION_PENDING -> PollingDecision.Continue(currentInterval)
      ERROR_SLOW_DOWN ->
        PollingDecision.Continue(
          nextIntervalSeconds =
            min(MAX_DEVICE_POLL_SECONDS, currentInterval + SLOW_DOWN_BACKOFF_SECONDS),
          message = SLOW_DOWN_USER_MESSAGE,
        )
      ERROR_EXPIRED -> PollingDecision.Stop(DEVICE_CODE_EXPIRED_MESSAGE)
      ERROR_ACCESS_DENIED -> PollingDecision.Stop(DEVICE_CODE_DENIED_MESSAGE)
      else -> {
        val description = oauthError?.errorDescription?.takeIf { it.isNotBlank() }
        val message = description ?: throwable.message ?: DEVICE_CODE_GENERIC_ERROR
        PollingDecision.Stop(message)
      }
    }
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
  ): HuggingFaceDeviceAuthState =
    HuggingFaceDeviceAuthState(
      userCode = userCode,
      verificationUri = verificationUri,
      verificationUriComplete = verificationUriComplete,
      expiresAt = expiresAt,
      pollIntervalSeconds = pollIntervalSeconds,
      isPolling = isPolling,
      lastError = lastError,
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
  data class Continue(val nextIntervalSeconds: Int, val message: String? = null) : PollingDecision

  data class Stop(val message: String) : PollingDecision
}
