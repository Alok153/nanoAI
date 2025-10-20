package com.vjaykrsna.nanoai.coverage.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.coverage.domain.usecase.GetCoverageReportUseCase
import com.vjaykrsna.nanoai.coverage.domain.usecase.GetCoverageReportUseCase.Result
import com.vjaykrsna.nanoai.coverage.model.CoverageMetric
import com.vjaykrsna.nanoai.coverage.model.TestLayer
import com.vjaykrsna.nanoai.coverage.ui.CoverageDashboardBanner
import com.vjaykrsna.nanoai.coverage.ui.CoverageDashboardUiState
import com.vjaykrsna.nanoai.coverage.ui.LayerCoverageState
import com.vjaykrsna.nanoai.coverage.ui.RiskChipState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CoverageDashboardViewModel
@Inject
constructor(private val getCoverageReportUseCase: GetCoverageReportUseCase) : ViewModel() {

  private val _uiState = MutableStateFlow(initialState())
  val uiState: StateFlow<CoverageDashboardUiState> = _uiState.asStateFlow()

  init {
    refresh()
  }

  fun refresh() {
    viewModelScope.launch {
      _uiState.update { current -> current.copy(isRefreshing = true, errorBanner = null) }
      runCatching { getCoverageReportUseCase() }
        .onSuccess { result -> _uiState.value = result.toUiState(isRefreshing = false) }
        .onFailure { error ->
          _uiState.update { current ->
            current.copy(isRefreshing = false, errorBanner = CoverageDashboardBanner.offline(error))
          }
        }
    }
  }

  private fun Result.toUiState(isRefreshing: Boolean): CoverageDashboardUiState {
    val layers =
      TestLayer.entries.mapNotNull { layer ->
        val metric = layerMetrics[layer] ?: return@mapNotNull null
        LayerCoverageState(layer = layer, metric = metric)
      }
    val risks =
      risks.map { risk ->
        RiskChipState(
          riskId = risk.riskId,
          title = risk.title,
          severity = risk.severity,
          status = risk.status,
        )
      }
    return CoverageDashboardUiState(
      buildId = buildId,
      generatedAtIso = generatedAtIso,
      isRefreshing = isRefreshing,
      layers = layers,
      risks = risks,
      trendDelta = trendDelta,
      errorBanner = null,
    )
  }

  private fun initialState(): CoverageDashboardUiState =
    CoverageDashboardUiState(
      buildId = "--",
      generatedAtIso = "",
      isRefreshing = false,
      layers =
        TestLayer.entries.map { layer ->
          LayerCoverageState(
            layer = layer,
            metric = CoverageMetric(coverage = 0.0, threshold = 0.0),
          )
        },
      risks = emptyList(),
      trendDelta = emptyMap(),
      errorBanner = null,
    )
}
