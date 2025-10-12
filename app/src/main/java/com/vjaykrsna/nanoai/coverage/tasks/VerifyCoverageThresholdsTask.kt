package com.vjaykrsna.nanoai.coverage.tasks

import com.vjaykrsna.nanoai.coverage.model.CoverageMetric
import com.vjaykrsna.nanoai.coverage.model.CoverageSummary
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
    logUnmappedClasses(outcome.unmappedClasses)
    handleViolation(outcome.violation)

    System.out.println("verifyCoverage: thresholds satisfied for build ${outcome.summary.buildId}")
  }

  private data class Arguments(
    val reportXml: Path,
    val layerMap: Path?,
    val markdownOutput: Path?,
    val buildId: String?,
  ) {
    companion object {
      fun parse(arguments: Array<String>): Arguments {
        var reportXml: Path? = null
        var layerMap: Path? = null
        var markdown: Path? = null
        var buildId: String? = null

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
          buildId = buildId
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
    val markdown =
      MarkdownRenderer.render(parseResult.summary, violation, parseResult.unmappedClasses)
    return TaskOutcome(
      summary = parseResult.summary,
      unmappedClasses = parseResult.unmappedClasses,
      violation = violation,
      markdown = markdown,
    )
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
      val unmapped = mutableListOf<String>()

      processClassNodes(document, totals, unmapped)

      val metrics = buildMetrics(totals)

      val trendDelta = metrics.mapValues { (_, metric) -> metric.deltaFromThreshold }
      val timestamp = Instant.ofEpochMilli(Files.getLastModifiedTime(reportXml).toMillis())
      val buildId = buildIdOverride ?: reportXml.nameWithoutExtension

      return ParseResult(
        summary =
          CoverageSummary(
            buildId = buildId,
            timestamp = timestamp,
            layerMetrics = metrics,
            thresholds = thresholds,
            trendDelta = trendDelta,
            riskItems = emptyList(),
          ),
        unmappedClasses = unmapped,
      )
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
      unmapped: MutableList<String>,
    ) {
      val classNodes = document.getElementsByTagName("class")
      for (index in 0 until classNodes.length) {
        val node = classNodes.item(index)
        if (node is Element) {
          updateTotalsForClass(node, totals, unmapped)
        }
      }
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
  }

  private data class LayerCoverageTotals(var covered: Long = 0, var total: Long = 0)

  private data class ParseResult(val summary: CoverageSummary, val unmappedClasses: List<String>)

  private object MarkdownRenderer {
    private val timestampFormatter = DateTimeFormatter.ISO_INSTANT

    fun render(
      summary: CoverageSummary,
      violation: CoverageThresholdVerifier.ThresholdViolation?,
      unmappedClasses: List<String>,
    ): String {
      val builder = StringBuilder()
      builder.appendLine("# Coverage Thresholds")
      builder.appendLine()
      builder.appendLine("- Build: `${summary.buildId}`")
      builder.appendLine("- Generated: `${timestampFormatter.format(summary.timestamp)}`")
      builder.appendLine()
      builder.appendLine("| Layer | Coverage | Threshold | Delta | Status |")
      builder.appendLine("| --- | ---: | ---: | ---: | --- |")
      TestLayer.entries.forEach { layer ->
        val metric = summary.metricFor(layer)
        builder
          .append("| ")
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
      builder.appendLine()
      val breakdown = summary.statusBreakdown()
      builder.appendLine(
        "Status breakdown: " +
          CoverageMetric.Status.entries.joinToString(
            separator = ", ",
          ) { status ->
            "${status.name}=${breakdown[status] ?: 0}"
          },
      )
      builder.appendLine()
      if (violation == null) {
        builder.appendLine("> ✅ Coverage thresholds satisfied.")
      } else {
        builder.appendLine(
          "> ❌ Coverage below threshold for: " + violation.layers.joinToString { it.displayName },
        )
      }
      if (unmappedClasses.isNotEmpty()) {
        builder.appendLine()
        builder.appendLine("_Unmapped classes (${unmappedClasses.size}):_")
        unmappedClasses.take(MAX_UNMAPPED_SAMPLE).forEach { builder.appendLine("- $it") }
        if (unmappedClasses.size > MAX_UNMAPPED_SAMPLE) {
          builder.appendLine("- …")
        }
      }
      return builder.toString()
    }

    private fun formatPercentage(value: Double): String = String.format(Locale.US, "%.2f%%", value)

    private fun formatDelta(delta: Double): String {
      return when {
        delta > 0.0 -> String.format(Locale.US, POSITIVE_DELTA_PATTERN, delta)
        delta < 0.0 -> String.format(Locale.US, NEGATIVE_DELTA_PATTERN, delta.absoluteValue)
        else -> ZERO_DELTA_LABEL
      }
    }
  }
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
