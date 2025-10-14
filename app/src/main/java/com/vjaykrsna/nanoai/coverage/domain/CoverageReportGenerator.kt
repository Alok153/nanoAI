package com.vjaykrsna.nanoai.coverage.domain

import com.vjaykrsna.nanoai.coverage.model.CoverageMetric
import com.vjaykrsna.nanoai.coverage.model.CoverageSummary
import com.vjaykrsna.nanoai.coverage.model.CoverageTrendPoint
import com.vjaykrsna.nanoai.coverage.model.RiskRegisterItem
import com.vjaykrsna.nanoai.coverage.model.RiskRegisterItemRef
import com.vjaykrsna.nanoai.coverage.model.TestLayer
import com.vjaykrsna.nanoai.coverage.model.TestSuiteCatalogEntry
import java.time.Clock
import java.util.Comparator
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Serialises coverage domain models into the JSON contract consumed by stakeholders and CI. */
class CoverageReportGenerator(
  private val clock: Clock = Clock.systemUTC(),
  private val json: Json = Json { encodeDefaults = true },
) {
  fun generate(
    summary: CoverageSummary,
    trend: List<CoverageTrendPoint>,
    riskRegister: List<RiskRegisterItem>,
    catalog: List<TestSuiteCatalogEntry>,
    branch: String?,
  ): String {
    val layersObject = JsonObject(buildLayerEntries(summary))
    val thresholdsObject = JsonObject(buildThresholdEntries(summary))
    val trendArray = JsonArray(buildTrendEntries(trend, summary))
    val sortedRisks = sortRisksBySeverity(riskRegister)
    ensureCatalogCoverage(summary, sortedRisks, catalog)
    val riskArray = JsonArray(buildRiskEntries(sortedRisks, summary.riskItems))

    val payload = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
    payload["buildId"] = JsonPrimitive(summary.buildId)
    branch?.let { payload["branch"] = JsonPrimitive(it) }
    payload["generatedAt"] = JsonPrimitive(clock.instant().toString())
    payload["layers"] = layersObject
    payload["thresholds"] = thresholdsObject
    payload["trend"] = trendArray
    if (riskArray.isNotEmpty()) {
      payload["riskRegister"] = riskArray
    }

    return json.encodeToString(JsonObject(payload))
  }

  private fun buildLayerEntries(
    summary: CoverageSummary
  ): Map<String, kotlinx.serialization.json.JsonElement> =
    summary.layerMetrics
      .mapValues { (layer, metric) ->
        JsonObject(
            mapOf(
              "coverage" to JsonPrimitive(metric.coverage),
              "threshold" to JsonPrimitive(metric.threshold),
              "status" to JsonPrimitive(metric.status.name),
            ),
          )
          .also { ensureSummaryThresholdConsistency(summary, layer, metric) }
      }
      .mapKeys { (layer, _) -> layer.schemaKey }

  private fun buildThresholdEntries(
    summary: CoverageSummary
  ): Map<String, kotlinx.serialization.json.JsonElement> =
    summary.thresholds
      .mapKeys { (layer, _) -> layer.schemaKey }
      .mapValues { (_, value) -> JsonPrimitive(value) }

  private fun buildTrendEntries(
    points: List<CoverageTrendPoint>,
    summary: CoverageSummary,
  ): List<kotlinx.serialization.json.JsonElement> =
    points.map { point ->
      ensureTrendThresholdConsistency(summary, point)
      JsonObject(
        mapOf(
          "buildId" to JsonPrimitive(point.buildId),
          "layer" to JsonPrimitive(point.layer.name),
          "coverage" to JsonPrimitive(point.coverage),
          "threshold" to JsonPrimitive(point.threshold),
          "delta" to JsonPrimitive(summary.trendDeltaFor(point.layer)),
        ),
      )
    }

  private fun buildRiskEntries(
    risks: List<RiskRegisterItem>,
    references: List<RiskRegisterItemRef>,
  ): List<kotlinx.serialization.json.JsonElement> {
    val referencesByRiskId = references.groupBy { it.riskId }
    return risks.map { risk ->
      JsonObject(
        buildMap {
          put("riskId", JsonPrimitive(risk.riskId))
          put("layer", JsonPrimitive(risk.layer.name))
          put("severity", JsonPrimitive(risk.severity.name))
          put("status", JsonPrimitive(risk.status.name))
          risk.targetBuild?.let { put("targetBuild", JsonPrimitive(it)) }
          risk.mitigation?.let { put("mitigation", JsonPrimitive(it)) }
          referencesByRiskId[risk.riskId]
            ?.takeIf { it.isNotEmpty() }
            ?.let { refs -> put("references", JsonArray(refs.map { JsonPrimitive(it.riskId) })) }
        },
      )
    }
  }

  private fun ensureCatalogCoverage(
    summary: CoverageSummary,
    risks: List<RiskRegisterItem>,
    catalog: List<TestSuiteCatalogEntry>,
  ) {
    if (risks.isEmpty()) return
    val catalogRiskTags = catalog.flatMap { entry -> entry.riskTags }.toSet()
    risks.forEach { risk ->
      require(catalogRiskTags.contains(risk.riskId)) {
        "No test suite catalog entry mitigates risk ${risk.riskId}"
      }
    }
    if (summary.riskItems.isNotEmpty()) {
      val uncovered =
        summary.riskItems.map { it.riskId }.filterNot { catalogRiskTags.contains(it) }.toSet()
      require(uncovered.isEmpty()) {
        "Summary references risks without catalog coverage: $uncovered"
      }
    }
  }

  private fun sortRisksBySeverity(risks: List<RiskRegisterItem>): List<RiskRegisterItem> {
    if (risks.isEmpty()) return risks
    return risks.sortedWith(
      Comparator.comparing<RiskRegisterItem, Int> { severityPriority[it.severity] ?: Int.MAX_VALUE }
        .thenComparing(RiskRegisterItem::riskId)
    )
  }

  private val severityPriority: Map<RiskRegisterItem.Severity, Int> =
    mapOf(
      RiskRegisterItem.Severity.CRITICAL to PRIORITY_CRITICAL,
      RiskRegisterItem.Severity.HIGH to PRIORITY_HIGH,
      RiskRegisterItem.Severity.MEDIUM to PRIORITY_MEDIUM,
      RiskRegisterItem.Severity.LOW to PRIORITY_LOW,
    )

  private fun ensureSummaryThresholdConsistency(
    summary: CoverageSummary,
    layer: TestLayer,
    metric: CoverageMetric,
  ) {
    val summaryThreshold = summary.thresholdFor(layer)
    require(summaryThreshold == metric.threshold) {
      "Threshold for $layer in summary does not match metric definition"
    }
  }

  private fun ensureTrendThresholdConsistency(
    summary: CoverageSummary,
    point: CoverageTrendPoint,
  ) {
    val expected = summary.thresholdFor(point.layer)
    require(expected == point.threshold) {
      "Trend point ${point.buildId} threshold ${point.threshold} does not align with summary threshold $expected"
    }
  }
}

private val TestLayer.schemaKey: String
  get() =
    when (this) {
      TestLayer.VIEW_MODEL -> "viewModel"
      TestLayer.UI -> "ui"
      TestLayer.DATA -> "data"
    }

private const val PRIORITY_CRITICAL = 0
private const val PRIORITY_HIGH = 1
private const val PRIORITY_MEDIUM = 2
private const val PRIORITY_LOW = 3
