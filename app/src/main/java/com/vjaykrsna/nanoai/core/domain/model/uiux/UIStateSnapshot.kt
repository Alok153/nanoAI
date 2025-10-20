package com.vjaykrsna.nanoai.core.domain.model.uiux

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Domain model describing a snapshot of the user's interactive UI state. */
data class UIStateSnapshot(
  val userId: String,
  var expandedPanels: List<String>,
  var recentActions: List<String>,
  val isSidebarCollapsed: Boolean,
  val isLeftDrawerOpen: Boolean = false,
  val isRightDrawerOpen: Boolean = false,
  val activeModeRoute: String = DEFAULT_MODE_ROUTE,
  val activeRightPanel: String? = null,
  val isCommandPaletteVisible: Boolean = false,
) {
  init {
    require(userId.isNotBlank()) { "UIStateSnapshot userId cannot be blank." }

    expandedPanels = sanitizePanels(expandedPanels)
    recentActions = sanitizeRecentActions(recentActions)
    require(activeModeRoute.isNotBlank()) { "Active mode route must not be blank." }
    require(activeRightPanel?.isNotBlank() ?: true) {
      "Active right panel identifier must be non-blank when provided."
    }
  }

  fun withExpandedPanels(panels: List<String>): UIStateSnapshot =
    copy(expandedPanels = sanitizePanels(panels))

  fun withRecentActions(actions: List<String>): UIStateSnapshot =
    copy(recentActions = sanitizeRecentActions(actions))

  fun recordAction(actionId: String): UIStateSnapshot = withRecentActions(recentActions + actionId)

  fun toggleSidebar(collapsed: Boolean): UIStateSnapshot = copy(isSidebarCollapsed = collapsed)

  fun toggleLeftDrawer(open: Boolean): UIStateSnapshot = copy(isLeftDrawerOpen = open)

  fun toggleRightDrawer(open: Boolean, panelId: String?): UIStateSnapshot =
    copy(isRightDrawerOpen = open, activeRightPanel = panelId?.takeIf { it.isNotBlank() })

  fun updateActiveMode(route: String): UIStateSnapshot =
    copy(activeModeRoute = sanitizeRoute(route))

  fun updatePaletteVisibility(visible: Boolean): UIStateSnapshot =
    copy(isCommandPaletteVisible = visible)

  companion object {
    const val MAX_RECENT_ACTIONS = 5
    const val DEFAULT_MODE_ROUTE = "home"
  }
}

data class UIStateSnapshotRecord(
  val userId: String,
  val expandedPanels: List<String>,
  val recentActions: List<String>,
  val isSidebarCollapsed: Boolean,
  val isLeftDrawerOpen: Boolean,
  val isRightDrawerOpen: Boolean,
  val activeModeRoute: String,
  val activeRightPanel: String?,
  val isCommandPaletteVisible: Boolean,
)

fun UIStateSnapshotRecord.toDomain(): UIStateSnapshot =
  UIStateSnapshot(
    userId = userId,
    expandedPanels = expandedPanels,
    recentActions = recentActions,
    isSidebarCollapsed = isSidebarCollapsed,
    isLeftDrawerOpen = isLeftDrawerOpen,
    isRightDrawerOpen = isRightDrawerOpen,
    activeModeRoute = activeModeRoute,
    activeRightPanel = activeRightPanel,
    isCommandPaletteVisible = isCommandPaletteVisible,
  )

fun UIStateSnapshot.toRecord(): UIStateSnapshotRecord =
  UIStateSnapshotRecord(
    userId = userId,
    expandedPanels = expandedPanels,
    recentActions = recentActions,
    isSidebarCollapsed = isSidebarCollapsed,
    isLeftDrawerOpen = isLeftDrawerOpen,
    isRightDrawerOpen = isRightDrawerOpen,
    activeModeRoute = activeModeRoute,
    activeRightPanel = activeRightPanel,
    isCommandPaletteVisible = isCommandPaletteVisible,
  )

fun Flow<UIStateSnapshotRecord?>.mapToUiStateSnapshot(): Flow<UIStateSnapshot?> = map { record ->
  record?.toDomain()
}

fun Flow<UIStateSnapshotRecord?>.requireUiStateSnapshot(
  fallback: () -> UIStateSnapshot
): Flow<UIStateSnapshot> {
  return map { record -> record?.toDomain() ?: fallback() }
}

private fun sanitizePanels(panels: List<String>): List<String> {
  val sanitized = panels.filter { it.isNotBlank() }
  require(sanitized.size == panels.size) { "Expanded panel identifiers must be non-blank." }
  return sanitized.distinct()
}

private fun sanitizeRecentActions(actions: List<String>): List<String> {
  val sanitized = actions.filter { it.isNotBlank() }
  require(sanitized.size == actions.size) { "Recent action identifiers must be non-blank." }
  return if (sanitized.size <= UIStateSnapshot.MAX_RECENT_ACTIONS) {
    sanitized
  } else {
    sanitized.takeLast(UIStateSnapshot.MAX_RECENT_ACTIONS)
  }
}

private fun sanitizeRoute(route: String): String {
  require(route.isNotBlank()) { "Mode route must not be blank." }
  return route.trim()
}
