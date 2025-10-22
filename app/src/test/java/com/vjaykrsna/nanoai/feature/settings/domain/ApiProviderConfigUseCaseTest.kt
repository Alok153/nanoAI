package com.vjaykrsna.nanoai.feature.settings.domain

import com.vjaykrsna.nanoai.core.data.repository.ApiProviderConfigRepository
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.model.APIType
import com.vjaykrsna.nanoai.core.model.ProviderStatus
import com.vjaykrsna.nanoai.testing.assertRecoverableError
import com.vjaykrsna.nanoai.testing.assertSuccess
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Test

class ApiProviderConfigUseCaseTest {
  private lateinit var useCase: ApiProviderConfigUseCase
  private lateinit var apiProviderConfigRepository: ApiProviderConfigRepository

  @Before
  fun setup() {
    apiProviderConfigRepository = mockk(relaxed = true)

    useCase = ApiProviderConfigUseCase(apiProviderConfigRepository)
  }

  @Test
  fun `getAllProviders returns success with provider list`() = runTest {
    val providers =
      listOf(
        APIProviderConfig(
          providerId = "openai",
          providerName = "OpenAI",
          baseUrl = "https://api.openai.com",
          apiKey = "sk-test123",
          apiType = APIType.OPENAI_COMPATIBLE,
          isEnabled = true,
          quotaResetAt = Instant.parse("2024-01-01T00:00:00Z"),
          lastStatus = ProviderStatus.OK,
        )
      )
    coEvery { apiProviderConfigRepository.getAllProviders() } returns providers

    val result = useCase.getAllProviders()

    val returnedProviders = result.assertSuccess()
    assert(returnedProviders == providers)
  }

  @Test
  fun `getAllProviders returns recoverable error when repository fails`() = runTest {
    val exception = RuntimeException("Database error")
    coEvery { apiProviderConfigRepository.getAllProviders() } throws exception

    val result = useCase.getAllProviders()

    result.assertRecoverableError()
  }

  @Test
  fun `getProvider returns success with provider when found`() = runTest {
    val providerId = "openai"
    val provider =
      APIProviderConfig(
        providerId = providerId,
        providerName = "OpenAI",
        baseUrl = "https://api.openai.com",
        apiKey = "sk-test123",
        apiType = APIType.OPENAI_COMPATIBLE,
      )
    coEvery { apiProviderConfigRepository.getProvider(providerId) } returns provider

    val result = useCase.getProvider(providerId)

    val returnedProvider = result.assertSuccess()
    assert(returnedProvider == provider)
  }

  @Test
  fun `getProvider returns success with null when not found`() = runTest {
    val providerId = "nonexistent"
    coEvery { apiProviderConfigRepository.getProvider(providerId) } returns null

    val result = useCase.getProvider(providerId)

    val returnedProvider = result.assertSuccess()
    assert(returnedProvider == null)
  }

  @Test
  fun `getProvider returns recoverable error when repository fails`() = runTest {
    val providerId = "openai"
    val exception = RuntimeException("Database error")
    coEvery { apiProviderConfigRepository.getProvider(providerId) } throws exception

    val result = useCase.getProvider(providerId)

    result.assertRecoverableError()
  }

  @Test
  fun `addProvider returns success when repository succeeds`() = runTest {
    val config =
      APIProviderConfig(
        providerId = "gemini",
        providerName = "Google Gemini",
        baseUrl = "https://generativelanguage.googleapis.com",
        apiKey = "AIzaSyTest",
        apiType = APIType.GEMINI,
      )
    coEvery { apiProviderConfigRepository.addProvider(config) } returns Unit

    val result = useCase.addProvider(config)

    result.assertSuccess()
  }

  @Test
  fun `addProvider returns recoverable error when repository fails`() = runTest {
    val config =
      APIProviderConfig(
        providerId = "gemini",
        providerName = "Google Gemini",
        baseUrl = "https://generativelanguage.googleapis.com",
        apiKey = "AIzaSyTest",
        apiType = APIType.GEMINI,
      )
    val exception = RuntimeException("Database error")
    coEvery { apiProviderConfigRepository.addProvider(config) } throws exception

    val result = useCase.addProvider(config)

    result.assertRecoverableError()
  }

  @Test
  fun `updateProvider returns success when repository succeeds`() = runTest {
    val config =
      APIProviderConfig(
        providerId = "openai",
        providerName = "OpenAI Updated",
        baseUrl = "https://api.openai.com",
        apiKey = "sk-updated123",
        apiType = APIType.OPENAI_COMPATIBLE,
      )
    coEvery { apiProviderConfigRepository.updateProvider(config) } returns Unit

    val result = useCase.updateProvider(config)

    result.assertSuccess()
  }

  @Test
  fun `updateProvider returns recoverable error when repository fails`() = runTest {
    val config =
      APIProviderConfig(
        providerId = "openai",
        providerName = "OpenAI Updated",
        baseUrl = "https://api.openai.com",
        apiKey = "sk-updated123",
        apiType = APIType.OPENAI_COMPATIBLE,
      )
    val exception = RuntimeException("Database error")
    coEvery { apiProviderConfigRepository.updateProvider(config) } throws exception

    val result = useCase.updateProvider(config)

    result.assertRecoverableError()
  }

  @Test
  fun `deleteProvider returns success when repository succeeds`() = runTest {
    val providerId = "openai"
    coEvery { apiProviderConfigRepository.deleteProvider(providerId) } returns Unit

    val result = useCase.deleteProvider(providerId)

    result.assertSuccess()
  }

  @Test
  fun `deleteProvider returns recoverable error when repository fails`() = runTest {
    val providerId = "openai"
    val exception = RuntimeException("Database error")
    coEvery { apiProviderConfigRepository.deleteProvider(providerId) } throws exception

    val result = useCase.deleteProvider(providerId)

    result.assertRecoverableError()
  }
}
