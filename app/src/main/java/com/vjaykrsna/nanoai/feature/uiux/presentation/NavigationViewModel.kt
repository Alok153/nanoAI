package com.vjaykrsna.nanoai.feature.uiux.presentation

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.core.data.repository.NavigationRepository
import com.vjaykrsna.nanoai.core.data.repository.UserProfileRepository
import com.vjaykrsna.nanoai.core.domain.model.uiux.UIStateSnapshot
import com.vjaykrsna.nanoai.feature.uiux.domain.NavigationOperationsUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.UIUX_DEFAULT_USER_ID
import com.vjaykrsna.nanoai.feature.uiux.state.ModeId
import com.vjaykrsna.nanoai.feature.uiux.state.PaletteDismissReason
import com.vjaykrsna.nanoai.feature.uiux.state.PaletteSource
import com.vjaykrsna.nanoai.feature.uiux.state.RightPanel
import com.vjaykrsna.nanoai.feature.uiux.state.toModeIdOrDefault
import com.vjaykrsna.nanoai.feature.uiux.state.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel responsible for navigation state management within the shell. */
@HiltViewModel
class NavigationViewModel
@Inject
constructor(
  private val navigationRepository: NavigationRepository,
  private val userProfileRepository: UserProfileRepository,
  private val navigationOperationsUseCase: NavigationOperationsUseCase,
  @Suppress("UnusedPrivateProperty")
  @MainImmediateDispatcher
  private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {

  private val userId: String = UIUX_DEFAULT_USER_ID
  private val hasAppliedHomeStartup = AtomicBoolean(false)

  private val uiSnapshot: StateFlow<UIStateSnapshot> =
    userProfileRepository
      .observeUIStateSnapshot(userId)
      .map { snapshot -> snapshot ?: defaultSnapshot(userId) }
      .map { snapshot -> coerceInitialActiveMode(snapshot) }
      .stateIn(viewModelScope, SharingStarted.Eagerly, defaultSnapshot(userId))

  /** Navigation-specific state derived from shell layout state. */
  val navigationState: StateFlow<NavigationState> =
    combine(navigationRepository.windowSizeClass, uiSnapshot) { windowSize, snapshot ->
        NavigationState(
          activeMode = snapshot.activeModeRoute.toModeIdOrDefault(),
          isLeftDrawerOpen = snapshot.isLeftDrawerOpen,
          isRightDrawerOpen = snapshot.isRightDrawerOpen,
          activeRightPanel = snapshot.activeRightPanel.toRightPanel(),
          showCommandPalette = snapshot.isCommandPaletteVisible,
          windowSizeClass = windowSize,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = NavigationState.default(),
      )

  /** Opens a specific mode, closing drawers and hiding the command palette. */
  fun openMode(modeId: ModeId) {
    navigationOperationsUseCase.openMode(modeId)
  }

  /** Toggles the left navigation drawer. */
  fun toggleLeftDrawer() {
    navigationOperationsUseCase.toggleLeftDrawer()
  }

  /** Sets the left drawer to a specific open/closed state. */
  fun setLeftDrawer(open: Boolean) {
    navigationOperationsUseCase.setLeftDrawer(open)
  }

  /** Toggles the right contextual drawer for a specific panel. */
  fun toggleRightDrawer(panel: RightPanel) {
    navigationOperationsUseCase.toggleRightDrawer(panel)
  }

  /** Shows the command palette overlay from a specific source. */
  fun showCommandPalette(source: PaletteSource) {
    navigationOperationsUseCase.showCommandPalette(source)
  }

  /** Hides the command palette overlay. */
  fun hideCommandPalette(@Suppress("UNUSED_PARAMETER") reason: PaletteDismissReason) {
    navigationOperationsUseCase.hideCommandPalette()
  }

  /** Updates the current window size class so adaptive layouts respond to device changes. */
  fun updateWindowSizeClass(sizeClass: WindowSizeClass) {
    navigationOperationsUseCase.updateWindowSizeClass(sizeClass)
  }

  private fun coerceInitialActiveMode(snapshot: UIStateSnapshot): UIStateSnapshot {
    if (hasAppliedHomeStartup.compareAndSet(false, true)) {
      val resetSnapshot =
        snapshot
          .updateActiveMode(UIStateSnapshot.DEFAULT_MODE_ROUTE)
          .toggleLeftDrawer(open = false)
          .toggleRightDrawer(open = false, panelId = null)
          .updatePaletteVisibility(visible = false)

      viewModelScope.launch {
        if (
          !snapshot.activeModeRoute.equals(UIStateSnapshot.DEFAULT_MODE_ROUTE, ignoreCase = true)
        ) {
          userProfileRepository.updateActiveModeRoute(userId, ModeId.HOME.toRoute())
        }
        if (snapshot.isLeftDrawerOpen) {
          userProfileRepository.updateLeftDrawerOpen(userId, false)
        }
        if (snapshot.isRightDrawerOpen || snapshot.activeRightPanel != null) {
          userProfileRepository.updateRightDrawerState(userId, false, null)
        }
        if (snapshot.isCommandPaletteVisible) {
          userProfileRepository.updateCommandPaletteVisibility(userId, false)
        }
      }

      return resetSnapshot
    }

    return snapshot
  }

  private fun defaultSnapshot(userId: String): UIStateSnapshot =
    UIStateSnapshot(
      userId = userId,
      expandedPanels = emptyList(),
      recentActions = emptyList(),
      isSidebarCollapsed = false,
    )

  private fun String?.toRightPanel(): RightPanel? {
    val value = this ?: return null
    return RightPanel.entries.firstOrNull { panel -> panel.name.equals(value, ignoreCase = true) }
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
