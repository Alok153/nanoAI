package com.vjaykrsna.nanoai.feature.library.domain

import com.vjaykrsna.nanoai.core.common.NanoAIResult

/** Interface for Hugging Face catalog operations. */
interface HuggingFaceCatalogUseCaseInterface {
  suspend fun listModels(
    query: HuggingFaceCatalogQuery
  ): NanoAIResult<List<HuggingFaceModelSummary>>
}
