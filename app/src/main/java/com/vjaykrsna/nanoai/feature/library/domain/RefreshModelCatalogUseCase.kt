package com.vjaykrsna.nanoai.feature.library.domain

import com.vjaykrsna.nanoai.feature.library.data.ModelCatalogRepository
import com.vjaykrsna.nanoai.feature.library.data.catalog.ModelCatalogSource
import javax.inject.Inject
import javax.inject.Singleton

/** Coordinates catalog refresh from remote or bundled sources. */
@Singleton
class RefreshModelCatalogUseCase
@Inject
constructor(
  private val modelCatalogSource: ModelCatalogSource,
  private val modelCatalogRepository: ModelCatalogRepository,
) {
  suspend operator fun invoke(): Result<Unit> = runCatching {
    val models = modelCatalogSource.fetchCatalog()
    modelCatalogRepository.replaceCatalog(models)
  }
}
