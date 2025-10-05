package com.vjaykrsna.nanoai.feature.uiux.state

import androidx.compose.material3.windowsizeclass.WindowSizeClass

/** Aggregated layout state consumed by the unified shell scaffold. */
data class ShellLayoutState(
  val windowSizeClass: WindowSizeClass,
  val isLeftDrawerOpen: Boolean,
  val isRightDrawerOpen: Boolean,
  val activeRightPanel: RightPanel?,
  val activeMode: ModeId,
  val showCommandPalette: Boolean,
  val connectivity: ConnectivityStatus,
  val pendingUndoAction: UndoPayload?,
  val progressJobs: List<ProgressJob>,
  val recentActivity: List<RecentActivityItem>,
) {
  companion object {
    fun empty(windowSizeClass: WindowSizeClass): ShellLayoutState {
      TODO("Phase 3.3 will provide default shell layout state")
    }
  }
}
