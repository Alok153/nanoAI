package com.vjaykrsna.nanoai.core.domain.library

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.annotations.OneShot

/** Interface for Hugging Face catalog operations. */
interface HuggingFaceCatalogUseCaseInterface {
  @OneShot("List Hugging Face catalog models")
  suspend fun listModels(
    query: HuggingFaceCatalogQuery
  ): NanoAIResult<List<HuggingFaceModelSummary>>
}
