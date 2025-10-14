package com.vjaykrsna.nanoai.coverage.domain.usecase

import com.vjaykrsna.nanoai.coverage.data.CoverageDashboardPayload
import com.vjaykrsna.nanoai.coverage.data.CoverageDashboardRepository
import com.vjaykrsna.nanoai.coverage.domain.usecase.GetCoverageReportUseCase.Result
import com.vjaykrsna.nanoai.coverage.domain.usecase.GetCoverageReportUseCase.RiskChip
import com.vjaykrsna.nanoai.coverage.model.CoverageMetric
import com.vjaykrsna.nanoai.coverage.model.TestLayer
import java.util.Locale
import javax.inject.Inject

class GetCoverageReportUseCase
@Inject
constructor(private val repository: CoverageDashboardRepository) {

  suspend operator fun invoke(): Result {
    val payload = repository.loadSnapshot()
    val layerMetrics = buildLayerMetrics(payload)
    val trendByLayer = buildTrendMap(payload)
    val risks =
      payload.risks.map { risk ->
        RiskChip(
          riskId = risk.riskId,
          title = risk.title.trim(),
          severity = risk.severity.trim().uppercase(Locale.US),
          status = risk.status.trim().uppercase(Locale.US),
        )
      }

    return Result(
      buildId = payload.buildId,
      generatedAtIso = payload.generatedAt,
      layerMetrics = layerMetrics,
      trendDelta = trendByLayer,
      risks = risks,
    )
  }

  private fun buildLayerMetrics(payload: CoverageDashboardPayload): Map<TestLayer, CoverageMetric> {
    require(payload.layers.isNotEmpty()) { "Coverage dashboard payload must contain layers" }
    return payload.layers.associate { layerPayload ->
      val layer = normalizeLayer(layerPayload.layer)
      layer to
        CoverageMetric(
          coverage = layerPayload.coverage,
          threshold = layerPayload.threshold,
        )
    }
  }

  private fun buildTrendMap(payload: CoverageDashboardPayload): Map<TestLayer, Double> {
    if (payload.trend.isEmpty()) {
      return TestLayer.entries.associateWith { DEFAULT_TREND_DELTA }
    }
    val mapped = payload.trend.mapKeys { (layerKey, _) -> normalizeLayer(layerKey) }
    return TestLayer.entries.associateWith { mapped[it] ?: DEFAULT_TREND_DELTA }
  }

  private fun normalizeLayer(raw: String): TestLayer {
    val key = raw.trim().uppercase(Locale.US)
    return TestLayer.valueOf(key)
  }

  data class Result(
    val buildId: String,
    val generatedAtIso: String,
    val layerMetrics: Map<TestLayer, CoverageMetric>,
    val trendDelta: Map<TestLayer, Double>,
    val risks: List<RiskChip>,
  )

  data class RiskChip(
    val riskId: String,
    val title: String,
    val severity: String,
    val status: String,
  )

  private companion object {
    private const val DEFAULT_TREND_DELTA = 0.0
  }
}
