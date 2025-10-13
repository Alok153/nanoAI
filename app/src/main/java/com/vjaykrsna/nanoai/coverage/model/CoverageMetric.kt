package com.vjaykrsna.nanoai.coverage.model

/** Value object storing coverage percentage, target threshold, and derived status. */
data class CoverageMetric(
  val coverage: Double,
  val threshold: Double,
) {
  init {
    require(coverage in MIN_PERCENTAGE..MAX_PERCENTAGE) {
      "Coverage must be between $MIN_PERCENTAGE and $MAX_PERCENTAGE: $coverage"
    }
    require(threshold in MIN_PERCENTAGE..MAX_PERCENTAGE) {
      "Threshold must be between $MIN_PERCENTAGE and $MAX_PERCENTAGE: $threshold"
    }
  }

  private val roundedDelta: Double = (coverage - threshold).roundToSingleDecimal()

  val status: Status =
    when {
      roundedDelta < 0.0 -> Status.BELOW_TARGET
      roundedDelta > 0.0 -> Status.EXCEEDS_TARGET
      else -> Status.ON_TARGET
    }

  /** Difference between achieved coverage and threshold (positive numbers exceed the goal). */
  val deltaFromThreshold: Double = roundedDelta

  fun meetsThreshold(): Boolean = roundedDelta >= 0.0

  fun isExceedingTarget(): Boolean = roundedDelta > 0.0

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
