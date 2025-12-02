package com.vjaykrsna.nanoai.core.domain.library

import com.vjaykrsna.nanoai.core.domain.model.ModelPackage

/** Abstraction for loading model catalog definitions from a source (assets, network, etc.). */
interface ModelCatalogSource {
  suspend fun fetchCatalog(): List<ModelPackage>
}
