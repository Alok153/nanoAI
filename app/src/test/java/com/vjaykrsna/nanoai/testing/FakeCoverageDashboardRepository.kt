package com.vjaykrsna.nanoai.testing

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.coverage.data.CoverageDashboardPayload
import com.vjaykrsna.nanoai.core.coverage.data.CoverageDashboardRepository
import com.vjaykrsna.nanoai.core.coverage.data.LayerPayload
import com.vjaykrsna.nanoai.core.coverage.data.RiskPayload

class FakeCoverageDashboardRepository : CoverageDashboardRepository {

  var snapshotResult: NanoAIResult<CoverageDashboardPayload> =
    NanoAIResult.success(defaultPayload())

  override suspend fun loadSnapshot(): NanoAIResult<CoverageDashboardPayload> = snapshotResult

  fun succeedWith(payload: CoverageDashboardPayload) {
    snapshotResult = NanoAIResult.success(payload)
  }

  fun failWith(error: NanoAIResult<CoverageDashboardPayload>) {
    require(error !is NanoAIResult.Success) { "Use succeedWith for successful payloads" }
    snapshotResult = error
  }

  companion object {
    fun defaultPayload(): CoverageDashboardPayload =
      CoverageDashboardPayload(
        buildId = "build-default",
        generatedAt = "2025-01-01T00:00:00Z",
        layers = listOf(LayerPayload(layer = "VIEW_MODEL", coverage = 80.0, threshold = 75.0)),
        trend = mapOf("VIEW_MODEL" to 1.5),
        risks =
          listOf(
            RiskPayload(
              riskId = "risk-default",
              title = "Offline writes failing",
              severity = "CRITICAL",
              status = "OPEN",
            )
          ),
      )
  }
}
