package com.vjaykrsna.nanoai.feature.library.domain

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.feature.library.domain.model.HuggingFaceCatalogQuery
import com.vjaykrsna.nanoai.feature.library.domain.model.HuggingFaceModelSummary

/** Interface for Hugging Face catalog operations. */
interface HuggingFaceCatalogUseCaseInterface {
  suspend fun listModels(
    query: HuggingFaceCatalogQuery
  ): NanoAIResult<List<HuggingFaceModelSummary>>
}
