package com.vjaykrsna.nanoai.core.domain.chat

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.runtime.LeapInferenceService
import com.vjaykrsna.nanoai.core.runtime.LocalGenerationRequest
import com.vjaykrsna.nanoai.core.runtime.LocalGenerationResult
import javax.inject.Inject

/** A use case that generates text from a given prompt using a Leap model. */
class GenerateLeapTextUseCase
@Inject
constructor(private val leapInferenceService: LeapInferenceService) {
  /** Generates text from a given prompt using a Leap model. */
  suspend operator fun invoke(
    request: LocalGenerationRequest
  ): NanoAIResult<LocalGenerationResult> {
    return leapInferenceService.generate(request)
  }
}
