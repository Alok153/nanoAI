package com.vjaykrsna.nanoai.feature.library.domain

import com.vjaykrsna.nanoai.core.runtime.LeapInferenceService
import com.vjaykrsna.nanoai.feature.library.data.ModelCatalogRepository
import javax.inject.Inject

/** A use case that downloads a Leap model. */
class DownloadLeapModelUseCase
@Inject
constructor(
  private val modelCatalogRepository: ModelCatalogRepository,
  private val leapInferenceService: LeapInferenceService,
) {
  /**
   * Downloads a Leap model.
   *
   * @param modelId The ID of the model to download.
   */
  suspend operator fun invoke(modelId: String) {
    val model =
      modelCatalogRepository.getModel(modelId)
        ?: throw IllegalArgumentException("Model not found: $modelId")

    leapInferenceService.loadModel(model)
  }
}
