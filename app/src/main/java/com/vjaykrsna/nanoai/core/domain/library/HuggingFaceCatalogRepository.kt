package com.vjaykrsna.nanoai.core.domain.library

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.annotations.OneShot

/** Repository exposing curated listings from the Hugging Face Hub. */
interface HuggingFaceCatalogRepository {
  /**
   * Fetches a page of models from Hugging Face.
   *
   * @param query composed filter + pagination parameters
   */
  @OneShot("Fetch Hugging Face catalog page")
  suspend fun listModels(
    query: HuggingFaceCatalogQuery
  ): NanoAIResult<List<HuggingFaceModelSummary>>

  companion object {
    const val DEFAULT_LIMIT = 50
  }
}
