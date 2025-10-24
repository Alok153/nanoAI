package com.vjaykrsna.nanoai.feature.library.domain

import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.data.ModelCatalogRepository
import com.vjaykrsna.nanoai.feature.library.domain.ProviderType
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
