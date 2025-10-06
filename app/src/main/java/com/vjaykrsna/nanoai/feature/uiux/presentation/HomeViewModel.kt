package com.vjaykrsna.nanoai.feature.uiux.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.feature.uiux.domain.ObserveUserProfileUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.RecordOnboardingProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val HOME_TOOLTIP_ID = "home_tools_tip"

@HiltViewModel
class HomeViewModel
@Inject
constructor(
  private val observeUserProfile: ObserveUserProfileUseCase,
  private val recordOnboardingProgress: RecordOnboardingProgressUseCase,
  @MainImmediateDispatcher private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {
  private val _uiState = MutableStateFlow(HomeUiState())
  val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
  private val presenterScope = CoroutineScope(SupervisorJob() + dispatcher)

  init {
    observeUserProfile.flow
      .onEach { result ->
        val recent = result.uiState?.recentActions ?: emptyList()
        val tooltipVisible = !(result.userProfile?.dismissedTips?.get(HOME_TOOLTIP_ID) ?: false)
        val queued = recent.size
        _uiState.update { current ->
          current.copy(
            recentActions = recent,
            offlineBannerVisible = result.offline,
            tooltipEntryVisible = tooltipVisible,
            queuedActions = queued,
            isHydrating = result.userProfile == null,
          )
        }
      }
      .launchIn(presenterScope)
  }

  fun toggleToolsExpanded() {
    _uiState.update { it.copy(toolsExpanded = !it.toolsExpanded) }
  }

  fun onRecentAction(actionId: String) {
    _uiState.update { it.copy(lastInteractedAction = actionId, latencyIndicatorVisible = true) }
  }

  fun dismissTooltip() {
    _uiState.update { it.copy(tooltipEntryVisible = false) }
    viewModelScope.launch(dispatcher) {
      recordOnboardingProgress.recordDismissal(
        tipId = HOME_TOOLTIP_ID,
        dismissed = true,
        completed = false,
      )
    }
  }

  fun dontShowTooltipAgain() {
    _uiState.update { it.copy(tooltipEntryVisible = false) }
    viewModelScope.launch(dispatcher) {
      recordOnboardingProgress.recordDismissal(
        tipId = HOME_TOOLTIP_ID,
        dismissed = true,
        completed = false,
      )
    }
  }

  fun onTooltipHelp() {
    _uiState.update { it.copy(lastInteractedAction = "help_center") }
  }

  fun retryPendingActions() {
    // No remote sync - all data is local
    _uiState.update { it.copy(lastInteractedAction = "retry") }
  }

  override fun onCleared() {
    super.onCleared()
    presenterScope.cancel()
  }
}

data class HomeUiState(
  val recentActions: List<String> = emptyList(),
  val offlineBannerVisible: Boolean = false,
  val queuedActions: Int = 0,
  val toolsExpanded: Boolean = false,
  val tooltipEntryVisible: Boolean = false,
  val lastInteractedAction: String? = null,
  val latencyIndicatorVisible: Boolean = false,
  val isHydrating: Boolean = true,
)
