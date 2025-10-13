package com.vjaykrsna.nanoai.contracts

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.networknt.schema.ValidationMessage
import com.vjaykrsna.nanoai.coverage.domain.CoverageReportGenerator
import com.vjaykrsna.nanoai.coverage.model.CoverageMetric
import com.vjaykrsna.nanoai.coverage.model.CoverageSummary
import com.vjaykrsna.nanoai.coverage.model.CoverageTrendPoint
import com.vjaykrsna.nanoai.coverage.model.RiskRegisterItem
import com.vjaykrsna.nanoai.coverage.model.TestLayer
import com.vjaykrsna.nanoai.coverage.model.TestSuiteCatalogEntry
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CoverageReportSchemaTest {
  private lateinit var schemaPath: Path
  private lateinit var schemaFactory: JsonSchemaFactory
  private val objectMapper = ObjectMapper()

  @BeforeEach
  fun setUp() {
    schemaPath =
      Path.of(
        System.getProperty("user.dir"),
        "specs",
        "005-improve-test-coverage",
        "contracts",
        "coverage-report.schema.json",
      )
    check(Files.exists(schemaPath)) { "Expected coverage schema at ${schemaPath.toAbsolutePath()}" }
    schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
  }

  @Test
  fun `generator payload validates against coverage schema`() {
    val generator = CoverageReportGenerator()
    val payload =
      generator.generate(
        summary = sampleSummary(),
        trend = sampleTrend(),
        riskRegister = sampleRisks(),
        catalog = sampleCatalog(),
        branch = "main",
      )

    val schema = schemaFactory.getSchema(Files.newInputStream(schemaPath))
    val validationMessages: Set<ValidationMessage> = schema.validate(objectMapper.readTree(payload))

    assertThat(validationMessages).isEmpty()
  }

  @Test
  fun `generator payload surfaces threshold metadata and risk references`() {
    val generator = CoverageReportGenerator()
    val payload =
      Json.parseToJsonElement(
          generator.generate(
            summary = sampleSummary(),
            trend = sampleTrend(),
            riskRegister = sampleRisks(),
            catalog = sampleCatalog(),
            branch = "main",
          )
        )
        .jsonObject

    val trendEntries = payload.requireArray("trend")
    assertThat(trendEntries).isNotEmpty()
    trendEntries.forEach { entry ->
      val trendObject = entry.jsonObject
      assertWithMessage("Trend entries must include layer thresholds for dashboards")
        .that(trendObject["threshold"])
        .isNotNull()
    }

    val riskRegister = payload.requireArray("riskRegister")
    assertThat(riskRegister).isNotEmpty()
    val firstRisk = riskRegister[0].jsonObject
    assertWithMessage("Risk register entries must reference summary risk IDs for cross-links")
      .that(firstRisk["references"])
      .isNotNull()
  }

  private fun JsonObject.requireArray(key: String) =
    get(key)?.let { element -> element.jsonArray } ?: error("Expected array '$key' in payload")

  private fun sampleSummary(): CoverageSummary =
    CoverageSummary(
      buildId = "build-123",
      timestamp = Instant.parse("2025-10-10T12:00:00Z"),
      layerMetrics =
        mapOf(
          TestLayer.VIEW_MODEL to CoverageMetric(coverage = 74.0, threshold = 75.0),
          TestLayer.UI to CoverageMetric(coverage = 66.0, threshold = 65.0),
          TestLayer.DATA to CoverageMetric(coverage = 69.5, threshold = 70.0),
        ),
      thresholds =
        mapOf(
          TestLayer.VIEW_MODEL to 75.0,
          TestLayer.UI to 65.0,
          TestLayer.DATA to 70.0,
        ),
      trendDelta =
        mapOf(
          TestLayer.VIEW_MODEL to -1.5,
          TestLayer.UI to 0.0,
          TestLayer.DATA to 2.1,
        ),
      riskItems = listOf("risk-offline-catalog"),
    )

  private fun sampleTrend(): List<CoverageTrendPoint> =
    listOf(
      CoverageTrendPoint(
        buildId = "build-121",
        layer = TestLayer.VIEW_MODEL,
        coverage = 76.0,
        threshold = 75.0,
        recordedAt = Instant.parse("2025-10-08T12:00:00Z"),
      ),
      CoverageTrendPoint(
        buildId = "build-122",
        layer = TestLayer.VIEW_MODEL,
        coverage = 74.0,
        threshold = 75.0,
        recordedAt = Instant.parse("2025-10-09T12:00:00Z"),
      ),
    )

  private fun sampleRisks(): List<RiskRegisterItem> =
    listOf(
      RiskRegisterItem(
        riskId = "risk-offline-catalog",
        layer = TestLayer.DATA,
        description = "Offline catalog hydration fails",
        severity = RiskRegisterItem.Severity.HIGH,
        targetBuild = "build-130",
        status = RiskRegisterItem.Status.OPEN,
        mitigation = "Add cached fallback and retry queue",
      ),
    )

  private fun sampleCatalog(): List<TestSuiteCatalogEntry> =
    listOf(
      TestSuiteCatalogEntry(
        suiteId = "suite-catalog-fallback",
        owner = "quality-engineering",
        layer = TestLayer.DATA,
        journey = "Model catalog fallback",
        coverageContribution = 4.0,
        riskTags = setOf("risk-offline-catalog"),
      ),
    )
}
