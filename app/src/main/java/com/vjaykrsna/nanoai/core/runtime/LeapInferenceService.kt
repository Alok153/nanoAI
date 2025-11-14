package com.vjaykrsna.nanoai.core.runtime

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
  override suspend fun generate(request: LocalGenerationRequest): Result<LocalGenerationResult> {
    if (!loadedModels.contains(request.modelId)) {
      return Result.failure(IllegalStateException("Model not loaded: ${request.modelId}"))
    }

    return withContext(Dispatchers.Default) {
      // TODO: Implement actual Leap inference once API is available
      val prompt =
        if (request.systemPrompt.isNullOrBlank()) {
          request.prompt
        } else {
          "${request.systemPrompt}\n\n${request.prompt}"
        }

      val latency = measureTime {
        // Placeholder for actual inference
      }

      Result.success(
        LocalGenerationResult(
          text = "Leap inference not yet implemented",
          latencyMs = latency.inWholeMilliseconds,
          metadata = mapOf("modelId" to request.modelId),
        )
      )
    }
  }

  /**
   * Loads a Leap model into memory.
   *
   * @param model The model to load.
   */
  suspend fun loadModel(model: ModelPackage) {
    runCatching {
        // TODO: Implement actual Leap model loading once API is available
        // LeapClient.loadModel(model.manifestUrl)
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
}
