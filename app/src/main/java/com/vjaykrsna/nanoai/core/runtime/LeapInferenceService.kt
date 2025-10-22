package com.vjaykrsna.nanoai.core.runtime

import ai.liquid.leap.LeapClient
import ai.liquid.leap.LeapModelLoadingException
import ai.liquid.leap.LeapModelRunner
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.collect

/** An [InferenceService] that uses the Leap SDK to perform local model inference. */
@Singleton
class LeapInferenceService @Inject constructor() : InferenceService {

  private val modelRunners = mutableMapOf<String, LeapModelRunner>()

  override suspend fun isModelReady(modelId: String): Boolean {
    return modelRunners.containsKey(modelId)
  }

  override suspend fun generate(request: LocalGenerationRequest): Result<LocalGenerationResult> {
    val modelRunner =
      modelRunners[request.modelId]
        ?: return Result.failure(IllegalStateException("Model not loaded: ${request.modelId}"))

    val conversation = modelRunner.createConversation()
    val response = StringBuilder()
    val startTime = System.currentTimeMillis()

    return try {
      conversation.generateResponse(request.prompt).collect {
        when (it) {
          is Message.Chunk -> response.append(it.text)
          else -> {
            // Ignore other response types for now
          }
        }
      }

      val latencyMs = System.currentTimeMillis() - startTime
      Result.success(
        LocalGenerationResult(
          text = response.toString(),
          latencyMs = latencyMs,
          metadata = mapOf("modelId" to request.modelId),
        )
      )
    } catch (e: Exception) {
      // TODO: Refine exception handling to catch more specific exceptions from the Leap SDK
      // if they are documented.
      Result.failure(e)
    }
  }

  /**
   * Loads a Leap model into memory.
   *
   * @param model The model to load.
   */
  suspend fun loadModel(model: ModelPackage) {
    try {
      val modelRunner = LeapClient.loadModel(model.manifestUrl)
      modelRunners[model.modelId] = modelRunner
    } catch (e: LeapModelLoadingException) {
      throw LeapModelLoadException("Failed to load Leap model: ${model.modelId}", e)
    }
  }

  /**
   * Unloads a Leap model from memory.
   *
   * @param modelId The ID of the model to unload.
   */
  fun unloadModel(modelId: String) {
    modelRunners.remove(modelId)
  }
}
