package com.vjaykrsna.nanoai.core.data.library.huggingface

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.annotations.OneShot
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceCatalogQuery
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceModelSummary

/** Repository exposing curated listings from the Hugging Face Hub. */
interface HuggingFaceCatalogRepository {
  /**
   * Fetches a page of models from Hugging Face.
   *
   * @param search Optional query to filter models by id or tags.
   * @param limit Maximum number of models to retrieve.
   */
  @OneShot("Fetch Hugging Face catalog page")
  suspend fun listModels(
    query: HuggingFaceCatalogQuery
  ): NanoAIResult<List<HuggingFaceModelSummary>>

  companion object {
    const val DEFAULT_LIMIT = 50
  }
}
