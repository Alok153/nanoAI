package com.vjaykrsna.nanoai.core.coverage

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.coverage.model.CoverageMetric
import com.vjaykrsna.nanoai.core.coverage.model.CoverageSummary
import com.vjaykrsna.nanoai.core.coverage.model.RiskRegisterItemRef
import com.vjaykrsna.nanoai.core.coverage.model.TestLayer
import java.time.Instant
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CoverageSummaryTest {

  @Test
  fun `statusFor returns metric status based on threshold comparison`() {
    val summary =
      CoverageSummary(
        buildId = "build-1",
        timestamp = Instant.parse("2025-10-09T07:00:00Z"),
        layerMetrics =
          mapOf(
            TestLayer.VIEW_MODEL to CoverageMetric(coverage = 74.0, threshold = 75.0),
            TestLayer.UI to CoverageMetric(coverage = 66.0, threshold = 65.0),
            TestLayer.DATA to CoverageMetric(coverage = 71.0, threshold = 70.0),
          ),
        thresholds =
          mapOf(TestLayer.VIEW_MODEL to 75.0, TestLayer.UI to 65.0, TestLayer.DATA to 70.0),
        trendDelta = emptyMap(),
        riskItems = emptyList(),
      )

    assertThat(summary.statusFor(TestLayer.VIEW_MODEL))
      .isEqualTo(CoverageMetric.Status.BELOW_TARGET)
    assertThat(summary.statusFor(TestLayer.UI)).isEqualTo(CoverageMetric.Status.EXCEEDS_TARGET)
    assertThat(summary.statusFor(TestLayer.DATA)).isEqualTo(CoverageMetric.Status.EXCEEDS_TARGET)
  }

  @Test
  fun `layersBelowTarget highlights underperforming layers`() {
    val summary =
      CoverageSummary(
        buildId = "build-2",
        timestamp = Instant.parse("2025-10-09T07:00:00Z"),
        layerMetrics =
          mapOf(
            TestLayer.VIEW_MODEL to CoverageMetric(coverage = 72.0, threshold = 75.0),
            TestLayer.UI to CoverageMetric(coverage = 63.0, threshold = 65.0),
            TestLayer.DATA to CoverageMetric(coverage = 74.0, threshold = 70.0),
          ),
        thresholds =
          mapOf(TestLayer.VIEW_MODEL to 75.0, TestLayer.UI to 65.0, TestLayer.DATA to 70.0),
        trendDelta =
          mapOf(TestLayer.VIEW_MODEL to -1.5, TestLayer.UI to -0.5, TestLayer.DATA to 2.0),
        riskItems = listOf(RiskRegisterItemRef("risk-critical-data")),
      )

    assertThat(summary.layersBelowTarget()).containsExactly(TestLayer.VIEW_MODEL, TestLayer.UI)
    assertThat(summary.trendDeltaFor(TestLayer.VIEW_MODEL)).isWithin(0.01).of(-1.5)
    assertThat(summary.trendDeltaFor(TestLayer.DATA)).isWithin(0.01).of(2.0)
  }

  @Test
  fun `invalid coverage metric throws`() {
    assertThrows<IllegalArgumentException> {
      CoverageSummary(
        buildId = "build-invalid",
        timestamp = Instant.parse("2025-10-09T07:00:00Z"),
        layerMetrics =
          mapOf(TestLayer.VIEW_MODEL to CoverageMetric(coverage = 112.0, threshold = 75.0)),
        thresholds = mapOf(TestLayer.VIEW_MODEL to 75.0),
        trendDelta = emptyMap(),
        riskItems = emptyList(),
      )
    }
  }

  @Test
  fun `statusBreakdown returns counts by metric status`() {
    val summary =
      CoverageSummary(
        buildId = "build-3",
        timestamp = Instant.parse("2025-10-09T07:00:00Z"),
        layerMetrics =
          mapOf(
            TestLayer.VIEW_MODEL to CoverageMetric(coverage = 72.0, threshold = 75.0),
            TestLayer.UI to CoverageMetric(coverage = 75.0, threshold = 75.0),
            TestLayer.DATA to CoverageMetric(coverage = 81.0, threshold = 70.0),
          ),
        thresholds =
          mapOf(TestLayer.VIEW_MODEL to 75.0, TestLayer.UI to 65.0, TestLayer.DATA to 70.0),
        trendDelta =
          mapOf(TestLayer.VIEW_MODEL to -2.0, TestLayer.UI to 0.0, TestLayer.DATA to 5.0),
        riskItems = emptyList(),
      )

    val breakdown = summary.statusBreakdown()

    assertThat(breakdown[CoverageMetric.Status.BELOW_TARGET]).isEqualTo(1)
    assertThat(breakdown[CoverageMetric.Status.ON_TARGET]).isEqualTo(1)
    assertThat(breakdown[CoverageMetric.Status.EXCEEDS_TARGET]).isEqualTo(1)
  }

  @Test
  fun `riskItems exposes typed references`() {
    val riskItemsField =
      CoverageSummary::class.java.declaredFields.firstOrNull { it.name == "riskItems" }
        ?: error("CoverageSummary riskItems field missing")

    assertThat(riskItemsField.genericType.typeName).contains("RiskRegisterItemRef")
  }

  @Test
  fun `trendDeltaFor rounds to a single decimal place`() {
    val summary =
      CoverageSummary(
        buildId = "build-rounding",
        timestamp = Instant.parse("2025-10-09T07:00:00Z"),
        layerMetrics = mapOf(TestLayer.UI to CoverageMetric(coverage = 63.4, threshold = 65.0)),
        thresholds = mapOf(TestLayer.UI to 65.0),
        trendDelta = mapOf(TestLayer.UI to 1.345),
        riskItems = emptyList(),
      )

    assertThat(summary.trendDeltaFor(TestLayer.UI)).isEqualTo(1.3)
  }
}
