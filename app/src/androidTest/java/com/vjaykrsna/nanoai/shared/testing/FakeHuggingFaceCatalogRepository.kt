package com.vjaykrsna.nanoai.shared.testing

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.data.library.huggingface.HuggingFaceCatalogRepository
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceCatalogQuery
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceModelSummary

/** Fake implementation of [HuggingFaceCatalogRepository] for testing. */
class FakeHuggingFaceCatalogRepository : HuggingFaceCatalogRepository {
  private var models: List<HuggingFaceModelSummary> = emptyList()
  private var shouldFail = false
  private var failureException: Exception = RuntimeException("Simulated failure")

  fun setModels(models: List<HuggingFaceModelSummary>) {
    this.models = models
  }

  fun setShouldFail(
    shouldFail: Boolean,
    exception: Exception = RuntimeException("Simulated failure"),
  ) {
    this.shouldFail = shouldFail
    this.failureException = exception
  }

  override suspend fun listModels(
    query: HuggingFaceCatalogQuery
  ): NanoAIResult<List<HuggingFaceModelSummary>> {
    if (shouldFail) {
      return NanoAIResult.recoverable(
        failureException.message ?: "Test failure",
        cause = failureException,
      )
    }
    return NanoAIResult.success(models)
  }
}
