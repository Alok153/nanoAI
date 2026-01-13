package com.vjaykrsna.nanoai.core.runtime

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** An [InferenceService] that uses the Leap SDK to perform local model inference. */
@Singleton
class LeapInferenceService @Inject constructor() : InferenceService {

  private val loadedModels = mutableSetOf<String>()

  override suspend fun isModelReady(modelId: String): Boolean {
    return loadedModels.contains(modelId)
  }

  @OptIn(ExperimentalTime::class)
  override suspend fun generate(
    request: LocalGenerationRequest
  ): NanoAIResult<LocalGenerationResult> {
    if (!loadedModels.contains(request.modelId)) {
      return NanoAIResult.recoverable(
        message = "Model ${request.modelId} is not loaded",
        telemetryId = "LEAP_MODEL_NOT_LOADED",
        context = mapOf("modelId" to request.modelId),
      )
    }

    return withContext(Dispatchers.Default) {
      try {
        val prompt =
          if (request.systemPrompt.isNullOrBlank()) {
            request.prompt
          } else {
            "${request.systemPrompt}\n\n${request.prompt}"
          }

        val latency = measureTime {
          // Placeholder for actual inference
        }

        NanoAIResult.success(
          LocalGenerationResult(
            text = "Leap inference not yet implemented",
            latencyMs = latency.inWholeMilliseconds,
            metadata =
              mapOf("modelId" to request.modelId, "prompt" to prompt.take(PROMPT_PREVIEW_LENGTH)),
          )
        )
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (io: IOException) {
        io.toLeapFailure(request.modelId, "LEAP_INFERENCE_IO")
      } catch (error: Throwable) {
        error.toLeapFailure(request.modelId, "LEAP_INFERENCE_ERROR")
      }
    }
  }

  /**
   * Loads a Leap model into memory.
   *
   * @param model The model to load.
   */
  suspend fun loadModel(model: ModelPackage) {
    runCatching {
        // Leap model loading - requires Leap SDK integration
        // Current implementation tracks model IDs for readiness checks
        loadedModels.add(model.modelId)
      }
      .onFailure { throwable ->
        val mappedThrowable =
          when (throwable) {
            is CancellationException -> throwable
            is IOException,
            is IllegalStateException ->
              LeapModelLoadException("Failed to load Leap model: ${model.modelId}", throwable)
            else -> throwable
          }
        throw mappedThrowable
      }
  }

  /**
   * Unloads a Leap model from memory.
   *
   * @param modelId The ID of the model to unload.
   */
  fun unloadModel(modelId: String) {
    loadedModels.remove(modelId)
  }

  private fun Throwable.toLeapFailure(
    modelId: String,
    telemetryId: String,
  ): NanoAIResult<LocalGenerationResult> =
    NanoAIResult.recoverable(
      message = message ?: LEAP_INFERENCE_FAILURE,
      telemetryId = telemetryId,
      cause = this,
      context = mapOf("modelId" to modelId),
    )

  private companion object {
    private const val PROMPT_PREVIEW_LENGTH = 32
    private const val LEAP_INFERENCE_FAILURE = "Leap inference failed"
  }
}
