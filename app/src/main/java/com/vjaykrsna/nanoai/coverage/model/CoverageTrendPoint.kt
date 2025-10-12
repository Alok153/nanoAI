package com.vjaykrsna.nanoai.coverage.model

import java.time.Instant

/** Historical coverage datapoint used for trend analysis and regressions. */
data class CoverageTrendPoint(
  val buildId: String,
  val layer: TestLayer,
  val coverage: Double,
  val threshold: Double,
  val recordedAt: Instant,
) {
  private val cachedDelta: Double = coverage - threshold

  init {
    require(buildId.isNotBlank()) { "buildId must not be blank" }
    require(coverage in MIN_COVERAGE_PERCENT..MAX_COVERAGE_PERCENT) {
      "Coverage must be between 0 and 100"
    }
    require(threshold in MIN_COVERAGE_PERCENT..MAX_COVERAGE_PERCENT) {
      "Threshold must be between 0 and 100"
    }
  }

  fun deltaFromThreshold(): Double = cachedDelta

  companion object {
    private const val MIN_COVERAGE_PERCENT = 0.0
    private const val MAX_COVERAGE_PERCENT = 100.0

    fun validateSequence(points: List<CoverageTrendPoint>) {
      points.zipWithNext { previous, current ->
        require(!current.recordedAt.isBefore(previous.recordedAt)) {
          "Coverage trend points must be ordered by non-decreasing recordedAt"
        }
      }
    }
  }
}
