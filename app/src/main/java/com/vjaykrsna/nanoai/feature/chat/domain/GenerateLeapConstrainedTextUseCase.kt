package com.vjaykrsna.nanoai.feature.chat.domain

import com.vjaykrsna.nanoai.core.runtime.LocalGenerationRequest
import com.vjaykrsna.nanoai.core.runtime.LocalGenerationResult
import javax.inject.Inject

/** A use case that generates constrained text from a given prompt using a Leap model. */
class GenerateLeapConstrainedTextUseCase @Inject constructor() {
  /**
   * Generates constrained text from a given prompt using a Leap model.
   *
   * @param request The generation request.
   * @return A [Result] containing the [LocalGenerationResult] or an exception.
   */
  suspend operator fun invoke(request: LocalGenerationRequest): Result<LocalGenerationResult> {
    // TODO: Implement constrained text generation with the Leap SDK.
    return Result.failure(
      UnsupportedOperationException(
        "Constrained text generation is not yet supported by the Leap SDK."
      )
    )
  }
}
