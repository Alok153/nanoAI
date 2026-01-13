package com.vjaykrsna.nanoai.core.domain.library

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.annotations.OneShot
import javax.inject.Inject
import javax.inject.Singleton

/** Use case for Hugging Face catalog operations. */
@Singleton
class HuggingFaceCatalogUseCase
@Inject
constructor(private val huggingFaceCatalogRepository: HuggingFaceCatalogRepository) {
  /** List models from Hugging Face catalog. */
  @OneShot("List Hugging Face catalog models")
  suspend fun listModels(
    query: HuggingFaceCatalogQuery
  ): NanoAIResult<List<HuggingFaceModelSummary>> {
    return huggingFaceCatalogRepository.listModels(query)
  }
}
