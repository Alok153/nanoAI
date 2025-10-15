package com.vjaykrsna.nanoai.testing

/**
 * Fake implementation of RefreshModelCatalogUseCase for instrumentation testing. Provides
 * controllable behavior for catalog refresh operations.
 */
class FakeRefreshModelCatalogUseCase {
  var shouldFail = false
  var invokeCount = 0
  var lastError: Throwable? = null

  fun reset() {
    shouldFail = false
    invokeCount = 0
    lastError = null
  }

  suspend operator fun invoke(): Result<Unit> {
    invokeCount++
    return if (shouldFail) {
      val error = IllegalStateException("Refresh failed")
      lastError = error
      Result.failure(error)
    } else {
      Result.success(Unit)
    }
  }
}
