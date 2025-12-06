package com.vjaykrsna.nanoai.core.runtime

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.library.ModelCatalogRepository
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.library.DeliveryType
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LocalModelRuntimeImplTest {

  private lateinit var modelCatalogRepository: ModelCatalogRepository
  private lateinit var mediaPipeInferenceService: MediaPipeInferenceService
  private lateinit var leapInferenceService: LeapInferenceService
  private lateinit var runtime: LocalModelRuntimeImpl

  @BeforeEach
  fun setUp() {
    modelCatalogRepository = mockk()
    mediaPipeInferenceService = mockk()
    leapInferenceService = mockk()
    runtime =
      LocalModelRuntimeImpl(
        modelCatalogRepository,
        mediaPipeInferenceService,
        leapInferenceService,
      )
  }

  @Test
  fun `isModelReady returns false when model not found`() = runTest {
    coEvery { modelCatalogRepository.getModel(any()) } returns null

    val result = runtime.isModelReady("unknown-model")

    assertThat(result).isFalse()
  }

  @Test
  fun `isModelReady delegates to MediaPipe for MEDIA_PIPE provider`() = runTest {
    val model = createTestModel(ProviderType.MEDIA_PIPE)
    coEvery { modelCatalogRepository.getModel("test-model") } returns model
    coEvery { mediaPipeInferenceService.isModelReady("test-model") } returns true

    val result = runtime.isModelReady("test-model")

    assertThat(result).isTrue()
    coVerify { mediaPipeInferenceService.isModelReady("test-model") }
  }

  @Test
  fun `isModelReady delegates to Leap for LEAP provider`() = runTest {
    val model = createTestModel(ProviderType.LEAP)
    coEvery { modelCatalogRepository.getModel("test-model") } returns model
    coEvery { leapInferenceService.isModelReady("test-model") } returns true

    val result = runtime.isModelReady("test-model")

    assertThat(result).isTrue()
    coVerify { leapInferenceService.isModelReady("test-model") }
  }

  @Test
  fun `isModelReady returns false for unsupported provider`() = runTest {
    val model = createTestModel(ProviderType.CLOUD_API)
    coEvery { modelCatalogRepository.getModel("test-model") } returns model

    val result = runtime.isModelReady("test-model")

    assertThat(result).isFalse()
  }

  @Test
  fun `hasReadyModel returns true when at least one model is ready`() = runTest {
    val model1 = createTestModel(ProviderType.MEDIA_PIPE, "model-1")
    val model2 = createTestModel(ProviderType.MEDIA_PIPE, "model-2")

    coEvery { modelCatalogRepository.getModel("model-1") } returns model1
    coEvery { modelCatalogRepository.getModel("model-2") } returns model2
    coEvery { mediaPipeInferenceService.isModelReady("model-1") } returns false
    coEvery { mediaPipeInferenceService.isModelReady("model-2") } returns true

    val result = runtime.hasReadyModel(listOf(model1, model2))

    assertThat(result).isTrue()
  }

  @Test
  fun `hasReadyModel returns false when no models are ready`() = runTest {
    val model1 = createTestModel(ProviderType.MEDIA_PIPE, "model-1")

    coEvery { modelCatalogRepository.getModel("model-1") } returns model1
    coEvery { mediaPipeInferenceService.isModelReady("model-1") } returns false

    val result = runtime.hasReadyModel(listOf(model1))

    assertThat(result).isFalse()
  }

  @Test
  fun `hasReadyModel returns false for empty list`() = runTest {
    val result = runtime.hasReadyModel(emptyList())

    assertThat(result).isFalse()
  }

  @Test
  fun `generate returns error when model not found`() = runTest {
    coEvery { modelCatalogRepository.getModel(any()) } returns null
    val request = LocalGenerationRequest("unknown-model", "test prompt")

    val result = runtime.generate(request)

    assertThat(result).isInstanceOf(NanoAIResult.RecoverableError::class.java)
    val error = result as NanoAIResult.RecoverableError
    assertThat(error.telemetryId).isEqualTo("LOCAL_MODEL_NOT_FOUND")
  }

  @Test
  fun `generate delegates to MediaPipe for MEDIA_PIPE provider`() = runTest {
    val model = createTestModel(ProviderType.MEDIA_PIPE)
    val request = LocalGenerationRequest("test-model", "test prompt")
    val expectedResult =
      NanoAIResult.success(LocalGenerationResult("response", 100L, emptyMap()))

    coEvery { modelCatalogRepository.getModel("test-model") } returns model
    coEvery { mediaPipeInferenceService.generate(request) } returns expectedResult

    val result = runtime.generate(request)

    assertThat(result).isEqualTo(expectedResult)
    coVerify { mediaPipeInferenceService.generate(request) }
  }

  @Test
  fun `generate delegates to Leap for LEAP provider`() = runTest {
    val model = createTestModel(ProviderType.LEAP)
    val request = LocalGenerationRequest("test-model", "test prompt")
    val expectedResult =
      NanoAIResult.success(LocalGenerationResult("response", 100L, emptyMap()))

    coEvery { modelCatalogRepository.getModel("test-model") } returns model
    coEvery { leapInferenceService.generate(request) } returns expectedResult

    val result = runtime.generate(request)

    assertThat(result).isEqualTo(expectedResult)
    coVerify { leapInferenceService.generate(request) }
  }

  @Test
  fun `generate returns error for unsupported provider`() = runTest {
    val model = createTestModel(ProviderType.CLOUD_API)
    val request = LocalGenerationRequest("test-model", "test prompt")

    coEvery { modelCatalogRepository.getModel("test-model") } returns model

    val result = runtime.generate(request)

    assertThat(result).isInstanceOf(NanoAIResult.RecoverableError::class.java)
    val error = result as NanoAIResult.RecoverableError
    assertThat(error.telemetryId).isEqualTo("UNSUPPORTED_PROVIDER")
  }

  private fun createTestModel(
    providerType: ProviderType,
    modelId: String = "test-model",
  ): ModelPackage {
    val now = Clock.System.now()
    return ModelPackage(
      modelId = modelId,
      displayName = "Test Model",
      version = "1.0.0",
      providerType = providerType,
      deliveryType = DeliveryType.LOCAL_ARCHIVE,
      minAppVersion = 1,
      sizeBytes = 1024L,
      capabilities = emptySet(),
      installState = InstallState.NOT_INSTALLED,
      manifestUrl = "https://example.com/manifest.json",
      createdAt = now,
      updatedAt = now,
    )
  }
}
