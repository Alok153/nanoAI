package com.vjaykrsna.nanoai.feature.settings.domain.huggingface

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.security.HuggingFaceCredentialRepository
import com.vjaykrsna.nanoai.security.model.CredentialScope
import com.vjaykrsna.nanoai.security.model.SecretCredential
import com.vjaykrsna.nanoai.shared.model.huggingface.network.HuggingFaceAccountService
import com.vjaykrsna.nanoai.shared.model.huggingface.network.HuggingFaceOAuthService
import com.vjaykrsna.nanoai.shared.model.huggingface.network.dto.HuggingFaceDeviceCodeResponse
import com.vjaykrsna.nanoai.shared.model.huggingface.network.dto.HuggingFaceTokenResponse
import com.vjaykrsna.nanoai.shared.model.huggingface.network.dto.HuggingFaceUserDto
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Response

class HuggingFaceAuthCoordinatorTest {

  @Test
  fun `savePersonalAccessToken persists credentials and updates state`() = runTest {
    val harness = createHarness()
    harness.accountService.response = Result.success(testUser())

    val state = harness.coordinator.savePersonalAccessToken("hf_test").getOrThrow()

    assertThat(state.isAuthenticated).isTrue()
    assertThat(state.username).isEqualTo("tester")
    assertThat(harness.coordinator.state.value.isAuthenticated).isTrue()
    assertThat(harness.credentialRepository.hasAccessToken()).isTrue()
  }

  @Test
  fun `savePersonalAccessToken clears invalid credentials`() = runTest {
    val harness = createHarness()
    harness.accountService.response = Result.failure(unauthorizedException())

    val result = harness.coordinator.savePersonalAccessToken("hf_invalid")

    assertThat(result.getOrThrow().isAuthenticated).isFalse()
    assertThat(harness.coordinator.state.value.lastError).contains("no longer valid")
    assertThat(harness.credentialRepository.hasAccessToken()).isFalse()
  }

  @Test
  fun `device authorization stores token and advances auth state`() = runTest {
    val harness = createHarness()
    harness.accountService.response = Result.success(testUser())
    harness.oauthService.deviceResponse =
      HuggingFaceDeviceCodeResponse(
        deviceCode = "device-code",
        userCode = "USER-CODE",
        verificationUri = "https://huggingface.co/login/device",
        verificationUriComplete = "https://huggingface.co/login/device?user-code=USER-CODE",
        expiresIn = 300,
        interval = 1,
      )
    harness.oauthService.tokenResponses +=
      Result.success(HuggingFaceTokenResponse(accessToken = "oauth-token"))

    val deviceState = harness.coordinator.beginDeviceAuthorization("client", "all").getOrThrow()

    assertThat(deviceState.userCode).isEqualTo("USER-CODE")
    assertThat(harness.coordinator.deviceAuthState.value?.isPolling).isTrue()

    advanceTimeBy(1_000)
    advanceUntilIdle()

    assertThat(harness.credentialRepository.hasAccessToken()).isTrue()
    assertThat(harness.coordinator.state.value.isAuthenticated).isTrue()
    assertThat(harness.coordinator.deviceAuthState.value).isNull()
  }

  @Test
  fun `begin device authorization fails when client id missing`() = runTest {
    val harness = createHarness()

    val result = harness.coordinator.beginDeviceAuthorization("", "all")

    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
  }

  @Test
  fun `cancel device authorization clears device state`() = runTest {
    val harness = createHarness()
    harness.oauthService.tokenResponses +=
      Result.failure(IllegalStateException("authorization_pending"))

    harness.coordinator.beginDeviceAuthorization("client", "all").getOrThrow()
    assertThat(harness.coordinator.deviceAuthState.value).isNotNull()

    harness.coordinator.cancelDeviceAuthorization()

    assertThat(harness.coordinator.deviceAuthState.value).isNull()
    assertThat(harness.coordinator.state.value.isAuthenticated).isFalse()
  }

  @Test
  fun `device polling surfaces accessible message when slow down requested`() = runTest {
    val harness = createHarness()
    harness.accountService.response = Result.success(testUser())
    harness.oauthService.deviceResponse = harness.oauthService.deviceResponse.copy(interval = 1)
    harness.oauthService.tokenResponses += Result.failure(slowDownException())
    harness.oauthService.tokenResponses +=
      Result.success(HuggingFaceTokenResponse(accessToken = "oauth-token"))

    harness.coordinator.beginDeviceAuthorization("client", "all").getOrThrow()

    advanceTimeBy(1_000)
    runCurrent()

    val deviceState = harness.coordinator.deviceAuthState.value
    assertThat(deviceState?.isPolling).isTrue()
    assertThat(deviceState?.lastError).contains("Hugging Face asked us to slow down")
    assertThat(deviceState?.lastErrorAnnouncement).contains("Hugging Face asked us to slow down")
    assertThat(deviceState?.pollIntervalSeconds).isEqualTo(6)

    advanceTimeBy(6_000)
    advanceUntilIdle()

    assertThat(harness.coordinator.state.value.isAuthenticated).isTrue()
  }

  @Test
  fun `refreshAccount without stored credential returns unauthenticated`() = runTest {
    val harness = createHarness()

    val state = harness.coordinator.refreshAccount()

    assertThat(state.isAuthenticated).isFalse()
    assertThat(state.username).isNull()
    assertThat(harness.coordinator.state.value.isAuthenticated).isFalse()
  }

  @Test
  fun `device polling slow down surfaces countdown message`() = runTest {
    val harness = createHarness()
    harness.accountService.response = Result.success(testUser())
    harness.oauthService.deviceResponse = harness.oauthService.deviceResponse.copy(interval = 2)
    harness.oauthService.tokenResponses += Result.failure(slowDownException())

    harness.coordinator.beginDeviceAuthorization("client", "all").getOrThrow()

    advanceTimeBy(2_000)
    runCurrent()

    val deviceState = harness.coordinator.deviceAuthState.value
    assertThat(deviceState?.pollIntervalSeconds).isEqualTo(7)
    assertThat(deviceState?.lastError).contains("Retrying in 7 seconds")
    assertThat(deviceState?.lastErrorAnnouncement).contains("Retrying in 7 seconds")
  }

  @Test
  fun `offline polling failures stop retries with offline guidance`() = runTest {
    val harness = createHarness()
    harness.accountService.response = Result.success(testUser())
    harness.oauthService.tokenResponses += Result.failure(IOException("failed to connect"))

    harness.coordinator.beginDeviceAuthorization("client", "all").getOrThrow()

    advanceTimeBy(1_000)
    advanceUntilIdle()

    val deviceState = harness.coordinator.deviceAuthState.value
    assertThat(deviceState?.isPolling).isFalse()
    assertThat(deviceState?.lastError).contains("offline")
    assertThat(deviceState?.lastErrorAnnouncement).contains("offline")
  }

  private fun testUser() =
    HuggingFaceUserDto(
      name = "tester",
      displayName = "Test User",
      email = null,
      accountType = "user",
    )

  private fun unauthorizedException(): HttpException {
    val body = "{\"error\":\"invalid_token\"}".toResponseBody("application/json".toMediaType())
    val response = Response.error<HuggingFaceUserDto>(401, body)
    return HttpException(response)
  }

  private fun slowDownException(): HttpException {
    val body =
      "{\"error\":\"slow_down\",\"error_description\":\"Back off and retry later\"}"
        .toResponseBody("application/json".toMediaType())
    val response = Response.error<HuggingFaceTokenResponse>(429, body)
    return HttpException(response)
  }

  private fun TestScope.createHarness(): CoordinatorHarness {
    val json = Json {
      ignoreUnknownKeys = true
      encodeDefaults = true
      explicitNulls = false
    }
    val clock = TestClock(testScheduler)
    var storedCredential: SecretCredential? = null
    val credentialRepository = mockk<HuggingFaceCredentialRepository>()
    every { credentialRepository.credential() } answers { storedCredential }
    every { credentialRepository.accessToken() } answers { storedCredential?.encryptedValue }
    every { credentialRepository.hasAccessToken() } answers
      {
        storedCredential?.encryptedValue?.isNotBlank() == true
      }
    every {
      credentialRepository.saveAccessToken(token = any(), rotatesAfter = any(), metadata = any())
    } answers
      {
        val token = firstArg<String>()
        val rotatesAfter = secondArg<Instant?>()
        val metadata = thirdArg<Map<String, String>>()
        storedCredential =
          SecretCredential(
            providerId = HuggingFaceCredentialRepository.PROVIDER_ID,
            encryptedValue = token,
            keyAlias = "test",
            storedAt = clock.now(),
            rotatesAfter = rotatesAfter,
            scope = CredentialScope.TEXT_INFERENCE,
            metadata = metadata,
          )
        Unit
      }
    every { credentialRepository.clearAccessToken() } answers
      {
        storedCredential = null
        Unit
      }
    val accountService = FakeAccountService()
    val oauthService = FakeOAuthService()
    val dispatcher = StandardTestDispatcher(testScheduler)
    val coordinator =
      HuggingFaceAuthCoordinator(
        credentialRepository = credentialRepository,
        accountService = accountService,
        oauthService = oauthService,
        clock = clock,
        json = json,
        ioDispatcher = dispatcher,
      )
    return CoordinatorHarness(coordinator, accountService, oauthService, credentialRepository)
  }

  private data class CoordinatorHarness(
    val coordinator: HuggingFaceAuthCoordinator,
    val accountService: FakeAccountService,
    val oauthService: FakeOAuthService,
    val credentialRepository: HuggingFaceCredentialRepository,
  )

  private class FakeAccountService : HuggingFaceAccountService {
    var response: Result<HuggingFaceUserDto> =
      Result.success(
        HuggingFaceUserDto(name = "fallback", displayName = null, email = null, accountType = null)
      )

    override suspend fun getCurrentUser(): HuggingFaceUserDto = response.getOrThrow()
  }

  private class FakeOAuthService : HuggingFaceOAuthService {
    var deviceResponse: HuggingFaceDeviceCodeResponse =
      HuggingFaceDeviceCodeResponse(
        deviceCode = "device",
        userCode = "code",
        verificationUri = "https://example.com",
        verificationUriComplete = null,
        expiresIn = 300,
        interval = 1,
      )

    val tokenResponses: MutableList<Result<HuggingFaceTokenResponse>> = mutableListOf()

    override suspend fun requestDeviceCode(
      clientId: String,
      scope: String,
    ): HuggingFaceDeviceCodeResponse = deviceResponse

    override suspend fun exchangeDeviceCode(
      clientId: String,
      deviceCode: String,
      grantType: String,
    ): HuggingFaceTokenResponse {
      if (tokenResponses.isEmpty()) {
        throw HttpException(
          Response.error<HuggingFaceTokenResponse>(
            428,
            "{\"error\":\"authorization_pending\"}".toResponseBody("application/json".toMediaType()),
          )
        )
      }
      return tokenResponses.removeAt(0).getOrThrow()
    }
  }

  private class TestClock(private val scheduler: TestCoroutineScheduler) : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(scheduler.currentTime)
  }
}
