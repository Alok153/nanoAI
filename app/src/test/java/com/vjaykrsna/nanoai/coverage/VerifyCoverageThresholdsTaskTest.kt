package com.vjaykrsna.nanoai.coverage

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.coverage.model.CoverageMetric
import com.vjaykrsna.nanoai.coverage.model.CoverageSummary
import com.vjaykrsna.nanoai.coverage.model.TestLayer
import com.vjaykrsna.nanoai.coverage.verification.CoverageThresholdVerifier
import java.time.Instant
import kotlin.test.assertFailsWith
import org.junit.Test

class VerifyCoverageThresholdsTaskTest {

  @Test
  fun `passes when all layers meet thresholds`() {
    val summary =
      CoverageSummary(
        buildId = "build-100",
        timestamp = Instant.parse("2025-10-09T12:00:00Z"),
        layerMetrics =
          mapOf(
            TestLayer.VIEW_MODEL to CoverageMetric(coverage = 78.0, threshold = 75.0),
            TestLayer.UI to CoverageMetric(coverage = 66.0, threshold = 65.0),
            TestLayer.DATA to CoverageMetric(coverage = 72.0, threshold = 70.0),
          ),
        thresholds =
          mapOf(
            TestLayer.VIEW_MODEL to 75.0,
            TestLayer.UI to 65.0,
            TestLayer.DATA to 70.0,
          ),
        trendDelta = emptyMap(),
        riskItems = emptyList(),
      )

    val result = CoverageThresholdVerifier().verify(summary)

    assertThat(result.belowThresholdLayers).isEmpty()
  }

  @Test
  fun `fails when any layer drops below the configured threshold`() {
    val summary =
      CoverageSummary(
        buildId = "build-101",
        timestamp = Instant.parse("2025-10-09T12:00:00Z"),
        layerMetrics =
          mapOf(
            TestLayer.VIEW_MODEL to CoverageMetric(coverage = 71.0, threshold = 75.0),
            TestLayer.UI to CoverageMetric(coverage = 66.0, threshold = 65.0),
            TestLayer.DATA to CoverageMetric(coverage = 72.0, threshold = 70.0),
          ),
        thresholds =
          mapOf(
            TestLayer.VIEW_MODEL to 75.0,
            TestLayer.UI to 65.0,
            TestLayer.DATA to 70.0,
          ),
        trendDelta = emptyMap(),
        riskItems = emptyList(),
      )

    val error =
      assertFailsWith<CoverageThresholdVerifier.ThresholdViolation> {
        CoverageThresholdVerifier().verify(summary)
      }

    assertThat(error.layers).containsExactly(TestLayer.VIEW_MODEL)
    assertThat(error.message).contains("VIEW_MODEL")
  }
}
