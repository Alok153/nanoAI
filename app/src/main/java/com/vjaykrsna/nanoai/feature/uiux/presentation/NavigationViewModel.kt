package com.vjaykrsna.nanoai.feature.uiux.presentation

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.feature.uiux.data.ShellStateRepository
import com.vjaykrsna.nanoai.feature.uiux.state.ModeId
import com.vjaykrsna.nanoai.feature.uiux.state.PaletteDismissReason
import com.vjaykrsna.nanoai.feature.uiux.state.PaletteSource
import com.vjaykrsna.nanoai.feature.uiux.state.RightPanel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel responsible for navigation state management within the shell. */
@HiltViewModel
class NavigationViewModel
@Inject
constructor(
  private val repository: ShellStateRepository,
  @MainImmediateDispatcher private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {

  /** Navigation-specific state derived from shell layout state. */
  val navigationState: StateFlow<NavigationState> =
    repository.shellLayoutState
      .map { layout ->
        NavigationState(
          activeMode = layout.activeMode,
          isLeftDrawerOpen = layout.isLeftDrawerOpen,
          isRightDrawerOpen = layout.isRightDrawerOpen,
          activeRightPanel = layout.activeRightPanel,
          showCommandPalette = layout.showCommandPalette,
          windowSizeClass = layout.windowSizeClass,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = NavigationState.default(),
      )

  /** Opens a specific mode, closing drawers and hiding the command palette. */
  fun openMode(modeId: ModeId) {
    viewModelScope.launch(dispatcher) { repository.openMode(modeId) }
  }

  /** Toggles the left navigation drawer. */
  fun toggleLeftDrawer() {
    viewModelScope.launch(dispatcher) { repository.toggleLeftDrawer() }
  }

  /** Sets the left drawer to a specific open/closed state. */
  fun setLeftDrawer(open: Boolean) {
    viewModelScope.launch(dispatcher) { repository.setLeftDrawer(open) }
  }

  /** Toggles the right contextual drawer for a specific panel. */
  fun toggleRightDrawer(panel: RightPanel) {
    viewModelScope.launch(dispatcher) { repository.toggleRightDrawer(panel) }
  }

  /** Shows the command palette overlay from a specific source. */
  fun showCommandPalette(source: PaletteSource) {
    viewModelScope.launch(dispatcher) { repository.showCommandPalette(source) }
  }

  /** Hides the command palette overlay. */
  fun hideCommandPalette(@Suppress("UNUSED_PARAMETER") reason: PaletteDismissReason) {
    viewModelScope.launch(dispatcher) { repository.hideCommandPalette() }
  }

  /** Updates the current window size class so adaptive layouts respond to device changes. */
  fun updateWindowSizeClass(sizeClass: WindowSizeClass) {
    viewModelScope.launch(dispatcher) { repository.updateWindowSizeClass(sizeClass) }
  }
}

/** Navigation-specific state extracted from ShellLayoutState. */
data class NavigationState(
  val activeMode: ModeId,
  val isLeftDrawerOpen: Boolean,
  val isRightDrawerOpen: Boolean,
  val activeRightPanel: RightPanel?,
  val showCommandPalette: Boolean,
  val windowSizeClass: WindowSizeClass,
) {
  companion object {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    fun default(): NavigationState =
      NavigationState(
        activeMode = ModeId.HOME,
        isLeftDrawerOpen = false,
        isRightDrawerOpen = false,
        activeRightPanel = null,
        showCommandPalette = false,
        windowSizeClass =
          WindowSizeClass.calculateFromSize(
            androidx.compose.ui.unit.DpSize(width = 640.dp, height = 360.dp)
          ),
      )
  }
}
