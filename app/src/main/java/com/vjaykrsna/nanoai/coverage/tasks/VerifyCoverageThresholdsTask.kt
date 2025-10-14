package com.vjaykrsna.nanoai.coverage.tasks

import com.vjaykrsna.nanoai.coverage.domain.CoverageReportGenerator
import com.vjaykrsna.nanoai.coverage.model.CoverageMetric
import com.vjaykrsna.nanoai.coverage.model.CoverageSummary
import com.vjaykrsna.nanoai.coverage.model.CoverageTrendPoint
import com.vjaykrsna.nanoai.coverage.model.TestLayer
import com.vjaykrsna.nanoai.coverage.verification.CoverageThresholdVerifier
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.outputStream
import kotlin.io.path.readText
import kotlin.math.absoluteValue
import kotlin.system.exitProcess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.w3c.dom.Element

/**
 * Host-side entry-point that parses merged JaCoCo XML coverage reports, aggregates coverage per
 * [TestLayer], writes a human-readable markdown summary, and enforces minimum thresholds using
 * [CoverageThresholdVerifier]. Intended to run from Gradle across CI and local environments.
 */
object VerifyCoverageThresholdsTask {

  private const val DEFAULT_LAYER_MAP = "config/coverage/layer-map.json"
  private const val EXIT_HELP = 0
  private const val EXIT_USAGE_ERROR = 64
  private const val EXIT_DATA_ERROR = 66
  private const val EXIT_THRESHOLD_FAILURE = 1
  private const val MAX_UNMAPPED_SAMPLE = 10
  private const val DEFAULT_VIEW_MODEL_THRESHOLD = 75.0
  private const val DEFAULT_UI_THRESHOLD = 65.0
  private const val DEFAULT_DATA_THRESHOLD = 70.0
  private const val PERCENT_SCALE = 100.0
  private const val ZERO_DELTA_LABEL = "0.00pp"
  private const val POSITIVE_DELTA_PATTERN = "+%.2fpp"
  private const val NEGATIVE_DELTA_PATTERN = "-%.2fpp"
  private val json = Json { ignoreUnknownKeys = true }
  private val DEFAULT_THRESHOLDS =
    mapOf(
      TestLayer.VIEW_MODEL to DEFAULT_VIEW_MODEL_THRESHOLD,
      TestLayer.UI to DEFAULT_UI_THRESHOLD,
      TestLayer.DATA to DEFAULT_DATA_THRESHOLD,
    )

  @JvmStatic
  fun main(rawArgs: Array<String>) {
    val parsedArgs = parseArgumentsOrExit(rawArgs)
    val layerMapPath = parsedArgs.layerMap ?: Path.of(DEFAULT_LAYER_MAP)

    validateInputPaths(parsedArgs.reportXml, layerMapPath)

    val outcome = evaluateReport(parsedArgs, layerMapPath)

    writeMarkdown(outcome.markdown, parsedArgs.markdownOutput)
    writeJsonReport(outcome, parsedArgs.jsonOutput)
    logUnmappedClasses(outcome.unmappedClasses)
    handleViolation(outcome.violation)

    System.out.println("verifyCoverage: thresholds satisfied for build ${outcome.summary.buildId}")
  }

  private data class Arguments(
    val reportXml: Path,
    val layerMap: Path?,
    val markdownOutput: Path?,
    val buildId: String?,
    val jsonOutput: Path?,
  ) {
    companion object {
      @Suppress("CyclomaticComplexMethod")
      fun parse(arguments: Array<String>): Arguments {
        var reportXml: Path? = null
        var layerMap: Path? = null
        var markdown: Path? = null
        var buildId: String? = null
        var jsonOutput: Path? = null

        var index = 0
        while (index < arguments.size) {
          when (val arg = arguments[index]) {
            "--report-xml" -> {
              reportXml = Path.of(next(arguments, ++index, arg))
            }
            "--layer-map" -> {
              layerMap = Path.of(next(arguments, ++index, arg))
            }
            "--markdown" -> {
              markdown = Path.of(next(arguments, ++index, arg))
            }
            "--build-id" -> {
              buildId = next(arguments, ++index, arg)
            }
            "--json" -> {
              jsonOutput = Path.of(next(arguments, ++index, arg))
            }
            "--help",
            "-h" -> {
              printUsage(System.out)
              exitProcess(EXIT_HELP)
            }
            else -> throw IllegalArgumentException("unknown argument $arg")
          }
          index += 1
        }

        val xml = reportXml ?: throw IllegalArgumentException("--report-xml is required")
        return Arguments(
          reportXml = xml,
          layerMap = layerMap,
          markdownOutput = markdown,
          buildId = buildId,
          jsonOutput = jsonOutput,
        )
      }

      private fun next(arguments: Array<String>, index: Int, flag: String): String {
        return arguments.getOrNull(index)
          ?: throw IllegalArgumentException("Missing value for $flag")
      }

      fun printUsage(stream: PrintStream) {
        stream.println(
          buildString {
              appendLine("Usage: VerifyCoverageThresholdsTask --report-xml <path> [options]")
              appendLine("  --report-xml <path>   Absolute path to the merged JaCoCo XML report.")
              appendLine(
                "  --layer-map <path>    Optional path to regex-based TestLayer mappings (default: $DEFAULT_LAYER_MAP)."
              )
              appendLine(
                "  --markdown <path>     Optional path to write markdown summary for CI artifacts."
              )
              appendLine(
                "  --build-id <value>    Optional override for the coverage summary build identifier."
              )
              appendLine(
                "  --json <path>         Optional path to write a JSON payload matching coverage-report.schema.json."
              )
            }
            .trimEnd()
        )
      }
    }
  }

  private fun parseArgumentsOrExit(rawArgs: Array<String>): Arguments {
    return runCatching { Arguments.parse(rawArgs) }
      .getOrElse { error ->
        System.err.println("verifyCoverage: ${error.message}")
        Arguments.printUsage(System.err)
        exitProcess(EXIT_USAGE_ERROR)
      }
  }

  private fun validateInputPaths(reportXml: Path, layerMapPath: Path) {
    if (!reportXml.exists() || !reportXml.isRegularFile()) {
      System.err.println("verifyCoverage: coverage report not found at $reportXml")
      exitProcess(EXIT_DATA_ERROR)
    }
    if (!layerMapPath.exists() || !layerMapPath.isRegularFile()) {
      System.err.println("verifyCoverage: layer map not found at $layerMapPath")
      exitProcess(EXIT_DATA_ERROR)
    }
  }

  private fun evaluateReport(parsedArgs: Arguments, layerMapPath: Path): TaskOutcome {
    val classifier = LayerClassifier.fromConfig(layerMapPath, json)
    val parser = CoverageReportParser(classifier)
    val parseResult = parser.parse(parsedArgs.reportXml, parsedArgs.buildId)
    val violation = verifyThresholds(parseResult.summary)
    val trend = buildTrendPoints(parseResult.summary)
    val markdown =
      MarkdownRenderer.render(
        parseResult.summary,
        violation,
        parseResult.unmappedClasses,
        trend,
      )
    return TaskOutcome(
      summary = parseResult.summary,
      unmappedClasses = parseResult.unmappedClasses,
      violation = violation,
      markdown = markdown,
      trend = trend,
    )
  }

  private fun buildTrendPoints(summary: CoverageSummary): List<CoverageTrendPoint> {
    val points =
      TestLayer.entries.map { layer ->
        val metric = summary.metricFor(layer)
        CoverageTrendPoint.fromMetric(
          buildId = summary.buildId,
          layer = layer,
          metric = metric,
          recordedAt = summary.timestamp,
        )
      }
    CoverageTrendPoint.validateSequence(points)
    return points
  }

  private fun verifyThresholds(
    summary: CoverageSummary
  ): CoverageThresholdVerifier.ThresholdViolation? {
    return runCatching { CoverageThresholdVerifier().verify(summary) }.exceptionOrNull()
      as? CoverageThresholdVerifier.ThresholdViolation
  }

  private fun writeMarkdown(markdown: String, markdownOutput: Path?) {
    markdownOutput?.let { path ->
      path.parent?.let { Files.createDirectories(it) }
      path.outputStream().use { output -> output.write(markdown.toByteArray(Charsets.UTF_8)) }
    }
  }

  private fun writeJsonReport(outcome: TaskOutcome, jsonOutput: Path?) {
    jsonOutput?.let { path ->
      path.parent?.let { Files.createDirectories(it) }
      val payload =
        CoverageReportGenerator()
          .generate(
            summary = outcome.summary,
            trend = outcome.trend,
            riskRegister = emptyList(),
            catalog = emptyList(),
            branch = resolveBranch(),
          )
      path.outputStream().use { output -> output.write(payload.toByteArray(Charsets.UTF_8)) }
    }
  }

  private fun logUnmappedClasses(unmappedClasses: List<String>) {
    if (unmappedClasses.isEmpty()) {
      return
    }
    System.out.println(
      "verifyCoverage: ${unmappedClasses.size} classes were not mapped to a TestLayer; " +
        "first missing=${unmappedClasses.first()}"
    )
  }

  private fun handleViolation(violation: CoverageThresholdVerifier.ThresholdViolation?) {
    if (violation == null) {
      return
    }
    System.err.println(
      "verifyCoverage: coverage below threshold for " +
        violation.layers.joinToString { it.displayName },
    )
    exitProcess(EXIT_THRESHOLD_FAILURE)
  }

  private data class TaskOutcome(
    val summary: CoverageSummary,
    val unmappedClasses: List<String>,
    val violation: CoverageThresholdVerifier.ThresholdViolation?,
    val markdown: String,
    val trend: List<CoverageTrendPoint>,
  )

  private class LayerClassifier(
    private val orderedPatterns: List<PatternEntry>,
    private val defaultLayer: TestLayer?,
  ) {
    data class PatternEntry(val layer: TestLayer, val regex: Regex)

    fun classify(className: String): TestLayer? {
      val normalised = className.replace('.', '/')
      for (entry in orderedPatterns) {
        if (entry.regex.containsMatchIn(normalised)) {
          return entry.layer
        }
      }
      return defaultLayer
    }

    companion object {
      fun fromConfig(path: Path, json: Json): LayerClassifier {
        val content = path.readText()
        val root = json.parseToJsonElement(content).jsonObject
        val defaultLayer = root["_default"]?.asLayer()
        val patterns = mutableListOf<PatternEntry>()
        for ((key, value) in root) {
          if (key == "_default") continue
          val layer = key.asLayer()
          val regexes = value.jsonArrayOrEmpty().map { Regex(it.jsonPrimitive.content) }
          regexes.forEach { regex -> patterns += PatternEntry(layer, regex) }
        }
        return LayerClassifier(patterns, defaultLayer)
      }
    }
  }

  private class CoverageReportParser(
    private val classifier: LayerClassifier,
    private val thresholds: Map<TestLayer, Double> = DEFAULT_THRESHOLDS,
  ) {
    fun parse(reportXml: Path, buildIdOverride: String?): ParseResult {
      val document = parseDocument(reportXml)
      val totals = initialiseTotals()
      val unmapped = processClassNodes(document, totals)

      val metrics = buildMetrics(totals)

      val trendDelta = computeTrendDelta(metrics)
      val summary = buildSummary(metrics, trendDelta, reportXml, buildIdOverride)

      return ParseResult(summary = summary, unmappedClasses = unmapped)
    }

    private fun parseDocument(reportXml: Path) =
      DocumentBuilderFactory.newInstance()
        .apply { isNamespaceAware = false }
        .newDocumentBuilder()
        .parse(reportXml.toFile())

    private fun initialiseTotals(): MutableMap<TestLayer, LayerCoverageTotals> {
      return TestLayer.entries.associateWith { LayerCoverageTotals() }.toMutableMap()
    }

    private fun processClassNodes(
      document: org.w3c.dom.Document,
      totals: MutableMap<TestLayer, LayerCoverageTotals>,
    ): List<String> {
      val unmapped = mutableListOf<String>()
      val classNodes = document.getElementsByTagName("class")
      for (index in 0 until classNodes.length) {
        val node = classNodes.item(index)
        if (node is Element) {
          updateTotalsForClass(node, totals, unmapped)
        }
      }
      return unmapped
    }

    private fun updateTotalsForClass(
      node: Element,
      totals: MutableMap<TestLayer, LayerCoverageTotals>,
      unmapped: MutableList<String>,
    ) {
      val className = node.getAttribute("name")
      if (className.isBlank()) {
        return
      }
      val layer = classifier.classify(className)
      if (layer == null) {
        unmapped += className
        return
      }
      val lineCounter = findLineCounter(node)
      if (lineCounter != null) {
        val missed = lineCounter.getAttribute("missed").toLong()
        val covered = lineCounter.getAttribute("covered").toLong()
        val total = missed + covered
        if (total != 0L) {
          val layerTotals = totals.getValue(layer)
          layerTotals.covered += covered
          layerTotals.total += total
        }
      }
    }

    private fun buildMetrics(
      totals: Map<TestLayer, LayerCoverageTotals>
    ): LinkedHashMap<TestLayer, CoverageMetric> {
      val metrics = linkedMapOf<TestLayer, CoverageMetric>()
      TestLayer.entries.forEach { layer ->
        val totalsForLayer = totals.getValue(layer)
        metrics[layer] =
          CoverageMetric(
            coverage = computeCoverage(totalsForLayer),
            threshold = thresholds.getValue(layer),
          )
      }
      return metrics
    }

    private fun computeCoverage(totals: LayerCoverageTotals): Double {
      if (totals.total == 0L) {
        return 0.0
      }
      return totals.covered.toDouble() / totals.total.toDouble() * PERCENT_SCALE
    }

    private fun computeTrendDelta(metrics: Map<TestLayer, CoverageMetric>): Map<TestLayer, Double> =
      metrics.mapValues { (_, metric) -> metric.deltaFromThreshold }

    private fun buildSummary(
      metrics: LinkedHashMap<TestLayer, CoverageMetric>,
      trendDelta: Map<TestLayer, Double>,
      reportXml: Path,
      buildIdOverride: String?,
    ): CoverageSummary {
      val timestamp = Instant.ofEpochMilli(Files.getLastModifiedTime(reportXml).toMillis())
      val buildId = buildIdOverride ?: reportXml.nameWithoutExtension
      return CoverageSummary(
        buildId = buildId,
        timestamp = timestamp,
        layerMetrics = metrics,
        thresholds = thresholds,
        trendDelta = trendDelta,
        riskItems = emptyList(),
      )
    }
  }

  private data class LayerCoverageTotals(var covered: Long = 0, var total: Long = 0)

  private data class ParseResult(val summary: CoverageSummary, val unmappedClasses: List<String>)

  private object MarkdownRenderer {
    private val timestampFormatter = DateTimeFormatter.ISO_INSTANT

    fun render(
      summary: CoverageSummary,
      violation: CoverageThresholdVerifier.ThresholdViolation?,
      unmappedClasses: List<String>,
      trend: List<CoverageTrendPoint>,
    ): String = buildString {
      appendHeader(summary)
      appendCoverageTable(summary)
      appendLine()
      appendStatusBreakdown(summary)
      appendLine()
      appendThresholdMessage(violation)
      appendLine()
      appendTrendSection(summary, trend)
      appendLine()
      appendRiskSection(summary)
      appendUnmappedSection(unmappedClasses)
    }

    private fun formatPercentage(value: Double): String = String.format(Locale.US, "%.2f%%", value)

    private fun formatDelta(delta: Double): String {
      return when {
        delta > 0.0 -> String.format(Locale.US, POSITIVE_DELTA_PATTERN, delta)
        delta < 0.0 -> String.format(Locale.US, NEGATIVE_DELTA_PATTERN, delta.absoluteValue)
        else -> ZERO_DELTA_LABEL
      }
    }

    private fun StringBuilder.appendHeader(summary: CoverageSummary) {
      appendLine("# Coverage Thresholds")
      appendLine()
      appendLine("- Build: `${summary.buildId}`")
      appendLine("- Generated: `${timestampFormatter.format(summary.timestamp)}`")
      appendLine()
    }

    private fun StringBuilder.appendCoverageTable(summary: CoverageSummary) {
      appendLine("| Layer | Coverage | Threshold | Delta | Status |")
      appendLine("| --- | ---: | ---: | ---: | --- |")
      TestLayer.entries.forEach { layer ->
        val metric = summary.metricFor(layer)
        append("| ")
          .append(layer.displayName)
          .append(" | ")
          .append(formatPercentage(metric.coverage))
          .append(" | ")
          .append(formatPercentage(metric.threshold))
          .append(" | ")
          .append(formatDelta(metric.deltaFromThreshold))
          .append(" | ")
          .append(metric.status.name)
          .appendLine(" |")
      }
    }

    private fun StringBuilder.appendStatusBreakdown(summary: CoverageSummary) {
      val breakdown = summary.statusBreakdown()
      appendLine(
        "Status breakdown: " +
          CoverageMetric.Status.entries.joinToString(separator = ", ") { status ->
            "${status.name}=${breakdown[status] ?: 0}"
          },
      )
    }

    private fun StringBuilder.appendThresholdMessage(
      violation: CoverageThresholdVerifier.ThresholdViolation?,
    ) {
      if (violation == null) {
        appendLine("> ✅ Coverage thresholds satisfied.")
      } else {
        appendLine(
          "> ❌ Coverage below threshold for: " + violation.layers.joinToString { it.displayName },
        )
      }
    }

    private fun StringBuilder.appendTrendSection(
      summary: CoverageSummary,
      trend: List<CoverageTrendPoint>,
    ) {
      appendLine("## Trend Snapshot")
      if (trend.isEmpty()) {
        appendLine("_No historical trend data captured yet._")
        return
      }
      TestLayer.entries.forEach { layer ->
        val latest = trend.lastOrNull { it.layer == layer }
        if (latest == null) {
          appendLine("- ${layer.displayName}: no recorded history")
        } else {
          val delta = summary.trendDeltaFor(layer)
          appendLine(
            "- ${layer.displayName}: ${formatPercentage(latest.coverage)} (${formatDelta(delta)})",
          )
        }
      }
    }

    private fun StringBuilder.appendRiskSection(summary: CoverageSummary) {
      appendLine("## Risk Register")
      if (summary.riskItems.isEmpty()) {
        appendLine("_No linked risk register items for this build._")
      } else {
        summary.riskItems.forEach { ref -> appendLine("- `${ref.riskId}`") }
      }
    }

    private fun StringBuilder.appendUnmappedSection(unmappedClasses: List<String>) {
      if (unmappedClasses.isEmpty()) return
      appendLine()
      appendLine("_Unmapped classes (${unmappedClasses.size}):_")
      unmappedClasses.take(MAX_UNMAPPED_SAMPLE).forEach { appendLine("- $it") }
      if (unmappedClasses.size > MAX_UNMAPPED_SAMPLE) {
        appendLine("- …")
      }
    }
  }
}

private fun resolveBranch(): String? {
  val env = System.getenv()
  return env["GITHUB_HEAD_REF"]
    ?: env["GITHUB_REF_NAME"]
    ?: env["BRANCH_NAME"]
    ?: env["CI_BRANCH"]
    ?: env["GIT_BRANCH"]
}

private fun String.asLayer(): TestLayer {
  return try {
    TestLayer.valueOf(this)
  } catch (error: IllegalArgumentException) {
    throw IllegalArgumentException("Unknown layer name '$this' in layer map", error)
  }
}

private fun JsonElement.jsonArrayOrEmpty(): JsonArray {
  return when (this) {
    is JsonArray -> this
    else -> JsonArray(emptyList())
  }
}

private fun JsonElement.asLayer(): TestLayer =
  when (this) {
    is JsonPrimitive -> this.content.asLayer()
    else -> throw IllegalArgumentException("Layer names must be string primitives")
  }

private fun findLineCounter(element: Element): Element? {
  for (child in element.childElements()) {
    if (child.tagName == "counter" && child.getAttribute("type") == "LINE") {
      return child
    }
  }
  return null
}

private fun Element.childElements(): Sequence<Element> = sequence {
  for (index in 0 until childNodes.length) {
    val node = childNodes.item(index)
    if (node is Element) {
      yield(node)
    }
  }
}
