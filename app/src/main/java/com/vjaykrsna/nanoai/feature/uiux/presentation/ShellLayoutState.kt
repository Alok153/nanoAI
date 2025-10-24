package com.vjaykrsna.nanoai.feature.uiux.presentation

import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass

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
  /** True when the current width class maps to a compact experience (phones, portrait). */
  val isCompactWidth: Boolean
    get() = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

  /** True when the device has enough width for a medium/expanded layout. */
  val isMediumOrWider: Boolean
    get() = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium

  /** True when vertical height can comfortably host persistent rails. */
  val isTallLayout: Boolean
    get() = windowSizeClass.heightSizeClass >= WindowHeightSizeClass.Medium

  /** True when the left navigation should be rendered as a permanent drawer. */
  val usesPermanentLeftDrawer: Boolean
    get() = isMediumOrWider

  /** True when the right contextual drawer should occupy persistent space. */
  val supportsRightRail: Boolean
    get() = isMediumOrWider && isTallLayout

  /** True when the left drawer should be visible in the current configuration. */
  val isLeftDrawerVisible: Boolean
    get() = usesPermanentLeftDrawer || isLeftDrawerOpen

  /** True when the nav icon should toggle the left drawer (permanent drawers stay open). */
  val canToggleLeftDrawer: Boolean
    get() = !usesPermanentLeftDrawer

  /** Derived visibility for the right drawer based on support + current toggle state. */
  val isRightRailVisible: Boolean
    get() = supportsRightRail && isRightDrawerOpen

  /** Whether the shell should prefer modal navigation gestures for the left drawer. */
  val useModalNavigation: Boolean
    get() = !usesPermanentLeftDrawer

  /** Number of jobs currently tracked in the progress center. */
  val jobCount: Int
    get() = progressJobs.size

  /** True if any async work is pending or running. */
  val hasActiveJobs: Boolean
    get() = progressJobs.any { !it.isTerminal }

  /** True when the command palette overlay is visible. */
  val isPaletteVisible: Boolean
    get() = showCommandPalette

  /** True when a pending undo payload should surface inline actions. */
  val hasUndoAvailable: Boolean
    get() = pendingUndoAction != null

  /** True when the shell is operating offline or in a degraded connectivity mode. */
  val isOffline: Boolean
    get() = connectivity != ConnectivityStatus.ONLINE

  /** Accessibility status string describing the current connectivity state. */
  val connectivityStatusDescription: String
    get() =
      when (connectivity) {
        ConnectivityStatus.ONLINE -> "Online"
        ConnectivityStatus.LIMITED -> "Limited connectivity"
        ConnectivityStatus.OFFLINE -> "Offline"
      }

  /** Convenience accessor for the active right drawer panel when the drawer is visible. */
  val visibleRightPanel: RightPanel?
    get() = if (isRightDrawerOpen) activeRightPanel else null

  /** Returns a copy with the command palette forcibly hidden. */
  fun withoutPalette(): ShellLayoutState = copy(showCommandPalette = false)

  /** Returns a copy with both drawers closed for navigation transitions. */
  fun withDrawersClosed(): ShellLayoutState =
    copy(isLeftDrawerOpen = false, isRightDrawerOpen = false, activeRightPanel = null)

  companion object {
    fun empty(windowSizeClass: WindowSizeClass): ShellLayoutState =
      ShellLayoutState(
        windowSizeClass = windowSizeClass,
        isLeftDrawerOpen = false,
        isRightDrawerOpen = false,
        activeRightPanel = null,
        activeMode = ModeId.HOME,
        showCommandPalette = false,
        connectivity = ConnectivityStatus.ONLINE,
        pendingUndoAction = null,
        progressJobs = emptyList(),
        recentActivity = emptyList(),
      )
  }
}
