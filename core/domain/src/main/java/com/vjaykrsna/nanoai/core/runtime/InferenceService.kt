package com.vjaykrsna.nanoai.core.runtime

import com.vjaykrsna.nanoai.core.common.NanoAIResult

/**
 * A generic interface for local model inference.
 *
 * This interface provides a common contract for different inference services, such as MediaPipe and
 * Leap.
 */
interface InferenceService {

  /**
   * Checks if a model is ready for inference.
   *
   * @param modelId The ID of the model to check.
   * @return `true` if the model is ready, `false` otherwise.
   */
  suspend fun isModelReady(modelId: String): Boolean

  /**
   * Generates a response from a given prompt.
   *
   * @param request The generation request.
   * @return A [NanoAIResult] containing the [LocalGenerationResult] or a telemetry-aware error.
   */
  suspend fun generate(request: LocalGenerationRequest): NanoAIResult<LocalGenerationResult>
}
