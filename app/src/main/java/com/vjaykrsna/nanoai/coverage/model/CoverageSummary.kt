package com.vjaykrsna.nanoai.coverage.model

import java.time.Instant
import java.util.EnumMap

/**
 * Aggregated coverage snapshot for a CI build. Provides helper methods for gating logic and
 * analytics rendering.
 */
data class CoverageSummary(
  val buildId: String,
  val timestamp: Instant,
  val layerMetrics: Map<TestLayer, CoverageMetric>,
  val thresholds: Map<TestLayer, Double>,
  val trendDelta: Map<TestLayer, Double>,
  val riskItems: List<RiskRegisterItemRef>,
) {
  init {
    require(buildId.isNotBlank()) { "buildId must not be blank" }
    require(layerMetrics.isNotEmpty()) { "layerMetrics must contain at least one entry" }
    require(layerMetrics.keys == thresholds.keys) {
      "thresholds must provide values for the same layers as layerMetrics"
    }
    require(layerMetrics.values.all { it.coverage in MIN_PERCENT..MAX_PERCENT }) {
      "Coverage metrics must be between 0 and 100"
    }
    require(riskItems.distinctBy { it.riskId }.size == riskItems.size) {
      "riskItems must not contain duplicate references"
    }
  }

  fun metricFor(layer: TestLayer): CoverageMetric =
    layerMetrics[layer] ?: error("No coverage metric recorded for $layer")

  fun statusFor(layer: TestLayer): CoverageMetric.Status = metricFor(layer).status

  fun thresholdFor(layer: TestLayer): Double =
    thresholds[layer] ?: error("No threshold recorded for $layer")

  private val roundedTrendDelta: Map<TestLayer, Double> =
    trendDelta.mapValues { (_, value) -> value.roundToSingleDecimal() }

  fun trendDeltaFor(layer: TestLayer): Double = roundedTrendDelta[layer] ?: DEFAULT_DELTA

  fun layersBelowTarget(): Set<TestLayer> =
    layerMetrics.filterValues { it.status == CoverageMetric.Status.BELOW_TARGET }.keys

  fun statusBreakdown(): Map<CoverageMetric.Status, Int> {
    val counts = EnumMap<CoverageMetric.Status, Int>(CoverageMetric.Status::class.java)
    CoverageMetric.Status.entries.forEach { status -> counts[status] = INITIAL_STATUS_COUNT }
    layerMetrics.values.forEach { metric ->
      val status = metric.status
      counts[status] = counts.getValue(status) + 1
    }
    return counts
  }

  companion object {
    private const val MIN_PERCENT = 0.0
    private const val MAX_PERCENT = 100.0
    private const val DEFAULT_DELTA = 0.0
    private const val INITIAL_STATUS_COUNT = 0
  }
}
