package com.vjaykrsna.nanoai.core.domain.model.uiux

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Domain model describing a snapshot of the user's interactive UI state. */
data class UIStateSnapshot(
  val userId: String,
  var expandedPanels: List<String>,
  var recentActions: List<String>,
  val isSidebarCollapsed: Boolean,
) {
  init {
    require(userId.isNotBlank()) { "UIStateSnapshot userId cannot be blank." }

    expandedPanels = sanitizePanels(expandedPanels)
    recentActions = sanitizeRecentActions(recentActions)
  }

  fun withExpandedPanels(panels: List<String>): UIStateSnapshot =
    copy(expandedPanels = sanitizePanels(panels))

  fun withRecentActions(actions: List<String>): UIStateSnapshot =
    copy(recentActions = sanitizeRecentActions(actions))

  fun recordAction(actionId: String): UIStateSnapshot = withRecentActions(recentActions + actionId)

  fun toggleSidebar(collapsed: Boolean): UIStateSnapshot = copy(isSidebarCollapsed = collapsed)

  companion object {
    const val MAX_RECENT_ACTIONS = 5
  }
}

data class UIStateSnapshotRecord(
  val userId: String,
  val expandedPanels: List<String>,
  val recentActions: List<String>,
  val isSidebarCollapsed: Boolean,
)

fun UIStateSnapshotRecord.toDomain(): UIStateSnapshot =
  UIStateSnapshot(
    userId = userId,
    expandedPanels = expandedPanels,
    recentActions = recentActions,
    isSidebarCollapsed = isSidebarCollapsed,
  )

fun UIStateSnapshot.toRecord(): UIStateSnapshotRecord =
  UIStateSnapshotRecord(
    userId = userId,
    expandedPanels = expandedPanels,
    recentActions = recentActions,
    isSidebarCollapsed = isSidebarCollapsed,
  )

fun Flow<UIStateSnapshotRecord?>.mapToUiStateSnapshot(): Flow<UIStateSnapshot?> = map { record ->
  record?.toDomain()
}

fun Flow<UIStateSnapshotRecord?>.requireUiStateSnapshot(
  fallback: () -> UIStateSnapshot,
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
