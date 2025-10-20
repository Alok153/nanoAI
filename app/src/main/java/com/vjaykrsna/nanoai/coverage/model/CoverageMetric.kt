package com.vjaykrsna.nanoai.coverage.model

/** Value object storing coverage percentage, target threshold, and derived status. */
data class CoverageMetric(val coverage: Double, val threshold: Double) {
  init {
    require(coverage in MIN_PERCENTAGE..MAX_PERCENTAGE) {
      "Coverage must be between $MIN_PERCENTAGE and $MAX_PERCENTAGE: $coverage"
    }
    require(threshold in MIN_PERCENTAGE..MAX_PERCENTAGE) {
      "Threshold must be between $MIN_PERCENTAGE and $MAX_PERCENTAGE: $threshold"
    }
  }

  private val normalizedCoverage: Double = coverage.coerceIn(MIN_PERCENTAGE, MAX_PERCENTAGE)
  private val normalizedThreshold: Double = threshold.coerceIn(MIN_PERCENTAGE, MAX_PERCENTAGE)

  val roundedCoverage: Double = normalizedCoverage.roundToSingleDecimal()
  val roundedThreshold: Double = normalizedThreshold.roundToSingleDecimal()

  private val roundedDelta: Double = (roundedCoverage - roundedThreshold).roundToSingleDecimal()

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

  val statusColor: StatusColor =
    when (status) {
      Status.BELOW_TARGET -> StatusColor.NEGATIVE
      Status.ON_TARGET -> StatusColor.NEUTRAL
      Status.EXCEEDS_TARGET -> StatusColor.POSITIVE
    }

  enum class Status {
    BELOW_TARGET,
    ON_TARGET,
    EXCEEDS_TARGET,
  }

  enum class StatusColor {
    NEGATIVE,
    NEUTRAL,
    POSITIVE,
  }

  companion object {
    private const val MIN_PERCENTAGE = 0.0
    private const val MAX_PERCENTAGE = 100.0
  }
}
