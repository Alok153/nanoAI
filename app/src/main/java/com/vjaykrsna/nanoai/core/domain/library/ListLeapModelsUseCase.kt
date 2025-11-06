package com.vjaykrsna.nanoai.core.domain.library

import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import javax.inject.Inject

/** A use case that lists all available Leap models. */
class ListLeapModelsUseCase
@Inject
constructor(private val modelCatalogRepository: ModelCatalogRepository) {
  /** Returns a list of all available Leap models. */
  suspend operator fun invoke(): List<ModelPackage> {
    return modelCatalogRepository.getAllModels().filter { it.providerType == ProviderType.LEAP }
  }
}
