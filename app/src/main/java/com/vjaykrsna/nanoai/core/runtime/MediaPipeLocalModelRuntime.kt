package com.vjaykrsna.nanoai.core.runtime

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DEFAULT_MAX_TOKENS = 1024
private const val DEFAULT_TEMPERATURE = 0.7f

/**
 * MediaPipe-backed implementation of [LocalModelRuntime].
 *
 * The current implementation provides a lightweight shim that verifies the model payload exists and
 * returns a synthesized response. Integration with MediaPipe LiteRT graph execution will be added
 * once the converted model artifacts are available (see TODO(NANO-421)).
 */
@Singleton
class MediaPipeLocalModelRuntime
@Inject
constructor(
  @ApplicationContext private val context: Context,
) : LocalModelRuntime {
  private val modelDirectory: File by lazy { File(context.filesDir, "models") }

  private var llmInference: LlmInference? = null

  override suspend fun isModelReady(modelId: String): Boolean =
    withContext(Dispatchers.IO) { modelFile(modelId).exists() }

  override suspend fun hasReadyModel(models: List<ModelPackage>): Boolean {
    if (models.isEmpty()) return false
    return models.any { model ->
      model.providerType.name != "CLOUD_API" && isModelReady(model.modelId)
    }
  }

  @OptIn(ExperimentalTime::class)
  override suspend fun generate(request: LocalGenerationRequest): Result<LocalGenerationResult> {
    return withContext(Dispatchers.Default) {
      val file = modelFile(request.modelId)
      if (!file.exists()) {
        return@withContext Result.failure(
          FileNotFoundException("Local model ${request.modelId} is not installed"),
        )
      }

      initializeInference(request)

      val prompt =
        if (request.systemPrompt.isNullOrBlank()) request.prompt
        else "${request.systemPrompt}\n\n${request.prompt}"

      var resultText = ""
      val latency = measureTime { resultText = llmInference?.generateResponse(prompt) ?: "" }

      Result.success(
        LocalGenerationResult(
          text = resultText,
          latencyMs = latency.inWholeMilliseconds,
          metadata =
            mapOf(
              "modelId" to request.modelId,
              "temperature" to request.temperature,
              "topP" to request.topP,
            ),
        ),
      )
    }
  }

  private fun modelFile(modelId: String): File = File(modelDirectory, "$modelId.bin")

  private fun initializeInference(request: LocalGenerationRequest) {
    if (llmInference == null) {
      val options =
        LlmInference.LlmInferenceOptions.builder()
          .setModelPath(modelFile(request.modelId).absolutePath)
          .setMaxTokens(request.maxOutputTokens ?: DEFAULT_MAX_TOKENS)
          .setTemperature(request.temperature ?: DEFAULT_TEMPERATURE)
          .build()
      llmInference = LlmInference.createFromOptions(context, options)
    }
  }
}
