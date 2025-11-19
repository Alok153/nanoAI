package com.vjaykrsna.nanoai.core.coverage.presentation

import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.error.NanoAIErrorEnvelope
import com.vjaykrsna.nanoai.core.common.error.toErrorEnvelope
import com.vjaykrsna.nanoai.core.coverage.domain.usecase.GetCoverageReportUseCase
import com.vjaykrsna.nanoai.core.coverage.domain.usecase.GetCoverageReportUseCase.Result
import com.vjaykrsna.nanoai.core.coverage.model.CoverageMetric
import com.vjaykrsna.nanoai.core.coverage.model.TestLayer
import com.vjaykrsna.nanoai.core.coverage.ui.CoverageDashboardBanner
import com.vjaykrsna.nanoai.core.coverage.ui.CoverageDashboardUiState
import com.vjaykrsna.nanoai.core.coverage.ui.LayerCoverageState
import com.vjaykrsna.nanoai.core.coverage.ui.RiskChipState
import com.vjaykrsna.nanoai.shared.state.NanoAIViewEvent
import com.vjaykrsna.nanoai.shared.state.ViewModelStateHost
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CoverageDashboardViewModel
@Inject
constructor(
  private val getCoverageReportUseCase: GetCoverageReportUseCase,
  @MainImmediateDispatcher mainDispatcher: CoroutineDispatcher,
) :
  ViewModelStateHost<CoverageDashboardUiState, CoverageDashboardUiEvent>(
    initialState = initialState(),
    dispatcher = mainDispatcher,
  ) {

  companion object {
    private const val REFRESH_ERROR = "Unable to refresh coverage dashboard"

    fun initialState(): CoverageDashboardUiState =
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
        lastErrorMessage = null,
      )
  }

  val uiState: StateFlow<CoverageDashboardUiState> = state

  init {
    refresh()
  }

  fun refresh() {
    viewModelScope.launch(dispatcher) {
      updateState { copy(isRefreshing = true, errorBanner = null, lastErrorMessage = null) }
      when (val result = getCoverageReportUseCase()) {
        is NanoAIResult.Success -> setState(result.value.toUiState(isRefreshing = false))
        else -> handleFailure(result)
      }
    }
  }

  private suspend fun handleFailure(result: NanoAIResult<Result>) {
    val envelope = result.toErrorEnvelope(REFRESH_ERROR).preferUserMessage(REFRESH_ERROR)
    updateState {
      copy(
        isRefreshing = false,
        errorBanner = CoverageDashboardBanner.offline(envelope.cause),
        lastErrorMessage = envelope.userMessage,
      )
    }
    emitEvent(CoverageDashboardUiEvent.ErrorRaised(envelope))
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
      lastErrorMessage = null,
    )
  }
}

sealed interface CoverageDashboardUiEvent : NanoAIViewEvent {
  data class ErrorRaised(val envelope: NanoAIErrorEnvelope) : CoverageDashboardUiEvent
}

private fun NanoAIErrorEnvelope.preferUserMessage(fallback: String): NanoAIErrorEnvelope {
  return if (userMessage.contains(fallback)) this else copy(userMessage = "$fallback: $userMessage")
}
