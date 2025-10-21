package com.vjaykrsna.nanoai.feature.library.domain

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.feature.library.data.huggingface.HuggingFaceCatalogRepository
import com.vjaykrsna.nanoai.feature.library.domain.model.HuggingFaceCatalogQuery
import com.vjaykrsna.nanoai.feature.library.domain.model.HuggingFaceModelSummary
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
    return repository
      .listModels(query)
      .fold(
        onSuccess = { models -> NanoAIResult.success(models) },
        onFailure = { error ->
          NanoAIResult.recoverable(
            message = "Failed to list Hugging Face models",
            cause = error,
            context = mapOf("query" to query.toString()),
          )
        },
      )
  }
}
