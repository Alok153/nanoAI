package com.vjaykrsna.nanoai.feature.library.domain

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.feature.library.data.huggingface.HuggingFaceCatalogRepository
import com.vjaykrsna.nanoai.feature.library.domain.model.HuggingFaceCatalogQuery
import com.vjaykrsna.nanoai.feature.library.domain.model.HuggingFaceModelSummary
import javax.inject.Inject
import javax.inject.Singleton

/** Use case for Hugging Face catalog operations. */
@Singleton
class HuggingFaceCatalogUseCase
@Inject
constructor(private val huggingFaceCatalogRepository: HuggingFaceCatalogRepository) {
  /** List models from Hugging Face catalog. */
  suspend fun listModels(
    query: HuggingFaceCatalogQuery
  ): NanoAIResult<List<HuggingFaceModelSummary>> {
    return try {
      val models = huggingFaceCatalogRepository.listModels(query).getOrThrow()
      NanoAIResult.success(models)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to list Hugging Face models",
        cause = e,
        context =
          mapOf(
            "search" to (query.search ?: ""),
            "pipelineTag" to (query.pipelineTag ?: ""),
            "library" to (query.library ?: ""),
            "limit" to query.limit.toString(),
            "offset" to query.offset.toString(),
          ),
      )
    }
  }
}
