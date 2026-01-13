package com.vjaykrsna.nanoai.core.domain.chat

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.runtime.LocalGenerationRequest
import com.vjaykrsna.nanoai.core.runtime.LocalGenerationResult
import javax.inject.Inject

/**
 * A use case that generates constrained text from a given prompt using a Leap model.
 *
 * **Status: Pending Leap SDK Integration**
 *
 * This use case is prepared for Leap SDK constrained text generation support. Currently returns an
 * unsupported operation result with telemetry context for tracking adoption readiness.
 *
 * Constrained generation enables grammar-guided output formats such as JSON schema adherence.
 */
class GenerateLeapConstrainedTextUseCase @Inject constructor() {
  /**
   * Generates constrained text from a given prompt using a Leap model.
   *
   * @param request The generation request.
   * @return A [NanoAIResult] containing the [LocalGenerationResult] or a recoverable error.
   */
  suspend operator fun invoke(
    request: LocalGenerationRequest
  ): NanoAIResult<LocalGenerationResult> {
    // Leap SDK constrained generation - awaiting SDK availability
    return NanoAIResult.recoverable(
      message = "Constrained text generation is not yet supported",
      telemetryId = "LEAP_CONSTRAINED_UNSUPPORTED",
      context = mapOf("modelId" to request.modelId),
    )
  }
}
