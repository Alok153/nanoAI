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
    require(coverage in 0.0..100.0) { "Coverage must be between 0 and 100" }
    require(threshold in 0.0..100.0) { "Threshold must be between 0 and 100" }
  }

  fun deltaFromThreshold(): Double = cachedDelta

  companion object {
    fun validateSequence(points: List<CoverageTrendPoint>) {
      points.zipWithNext { previous, current ->
        require(!current.recordedAt.isBefore(previous.recordedAt)) {
          "Coverage trend points must be ordered by non-decreasing recordedAt"
        }
      }
    }
  }
}
