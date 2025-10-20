package com.vjaykrsna.nanoai.coverage

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.truth.Truth.assertThat
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.vjaykrsna.nanoai.coverage.domain.CoverageReportGenerator
import com.vjaykrsna.nanoai.coverage.model.CoverageMetric
import com.vjaykrsna.nanoai.coverage.model.CoverageSummary
import com.vjaykrsna.nanoai.coverage.model.CoverageTrendPoint
import com.vjaykrsna.nanoai.coverage.model.RiskRegisterItem
import com.vjaykrsna.nanoai.coverage.model.RiskRegisterItemRef
import com.vjaykrsna.nanoai.coverage.model.TestLayer
import com.vjaykrsna.nanoai.coverage.model.TestSuiteCatalogEntry
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CoverageReportContractTest {

  private val mapper = ObjectMapper()
  private val schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
  private val schema =
    schemaFactory.getSchema(
      mapper.readTree(
        javaClass.classLoader?.getResourceAsStream("schemas/coverage-report.schema.json")
          ?: error("Test schema resource missing: schemas/coverage-report.schema.json")
      )
    )

  private val fixedClock = Clock.fixed(Instant.parse("2025-10-10T12:30:00Z"), ZoneOffset.UTC)

  @Test
  fun `generated coverage report conforms to schema`() {
    val summary =
      createSummary(
        buildId = "build-2025-10-10",
        riskIds = listOf("risk-high-ui", "risk-critical-data"),
        trendDelta = mapOf(TestLayer.VIEW_MODEL to 3.2, TestLayer.UI to 1.5, TestLayer.DATA to -0.5),
      )

    val trend =
      listOf(
        CoverageTrendPoint(
          buildId = "build-2025-10-08",
          layer = TestLayer.VIEW_MODEL,
          coverage = 78.0,
          threshold = 75.0,
          recordedAt = Instant.parse("2025-10-08T12:00:00Z"),
        ),
        CoverageTrendPoint(
          buildId = "build-2025-10-09",
          layer = TestLayer.UI,
          coverage = 67.0,
          threshold = 65.0,
          recordedAt = Instant.parse("2025-10-09T12:00:00Z"),
        ),
      )

    val riskRegister =
      listOf(
        RiskRegisterItem(
          riskId = "risk-high-ui",
          layer = TestLayer.UI,
          description = "Compose semantics missing for chat composer",
          severity = RiskRegisterItem.Severity.HIGH,
          targetBuild = "build-2025-10-15",
          status = RiskRegisterItem.Status.IN_PROGRESS,
          mitigation = "Add Compose accessibility tests",
        ),
        RiskRegisterItem(
          riskId = "risk-critical-data",
          layer = TestLayer.DATA,
          description = "Room DAO lacks offline write coverage",
          severity = RiskRegisterItem.Severity.CRITICAL,
          targetBuild = "build-2025-10-20",
          status = RiskRegisterItem.Status.OPEN,
          mitigation = "Expand DAO integration tests",
        ),
      )

    val catalog =
      listOf(
        TestSuiteCatalogEntry(
          suiteId = "suite-viewmodel-chat",
          owner = "quality-engineering",
          layer = TestLayer.VIEW_MODEL,
          journey = "Chat send message",
          coverageContribution = 4.5,
          riskTags = setOf("risk-high-ui", "risk-critical-data"),
        )
      )

    val reportJson =
      CoverageReportGenerator(clock = fixedClock)
        .generate(
          summary = summary,
          trend = trend,
          riskRegister = riskRegister,
          catalog = catalog,
          branch = "main",
        )

    val validationErrors = schema.validate(mapper.readTree(reportJson))

    assertThat(validationErrors).isEmpty()
  }

  @Test
  fun `risk register is sorted by severity`() {
    val riskRegister =
      listOf(
        RiskRegisterItem(
          riskId = "risk-medium-vm",
          layer = TestLayer.VIEW_MODEL,
          description = "ViewModel emits stale state",
          severity = RiskRegisterItem.Severity.MEDIUM,
          targetBuild = "build-2025-10-18",
          status = RiskRegisterItem.Status.OPEN,
          mitigation = "Add regression suite",
        ),
        RiskRegisterItem(
          riskId = "risk-critical-data",
          layer = TestLayer.DATA,
          description = "Room DAO lacks offline write coverage",
          severity = RiskRegisterItem.Severity.CRITICAL,
          targetBuild = "build-2025-10-20",
          status = RiskRegisterItem.Status.OPEN,
          mitigation = "Expand DAO integration tests",
        ),
        RiskRegisterItem(
          riskId = "risk-low-history",
          layer = TestLayer.DATA,
          description = "History cache misses refresh occasionally",
          severity = RiskRegisterItem.Severity.LOW,
          targetBuild = null,
          status = RiskRegisterItem.Status.DEFERRED,
          mitigation = "Monitor via telemetry",
        ),
        RiskRegisterItem(
          riskId = "risk-high-ui",
          layer = TestLayer.UI,
          description = "Compose semantics missing for chat composer",
          severity = RiskRegisterItem.Severity.HIGH,
          targetBuild = "build-2025-10-15",
          status = RiskRegisterItem.Status.IN_PROGRESS,
          mitigation = "Add Compose accessibility tests",
        ),
      )

    val catalog =
      listOf(
        TestSuiteCatalogEntry(
          suiteId = "suite-coverage",
          owner = "quality-engineering",
          layer = TestLayer.VIEW_MODEL,
          journey = "Coverage guard",
          coverageContribution = 6.0,
          riskTags =
            setOf("risk-critical-data", "risk-high-ui", "risk-medium-vm", "risk-low-history"),
        )
      )

    val reportJson =
      CoverageReportGenerator(clock = fixedClock)
        .generate(
          summary =
            createSummary(
              buildId = "build-2025-10-10",
              riskIds =
                listOf("risk-low-history", "risk-critical-data", "risk-medium-vm", "risk-high-ui"),
            ),
          trend = emptyList(),
          riskRegister = riskRegister,
          catalog = catalog,
          branch = null,
        )

    val riskSeverities =
      mapper.readTree(reportJson).path("riskRegister").map { node ->
        node.path("severity").asText()
      }

    assertThat(riskSeverities).containsExactly("CRITICAL", "HIGH", "MEDIUM", "LOW").inOrder()
  }

  @Test
  fun `throws when risk tags do not match catalog`() {
    val riskRegister =
      listOf(
        RiskRegisterItem(
          riskId = "risk-critical-data",
          layer = TestLayer.DATA,
          description = "Room DAO lacks offline write coverage",
          severity = RiskRegisterItem.Severity.CRITICAL,
          targetBuild = "build-2025-10-20",
          status = RiskRegisterItem.Status.OPEN,
          mitigation = "Expand DAO integration tests",
        )
      )

    val catalog =
      listOf(
        TestSuiteCatalogEntry(
          suiteId = "suite-viewmodel-chat",
          owner = "quality-engineering",
          layer = TestLayer.VIEW_MODEL,
          journey = "Chat send message",
          coverageContribution = 4.5,
          riskTags = setOf("unrelated-risk"),
        )
      )

    assertThrows<IllegalArgumentException> {
      CoverageReportGenerator(clock = fixedClock)
        .generate(
          summary =
            createSummary(buildId = "build-2025-10-10", riskIds = listOf("risk-critical-data")),
          trend = emptyList(),
          riskRegister = riskRegister,
          catalog = catalog,
          branch = null,
        )
    }
  }

  private fun createSummary(
    buildId: String,
    riskIds: List<String>,
    trendDelta: Map<TestLayer, Double> = defaultTrendDelta(),
  ): CoverageSummary =
    CoverageSummary(
      buildId = buildId,
      timestamp = Instant.parse("2025-10-10T12:15:00Z"),
      layerMetrics = defaultLayerMetrics(),
      thresholds = defaultThresholds(),
      trendDelta = trendDelta,
      riskItems = riskIds.map(::RiskRegisterItemRef),
    )

  private fun defaultLayerMetrics(): Map<TestLayer, CoverageMetric> =
    mapOf(
      TestLayer.VIEW_MODEL to CoverageMetric(coverage = 81.2, threshold = 75.0),
      TestLayer.UI to CoverageMetric(coverage = 68.5, threshold = 65.0),
      TestLayer.DATA to CoverageMetric(coverage = 72.0, threshold = 70.0),
    )

  private fun defaultThresholds(): Map<TestLayer, Double> =
    mapOf(TestLayer.VIEW_MODEL to 75.0, TestLayer.UI to 65.0, TestLayer.DATA to 70.0)

  private fun defaultTrendDelta(): Map<TestLayer, Double> =
    mapOf(TestLayer.VIEW_MODEL to 3.2, TestLayer.UI to 1.5, TestLayer.DATA to -0.5)
}
