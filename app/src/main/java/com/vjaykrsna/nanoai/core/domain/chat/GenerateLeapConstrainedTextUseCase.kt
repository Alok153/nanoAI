package com.vjaykrsna.nanoai.core.domain.chat

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.runtime.LocalGenerationRequest
import com.vjaykrsna.nanoai.core.runtime.LocalGenerationResult
import javax.inject.Inject

/** A use case that generates constrained text from a given prompt using a Leap model. */
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
    // TODO: Implement constrained text generation with the Leap SDK.
    return NanoAIResult.recoverable(
      message = "Constrained text generation is not yet supported",
      telemetryId = "LEAP_CONSTRAINED_UNSUPPORTED",
      context = mapOf("modelId" to request.modelId),
    )
  }
}
