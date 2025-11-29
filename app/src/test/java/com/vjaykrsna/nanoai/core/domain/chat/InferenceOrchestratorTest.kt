package com.vjaykrsna.nanoai.core.domain.chat

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.library.ModelCatalogRepository
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.domain.model.InferencePreference
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.library.DeliveryType
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import com.vjaykrsna.nanoai.core.domain.repository.ApiProviderConfigRepository
import com.vjaykrsna.nanoai.core.domain.repository.InferencePreferenceRepository
import com.vjaykrsna.nanoai.core.model.APIType
import com.vjaykrsna.nanoai.core.model.InferenceMode
import com.vjaykrsna.nanoai.core.model.MessageSource
import com.vjaykrsna.nanoai.core.network.CloudGatewayClient
import com.vjaykrsna.nanoai.core.network.CloudGatewayResult
import com.vjaykrsna.nanoai.core.network.ConnectivityStatusProvider
import com.vjaykrsna.nanoai.core.network.dto.CompletionChoiceDto
import com.vjaykrsna.nanoai.core.network.dto.CompletionChoiceMessageDto
import com.vjaykrsna.nanoai.core.network.dto.CompletionResponseDto
import com.vjaykrsna.nanoai.core.runtime.LocalGenerationResult
import com.vjaykrsna.nanoai.core.runtime.LocalModelRuntime
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InferenceOrchestratorTest {
  @MockK private lateinit var modelCatalogRepository: ModelCatalogRepository
  @MockK private lateinit var apiProviderConfigRepository: ApiProviderConfigRepository
  @MockK private lateinit var inferencePreferenceRepository: InferencePreferenceRepository
  @MockK private lateinit var localModelRuntime: LocalModelRuntime
  @MockK private lateinit var cloudGatewayClient: CloudGatewayClient
  @MockK private lateinit var connectivityStatusProvider: ConnectivityStatusProvider

  private lateinit var orchestrator: InferenceOrchestrator
  private lateinit var inferencePreferenceFlow: MutableStateFlow<InferencePreference>

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this)
    inferencePreferenceFlow = MutableStateFlow(InferencePreference(InferenceMode.LOCAL_FIRST))
    every { inferencePreferenceRepository.observeInferencePreference() } returns
      inferencePreferenceFlow

    orchestrator =
      InferenceOrchestrator(
        modelCatalogRepository = modelCatalogRepository,
        apiProviderConfigRepository = apiProviderConfigRepository,
        inferencePreferenceRepository = inferencePreferenceRepository,
        localModelRuntime = localModelRuntime,
        cloudGatewayClient = cloudGatewayClient,
        connectivityStatusProvider = connectivityStatusProvider,
      )
  }

  @Test
  fun `hasLocalModelAvailable returns false when no local models`() = runTest {
    coEvery { modelCatalogRepository.getInstalledModels() } returns listOf(cloudModel())

    val result = orchestrator.hasLocalModelAvailable()

    assertFalse(result)
    coVerify(exactly = 0) { localModelRuntime.hasReadyModel(any()) }
  }

  @Test
  fun `hasLocalModelAvailable returns true when runtime has ready model`() = runTest {
    val model = localModel()
    coEvery { modelCatalogRepository.getInstalledModels() } returns listOf(model)
    coEvery { localModelRuntime.hasReadyModel(listOf(model)) } returns true

    val result = orchestrator.hasLocalModelAvailable()

    assertTrue(result)
  }

  @Test
  fun `generateResponse prefers local runtime and stamps persona metadata`() = runTest {
    val personaId = UUID.randomUUID()
    val localModel = localModel(modelId = "nano.local")
    inferencePreferenceFlow.value = InferencePreference(InferenceMode.LOCAL_FIRST)
    coEvery { modelCatalogRepository.getInstalledModels() } returns listOf(localModel)
    coEvery { connectivityStatusProvider.isOnline() } returns true
    coEvery { localModelRuntime.isModelReady(localModel.modelId) } returns true
    coEvery { localModelRuntime.generate(any()) } returns
      NanoAIResult.success(LocalGenerationResult(text = "hello", latencyMs = 42))

    val result = orchestrator.generateResponse(prompt = "Hello", personaId = personaId)

    val success = assertIs<NanoAIResult.Success<InferenceSuccessData>>(result)
    assertEquals("hello", success.value.text)
    assertEquals(MessageSource.LOCAL_MODEL, success.value.source)
    assertEquals(personaId.toString(), success.value.metadata["personaId"])
    assertEquals(localModel.modelId, success.value.metadata["modelId"])
  }

  @Test
  fun `generateResponse falls back to cloud when local fails and device online`() = runTest {
    val localModel = localModel(modelId = "nano.local")
    val cloudModel = cloudModel(modelId = "cloud.text")
    val provider = apiProviderConfig()

    inferencePreferenceFlow.value = InferencePreference(InferenceMode.LOCAL_FIRST)
    coEvery { modelCatalogRepository.getInstalledModels() } returns listOf(localModel)
    coEvery { modelCatalogRepository.getAllModels() } returns listOf(cloudModel)
    coEvery { connectivityStatusProvider.isOnline() } returns true
    coEvery { localModelRuntime.isModelReady(localModel.modelId) } returns true
    coEvery { localModelRuntime.generate(any()) } returns
      NanoAIResult.recoverable(message = "Local failure", telemetryId = "LOCAL_FAIL")
    coEvery { apiProviderConfigRepository.getEnabledProviders() } returns listOf(provider)
    coEvery { cloudGatewayClient.createCompletion(provider, any()) } returns
      CloudGatewayResult.Success(
        CompletionResponseDto(
          id = "req-1",
          created = 0L,
          model = cloudModel.modelId,
          choices =
            listOf(
              CompletionChoiceDto(
                index = 0,
                message = CompletionChoiceMessageDto(content = "cloud"),
                finishReason = "stop",
              )
            ),
          usage = null,
        ),
        latencyMs = 120,
      )

    val result = orchestrator.generateResponse(prompt = "Hello", personaId = null)

    val success = assertIs<NanoAIResult.Success<InferenceSuccessData>>(result)
    assertEquals(MessageSource.CLOUD_API, success.value.source)
    assertEquals("cloud", success.value.text)
    assertEquals(provider.providerId, success.value.metadata["providerId"])
    assertEquals(cloudModel.modelId, success.value.metadata["modelId"])
  }

  @Test
  fun `generateResponse returns offline error when cloud preferred but offline`() = runTest {
    inferencePreferenceFlow.value = InferencePreference(InferenceMode.CLOUD_FIRST)
    coEvery { modelCatalogRepository.getInstalledModels() } returns emptyList()
    coEvery { connectivityStatusProvider.isOnline() } returns false

    val result = orchestrator.generateResponse(prompt = "Need help", personaId = null)

    val recoverable = assertIs<NanoAIResult.RecoverableError>(result)
    assertEquals("OFFLINE", recoverable.telemetryId)
  }

  @Test
  fun `generateResponse reports configuration error when no provider enabled`() = runTest {
    val cloudModel = cloudModel()
    inferencePreferenceFlow.value = InferencePreference(InferenceMode.CLOUD_FIRST)
    coEvery { modelCatalogRepository.getInstalledModels() } returns emptyList()
    coEvery { modelCatalogRepository.getAllModels() } returns listOf(cloudModel)
    coEvery { connectivityStatusProvider.isOnline() } returns true
    coEvery { apiProviderConfigRepository.getEnabledProviders() } returns emptyList()

    val result = orchestrator.generateResponse(prompt = "Need help", personaId = null)

    val recoverable = assertIs<NanoAIResult.RecoverableError>(result)
    assertEquals("NO_CLOUD_PROVIDER", recoverable.telemetryId)
  }

  private fun localModel(modelId: String = "local-model"): ModelPackage =
    modelPackage(modelId = modelId, providerType = ProviderType.MEDIA_PIPE)

  private fun cloudModel(modelId: String = "cloud-model"): ModelPackage =
    modelPackage(modelId = modelId, providerType = ProviderType.CLOUD_API)

  private fun modelPackage(modelId: String, providerType: ProviderType): ModelPackage =
    ModelPackage(
      modelId = modelId,
      displayName = modelId,
      version = "1.0",
      providerType = providerType,
      deliveryType = DeliveryType.LOCAL_ARCHIVE,
      minAppVersion = 1,
      sizeBytes = 1,
      capabilities = emptySet(),
      installState = InstallState.INSTALLED,
      manifestUrl = "https://example.com/$modelId",
      createdAt = Instant.parse("2024-01-01T00:00:00Z"),
      updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
    )

  private fun apiProviderConfig(): APIProviderConfig =
    APIProviderConfig(
      providerId = "provider-1",
      providerName = "Test",
      baseUrl = "https://api.example.com",
      apiType = APIType.OPENAI_COMPATIBLE,
      isEnabled = true,
      quotaResetAt = null,
      lastStatus = com.vjaykrsna.nanoai.core.model.ProviderStatus.UNKNOWN,
      credentialId = "cred",
    )
}
