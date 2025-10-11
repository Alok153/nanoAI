package com.vjaykrsna.nanoai.coverage.model

/** Value object storing coverage percentage, target threshold, and derived status. */
data class CoverageMetric(
  val coverage: Double,
  val threshold: Double,
) {
  private val cachedDelta: Double = coverage - threshold

  init {
    require(coverage in MIN_PERCENTAGE..MAX_PERCENTAGE) {
      "Coverage must be between $MIN_PERCENTAGE and $MAX_PERCENTAGE: $coverage"
    }
    require(threshold in MIN_PERCENTAGE..MAX_PERCENTAGE) {
      "Threshold must be between $MIN_PERCENTAGE and $MAX_PERCENTAGE: $threshold"
    }
  }

  val status: Status =
    when {
      coverage < threshold -> Status.BELOW_TARGET
      coverage > threshold -> Status.EXCEEDS_TARGET
      else -> Status.ON_TARGET
    }

  /** Difference between achieved coverage and threshold (positive numbers exceed the goal). */
  val deltaFromThreshold: Double = cachedDelta

  fun meetsThreshold(): Boolean = coverage >= threshold

  fun isExceedingTarget(): Boolean = coverage > threshold

  enum class Status {
    BELOW_TARGET,
    ON_TARGET,
    EXCEEDS_TARGET,
  }

  companion object {
    private const val MIN_PERCENTAGE = 0.0
    private const val MAX_PERCENTAGE = 100.0
  }
}
