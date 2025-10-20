package com.vjaykrsna.nanoai.telemetry

import com.vjaykrsna.nanoai.coverage.model.CoverageMetric
import com.vjaykrsna.nanoai.coverage.model.CoverageSummary
import com.vjaykrsna.nanoai.coverage.model.RiskRegisterItem
import com.vjaykrsna.nanoai.coverage.model.TestLayer
import java.time.Clock
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Publishes structured telemetry events for coverage summaries and risk escalations while ensuring
 * no personally identifiable information is attached to the payload.
 */
@Singleton
class CoverageTelemetryReporter
@Inject
constructor(
  private val telemetryReporter: TelemetryReporter,
  private val clock: Clock = Clock.systemUTC(),
) {

  fun reportSummary(summary: CoverageSummary) {
    val generatedAt = summary.timestamp.toString()
    TestLayer.entries.forEach { layer ->
      val metric = summary.metricFor(layer)
      telemetryReporter.trackInteraction(
        event = "coverage_summary",
        metadata =
          mapOf(
            "buildId" to summary.buildId,
            "generatedAt" to generatedAt,
            "layer" to layer.analyticsKey,
            "coverage" to metric.coverage.formatPercentage(),
            "threshold" to metric.threshold.formatPercentage(),
            "delta" to metric.deltaFromThreshold.formatDelta(),
            "status" to metric.status.name,
          ),
      )
    }

    val breakdown = summary.statusBreakdown()
    telemetryReporter.trackInteraction(
      event = "coverage_status_breakdown",
      metadata =
        mapOf(
          "buildId" to summary.buildId,
          CoverageMetric.Status.BELOW_TARGET.name.lowercase() to
            breakdown[CoverageMetric.Status.BELOW_TARGET].orZeroString(),
          CoverageMetric.Status.ON_TARGET.name.lowercase() to
            breakdown[CoverageMetric.Status.ON_TARGET].orZeroString(),
          CoverageMetric.Status.EXCEEDS_TARGET.name.lowercase() to
            breakdown[CoverageMetric.Status.EXCEEDS_TARGET].orZeroString(),
        ),
    )
  }

  fun reportRiskEscalations(risks: List<RiskRegisterItem>) {
    if (risks.isEmpty()) return
    val now = clock.instant()
    risks.forEach { risk ->
      val metadata =
        mutableMapOf(
          "riskId" to risk.riskId,
          "layer" to risk.layer.analyticsKey,
          "severity" to risk.severity.name,
          "status" to risk.status.name,
          "actionable" to risk.isActionable(now).toString(),
          "hasMitigation" to (!risk.mitigation.isNullOrBlank()).toString(),
        )
      risk.targetBuild?.let { target ->
        if (target.isNotBlank()) {
          metadata["targetBuild"] = target
        }
      }
      telemetryReporter.trackInteraction(event = "coverage_risk_escalation", metadata = metadata)
    }
  }

  private fun Double.formatPercentage(): String = String.format(Locale.US, "%.2f", this)

  private fun Double.formatDelta(): String =
    when {
      this > 0.0 -> String.format(Locale.US, "+%.2f", this)
      this < 0.0 -> String.format(Locale.US, "%.2f", this)
      else -> "0.00"
    }

  private fun Int?.orZeroString(): String = (this ?: 0).toString()
}
