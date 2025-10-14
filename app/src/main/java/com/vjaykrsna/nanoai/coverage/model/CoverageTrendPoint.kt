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
  private val roundedDelta: Double = (coverage - threshold).roundToSingleDecimal()

  init {
    require(buildId.isNotBlank()) { "buildId must not be blank" }
    require(coverage in MIN_COVERAGE_PERCENT..MAX_COVERAGE_PERCENT) {
      "Coverage must be between 0 and 100"
    }
    require(threshold in MIN_COVERAGE_PERCENT..MAX_COVERAGE_PERCENT) {
      "Threshold must be between 0 and 100"
    }
  }

  fun deltaFromThreshold(): Double = roundedDelta

  companion object {
    private const val MIN_COVERAGE_PERCENT = 0.0
    private const val MAX_COVERAGE_PERCENT = 100.0

    fun fromMetric(
      buildId: String,
      layer: TestLayer,
      metric: CoverageMetric,
      recordedAt: Instant,
    ): CoverageTrendPoint =
      CoverageTrendPoint(
        buildId = buildId,
        layer = layer,
        coverage = metric.coverage,
        threshold = metric.threshold,
        recordedAt = recordedAt,
      )

    fun fromSummary(
      buildId: String,
      layer: TestLayer,
      coverage: Double,
      summary: CoverageSummary,
      recordedAt: Instant,
    ): CoverageTrendPoint =
      CoverageTrendPoint(
        buildId = buildId,
        layer = layer,
        coverage = coverage,
        threshold = summary.thresholdFor(layer),
        recordedAt = recordedAt,
      )

    fun validateSequence(points: List<CoverageTrendPoint>) {
      points.zipWithNext { previous, current ->
        require(!current.recordedAt.isBefore(previous.recordedAt)) {
          "Coverage trend points must be ordered by non-decreasing recordedAt"
        }
        if (current.layer == previous.layer) {
          require(
            current.threshold.roundToSingleDecimal() == previous.threshold.roundToSingleDecimal()
          ) {
            "Threshold for ${current.layer} changed between ${previous.buildId} and ${current.buildId}"
          }
          val previousCoverage = previous.coverage.roundToSingleDecimal()
          val currentCoverage = current.coverage.roundToSingleDecimal()
          require(currentCoverage >= previousCoverage) {
            "Coverage regressed for ${current.layer} between ${previous.buildId} and ${current.buildId}"
          }
        }
      }
    }
  }
}
