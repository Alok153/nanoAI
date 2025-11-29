package com.vjaykrsna.nanoai.core.coverage.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.coverage.data.CoverageDashboardPayload
import com.vjaykrsna.nanoai.core.coverage.data.LayerPayload
import com.vjaykrsna.nanoai.core.coverage.data.RiskPayload
import com.vjaykrsna.nanoai.core.coverage.model.TestLayer
import com.vjaykrsna.nanoai.testing.FakeCoverageDashboardRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetCoverageReportUseCaseTest {

  private lateinit var repository: FakeCoverageDashboardRepository
  private lateinit var useCase: GetCoverageReportUseCase

  @BeforeEach
  fun setUp() {
    repository = FakeCoverageDashboardRepository()
    useCase = GetCoverageReportUseCase(repository)
  }

  @Test
  fun `invoke returns success when repository supplies payload`() = runTest {
    val payload =
      CoverageDashboardPayload(
        buildId = "build-123",
        generatedAt = "2025-11-05T12:00:00Z",
        layers =
          listOf(
            LayerPayload(layer = "VIEW_MODEL", coverage = 82.5, threshold = 75.0),
            LayerPayload(layer = "UI", coverage = 68.0, threshold = 65.0),
          ),
        trend = mapOf("VIEW_MODEL" to 1.2, "UI" to -0.4),
        risks = listOf(RiskPayload("risk-1", "Offline writes failing", "critical", "open")),
      )
    repository.succeedWith(payload)

    val result = useCase()

    assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
    val success = result as NanoAIResult.Success
    assertThat(success.value.buildId).isEqualTo("build-123")
    assertThat(success.value.layerMetrics.keys).containsAtLeast(TestLayer.VIEW_MODEL, TestLayer.UI)
  }

  @Test
  fun `invoke propagates repository failure`() = runTest {
    repository.failWith(NanoAIResult.recoverable(message = "HTTP 503"))

    val result = useCase()

    assertThat(result).isInstanceOf(NanoAIResult.RecoverableError::class.java)
    val error = result as NanoAIResult.RecoverableError
    assertThat(error.message).isEqualTo("HTTP 503")
  }

  @Test
  fun `invoke emits recoverable when payload invalid`() = runTest {
    val payload =
      CoverageDashboardPayload(
        buildId = "build-invalid",
        generatedAt = "2025-11-05T12:00:00Z",
        layers = emptyList(),
        trend = emptyMap(),
        risks = emptyList(),
      )
    repository.succeedWith(payload)

    val result = useCase()

    assertThat(result).isInstanceOf(NanoAIResult.RecoverableError::class.java)
    val error = result as NanoAIResult.RecoverableError
    assertThat(error.message).contains("Coverage dashboard payload")
  }
}
