package com.vjaykrsna.nanoai.core.runtime

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.library.DeliveryType
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LeapInferenceServiceTest {

  private lateinit var service: LeapInferenceService

  @BeforeEach
  fun setUp() {
    service = LeapInferenceService()
  }

  @Test
  fun `isModelReady returns false when model not loaded`() = runTest {
    val result = service.isModelReady("unloaded-model")

    assertThat(result).isFalse()
  }

  @Test
  fun `isModelReady returns true after model is loaded`() = runTest {
    val model = createTestModel("test-model")
    service.loadModel(model)

    val result = service.isModelReady("test-model")

    assertThat(result).isTrue()
  }

  @Test
  fun `isModelReady returns false after model is unloaded`() = runTest {
    val model = createTestModel("test-model")
    service.loadModel(model)
    service.unloadModel("test-model")

    val result = service.isModelReady("test-model")

    assertThat(result).isFalse()
  }

  @Test
  fun `generate returns error when model not loaded`() = runTest {
    val request = LocalGenerationRequest("unloaded-model", "test prompt")

    val result = service.generate(request)

    assertThat(result).isInstanceOf(NanoAIResult.RecoverableError::class.java)
    val error = result as NanoAIResult.RecoverableError
    assertThat(error.telemetryId).isEqualTo("LEAP_MODEL_NOT_LOADED")
    assertThat(error.message).contains("unloaded-model")
  }

  @Test
  fun `generate returns success for loaded model`() = runTest {
    val model = createTestModel("test-model")
    service.loadModel(model)
    val request = LocalGenerationRequest("test-model", "test prompt")

    val result = service.generate(request)

    assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
    val success = result as NanoAIResult.Success
    assertThat(success.value.text).isNotEmpty()
    assertThat(success.value.latencyMs).isAtLeast(0)
  }

  @Test
  fun `generate includes system prompt when provided`() = runTest {
    val model = createTestModel("test-model")
    service.loadModel(model)
    val request =
      LocalGenerationRequest(
        modelId = "test-model",
        prompt = "user prompt",
        systemPrompt = "system prompt",
      )

    val result = service.generate(request)

    assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
    val success = result as NanoAIResult.Success
    assertThat(success.value.metadata["prompt"]).isEqualTo("system prompt\n\nuser prompt")
  }

  @Test
  fun `loadModel adds model to loaded set`() = runTest {
    val model = createTestModel("new-model")

    service.loadModel(model)

    assertThat(service.isModelReady("new-model")).isTrue()
  }

  @Test
  fun `unloadModel removes model from loaded set`() = runTest {
    val model = createTestModel("model-to-unload")
    service.loadModel(model)
    assertThat(service.isModelReady("model-to-unload")).isTrue()

    service.unloadModel("model-to-unload")

    assertThat(service.isModelReady("model-to-unload")).isFalse()
  }

  @Test
  fun `unloadModel is idempotent for non-loaded model`() = runTest {
    service.unloadModel("never-loaded")

    assertThat(service.isModelReady("never-loaded")).isFalse()
  }

  @Test
  fun `multiple models can be loaded concurrently`() = runTest {
    val model1 = createTestModel("model-1")
    val model2 = createTestModel("model-2")

    service.loadModel(model1)
    service.loadModel(model2)

    assertThat(service.isModelReady("model-1")).isTrue()
    assertThat(service.isModelReady("model-2")).isTrue()
  }

  private fun createTestModel(modelId: String): ModelPackage {
    val now = Clock.System.now()
    return ModelPackage(
      modelId = modelId,
      displayName = "Test Model",
      version = "1.0.0",
      providerType = ProviderType.LEAP,
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

  @Test
  fun `generate handles null system prompt as empty`() = runTest {
    val model = createTestModel("test-model")
    service.loadModel(model)
    val request =
      LocalGenerationRequest(
        modelId = "test-model",
        prompt = "user prompt only",
        systemPrompt = null,
      )

    val result = service.generate(request)

    assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
    val success = result as NanoAIResult.Success
    assertThat(success.value.metadata["prompt"]).isEqualTo("user prompt only")
  }

  @Test
  fun `generate handles blank system prompt as empty`() = runTest {
    val model = createTestModel("test-model")
    service.loadModel(model)
    val request =
      LocalGenerationRequest(modelId = "test-model", prompt = "user prompt", systemPrompt = "   ")

    val result = service.generate(request)

    assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
    val success = result as NanoAIResult.Success
    assertThat(success.value.metadata["prompt"]).isEqualTo("user prompt")
  }

  @Test
  fun `generate includes modelId in metadata`() = runTest {
    val model = createTestModel("metadata-model")
    service.loadModel(model)
    val request = LocalGenerationRequest("metadata-model", "test prompt")

    val result = service.generate(request)

    assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
    val success = result as NanoAIResult.Success
    assertThat(success.value.metadata["modelId"]).isEqualTo("metadata-model")
  }

  @Test
  fun `generate truncates long prompts in metadata`() = runTest {
    val model = createTestModel("truncate-model")
    service.loadModel(model)
    val longPrompt = "A".repeat(100)
    val request = LocalGenerationRequest("truncate-model", longPrompt)

    val result = service.generate(request)

    assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
    val success = result as NanoAIResult.Success
    // PROMPT_PREVIEW_LENGTH is 32 in the source
    val promptPreview = success.value.metadata["prompt"]
    assertThat(promptPreview).isNotNull()
    assertThat(promptPreview).isEqualTo("A".repeat(32))
  }

  @Test
  fun `generate returns placeholder text for unimplemented Leap inference`() = runTest {
    val model = createTestModel("placeholder-model")
    service.loadModel(model)
    val request = LocalGenerationRequest("placeholder-model", "test")

    val result = service.generate(request)

    assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
    val success = result as NanoAIResult.Success
    assertThat(success.value.text).contains("not yet implemented")
  }

  @Test
  fun `loadModel is idempotent`() = runTest {
    val model = createTestModel("idempotent-model")

    service.loadModel(model)
    service.loadModel(model)
    service.loadModel(model)

    assertThat(service.isModelReady("idempotent-model")).isTrue()
  }

  @Test
  fun `generate measures latency`() = runTest {
    val model = createTestModel("latency-model")
    service.loadModel(model)
    val request = LocalGenerationRequest("latency-model", "test")

    val result = service.generate(request)

    assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
    val success = result as NanoAIResult.Success
    assertThat(success.value.latencyMs).isAtLeast(0L)
  }

  @Test
  fun `unload and reload model works correctly`() = runTest {
    val model = createTestModel("reload-model")

    service.loadModel(model)
    assertThat(service.isModelReady("reload-model")).isTrue()

    service.unloadModel("reload-model")
    assertThat(service.isModelReady("reload-model")).isFalse()

    service.loadModel(model)
    assertThat(service.isModelReady("reload-model")).isTrue()
  }
}
