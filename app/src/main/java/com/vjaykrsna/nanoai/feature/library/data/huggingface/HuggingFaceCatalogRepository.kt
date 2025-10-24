package com.vjaykrsna.nanoai.feature.library.data.huggingface

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.feature.library.domain.model.HuggingFaceCatalogQuery
import com.vjaykrsna.nanoai.feature.library.domain.model.HuggingFaceModelSummary

/** Repository exposing curated listings from the Hugging Face Hub. */
interface HuggingFaceCatalogRepository {
  /**
   * Fetches a page of models from Hugging Face.
   *
   * @param search Optional query to filter models by id or tags.
   * @param limit Maximum number of models to retrieve.
   */
  suspend fun listModels(
    query: HuggingFaceCatalogQuery
  ): NanoAIResult<List<HuggingFaceModelSummary>>

  companion object {
    const val DEFAULT_LIMIT = 50
  }
}
