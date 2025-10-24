package com.vjaykrsna.nanoai.feature.uiux.domain

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.data.repository.NavigationRepository
import com.vjaykrsna.nanoai.feature.uiux.presentation.ModeId
import com.vjaykrsna.nanoai.feature.uiux.presentation.PaletteSource
import com.vjaykrsna.nanoai.feature.uiux.presentation.RightPanel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Consolidated navigation operations for shell UI state management. */
class NavigationOperationsUseCase
@Inject
constructor(
  private val repository: NavigationRepository,
  @IoDispatcher private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  private val scope = CoroutineScope(SupervisorJob() + dispatcher)

  /** Opens a specific mode, closing drawers and hiding the command palette. */
  fun openMode(modeId: ModeId) {
    scope.launch { repository.openMode(modeId) }
  }

  /** Toggles the left navigation drawer. */
  fun toggleLeftDrawer() {
    scope.launch { repository.toggleLeftDrawer() }
  }

  /** Sets the left drawer to a specific open/closed state. */
  fun setLeftDrawer(open: Boolean) {
    scope.launch { repository.setLeftDrawer(open) }
  }

  /** Toggles the right contextual drawer for a specific panel. */
  fun toggleRightDrawer(panel: RightPanel) {
    scope.launch { repository.toggleRightDrawer(panel) }
  }

  /** Shows the command palette overlay from a specific source. */
  fun showCommandPalette(source: PaletteSource) {
    scope.launch { repository.showCommandPalette(source) }
  }

  /** Hides the command palette overlay. */
  fun hideCommandPalette() {
    scope.launch { repository.hideCommandPalette() }
  }

  /** Updates the current window size class for responsive layouts. */
  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  fun updateWindowSizeClass(sizeClass: WindowSizeClass) {
    scope.launch { repository.updateWindowSizeClass(sizeClass) }
  }
}
