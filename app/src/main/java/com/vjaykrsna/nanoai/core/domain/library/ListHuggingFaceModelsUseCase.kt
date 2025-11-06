package com.vjaykrsna.nanoai.core.domain.library

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.data.library.huggingface.HuggingFaceCatalogRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Retrieves Hugging Face model listings for the model library. */
@Singleton
class ListHuggingFaceModelsUseCase
@Inject
constructor(private val repository: HuggingFaceCatalogRepository) {
  suspend operator fun invoke(
    query: HuggingFaceCatalogQuery = HuggingFaceCatalogQuery()
  ): NanoAIResult<List<HuggingFaceModelSummary>> {
    return repository.listModels(query)
  }
}
