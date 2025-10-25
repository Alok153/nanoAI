package com.vjaykrsna.nanoai.feature.library.domain

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.feature.library.data.huggingface.HuggingFaceCatalogRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Use case for Hugging Face catalog operations. */
@Singleton
class HuggingFaceCatalogUseCase
@Inject
constructor(private val huggingFaceCatalogRepository: HuggingFaceCatalogRepository) :
  HuggingFaceCatalogUseCaseInterface {
  /** List models from Hugging Face catalog. */
  override suspend fun listModels(
    query: HuggingFaceCatalogQuery
  ): NanoAIResult<List<HuggingFaceModelSummary>> {
    return huggingFaceCatalogRepository.listModels(query)
  }
}
