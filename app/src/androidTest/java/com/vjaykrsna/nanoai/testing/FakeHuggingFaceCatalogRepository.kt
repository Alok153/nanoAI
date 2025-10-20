package com.vjaykrsna.nanoai.testing

import com.vjaykrsna.nanoai.feature.library.data.huggingface.HuggingFaceCatalogRepository
import com.vjaykrsna.nanoai.feature.library.domain.model.HuggingFaceCatalogQuery
import com.vjaykrsna.nanoai.feature.library.domain.model.HuggingFaceModelSummary

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
  ): Result<List<HuggingFaceModelSummary>> {
    if (shouldFail) {
      return Result.failure(failureException)
    }
    return Result.success(models)
  }
}
