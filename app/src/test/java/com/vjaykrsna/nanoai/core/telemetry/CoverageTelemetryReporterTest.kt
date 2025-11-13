package com.vjaykrsna.nanoai.core.telemetry

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.coverage.model.CoverageMetric
import com.vjaykrsna.nanoai.core.coverage.model.CoverageSummary
import com.vjaykrsna.nanoai.core.coverage.model.RiskRegisterItem
import com.vjaykrsna.nanoai.core.coverage.model.RiskRegisterItem.Severity
import com.vjaykrsna.nanoai.core.coverage.model.RiskRegisterItem.Status
import com.vjaykrsna.nanoai.core.coverage.model.TestLayer
import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.EnumMap
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CoverageTelemetryReporterTest {

  private val telemetryReporter: TelemetryReporter = mockk(relaxed = true)
  private val fixedClock: Clock = Clock.fixed(Instant.parse("2025-05-01T12:00:00Z"), ZoneOffset.UTC)
  private lateinit var reporter: CoverageTelemetryReporter

  @BeforeEach
  fun setUp() {
    reporter = CoverageTelemetryReporter(telemetryReporter, fixedClock)
  }

  @Test
  fun `reportSummary emits telemetry per layer`() {
    val metrics = EnumMap<TestLayer, CoverageMetric>(TestLayer::class.java)
    metrics[TestLayer.VIEW_MODEL] = CoverageMetric(80.0, 75.0)
    metrics[TestLayer.UI] = CoverageMetric(65.0, 65.0)
    metrics[TestLayer.DATA] = CoverageMetric(70.0, 70.0)
    val trendDelta = metrics.mapValues { (_, metric) -> metric.deltaFromThreshold }
    val summary =
      CoverageSummary(
        buildId = "build-123",
        timestamp = Instant.parse("2025-05-01T11:00:00Z"),
        layerMetrics = metrics,
        thresholds = metrics.mapValues { it.value.threshold },
        trendDelta = trendDelta,
        riskItems = emptyList(),
      )

    val summaryPayloads = mutableListOf<Map<String, String>>()
    every { telemetryReporter.trackInteraction("coverage_summary", any()) } answers
      {
        summaryPayloads += secondArg() as Map<String, String>
      }
    val statusPayloads = mutableListOf<Map<String, String>>()
    every { telemetryReporter.trackInteraction("coverage_status_breakdown", any()) } answers
      {
        statusPayloads += secondArg() as Map<String, String>
      }

    reporter.reportSummary(summary)

    assertThat(summaryPayloads).isNotEmpty()
    val viewModelPayload =
      summaryPayloads.first { payload ->
        payload.getValue("layer") == TestLayer.VIEW_MODEL.analyticsKey
      }
    assertThat(viewModelPayload.getValue("coverage")).isEqualTo("80.00")
    assertThat(viewModelPayload.getValue("status")).isEqualTo("EXCEEDS_TARGET")
    assertThat(statusPayloads).isNotEmpty()
    val statusMetadata = statusPayloads.last()
    assertThat(statusMetadata["below_target"]).isEqualTo("0")
    assertThat(statusMetadata["on_target"]).isEqualTo("2")
    assertThat(statusMetadata["exceeds_target"]).isEqualTo("1")
  }

  @Test
  fun `reportRiskEscalations forwards metadata`() {
    val risk =
      RiskRegisterItem(
        riskId = "risk-1",
        layer = TestLayer.DATA,
        description = "Data layer gap",
        severity = Severity.CRITICAL,
        targetBuild = "build-2025-05-01",
        status = Status.OPEN,
        mitigation = "Add repository tests",
      )

    val captured = mutableListOf<Map<String, String>>()
    every { telemetryReporter.trackInteraction("coverage_risk_escalation", any()) } answers
      {
        captured += secondArg() as Map<String, String>
      }

    reporter.reportRiskEscalations(listOf(risk))

    assertThat(captured).hasSize(1)
    val payload = captured.single()
    assertThat(payload.getValue("riskId")).isEqualTo("risk-1")
    assertThat(payload.getValue("layer")).isEqualTo(TestLayer.DATA.analyticsKey)
    assertThat(payload.getValue("actionable")).isEqualTo("true")
    assertThat(payload.getValue("hasMitigation")).isEqualTo("true")
  }
}
