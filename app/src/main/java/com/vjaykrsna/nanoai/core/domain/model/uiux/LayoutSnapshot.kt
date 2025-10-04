package com.vjaykrsna.nanoai.core.domain.model.uiux

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Domain representation of a saved layout configuration for the UI/UX feature. */
data class LayoutSnapshot(
  val id: String,
  val name: String,
  val lastOpenedScreen: String,
  var pinnedTools: List<String>,
  val isCompact: Boolean,
) {
  init {
    require(id.isNotBlank()) { "LayoutSnapshot id cannot be blank." }
    require(name.length <= MAX_NAME_LENGTH) {
      "LayoutSnapshot name must be ≤ $MAX_NAME_LENGTH characters."
    }

    pinnedTools = sanitizePinnedTools(pinnedTools, isCompact)
  }

  /** Returns a copy with sanitized pinned tools for the current compact mode. */
  fun withPinnedTools(tools: List<String>): LayoutSnapshot =
    copy(pinnedTools = sanitizePinnedTools(tools, isCompact))

  /** Returns a copy with compact mode updated while enforcing tool count limits. */
  fun withCompactMode(compact: Boolean): LayoutSnapshot =
    copy(isCompact = compact, pinnedTools = sanitizePinnedTools(pinnedTools, compact))

  companion object {
    const val MAX_NAME_LENGTH = 64
    const val MAX_PINNED_TOOLS = 10
  }
}

data class LayoutSnapshotRecord(
  val id: String,
  val name: String,
  val lastOpenedScreen: String,
  val pinnedTools: List<String>,
  val isCompact: Boolean,
)

fun LayoutSnapshotRecord.toDomain(): LayoutSnapshot =
  LayoutSnapshot(
    id = id,
    name = name,
    lastOpenedScreen = lastOpenedScreen,
    pinnedTools = pinnedTools,
    isCompact = isCompact,
  )

fun LayoutSnapshot.toRecord(): LayoutSnapshotRecord =
  LayoutSnapshotRecord(
    id = id,
    name = name,
    lastOpenedScreen = lastOpenedScreen,
    pinnedTools = pinnedTools,
    isCompact = isCompact,
  )

fun Flow<List<LayoutSnapshotRecord>>.mapToLayoutSnapshots(): Flow<List<LayoutSnapshot>> {
  return map { records -> records.map(LayoutSnapshotRecord::toDomain) }
}

fun Flow<LayoutSnapshotRecord?>.mapToLayoutSnapshot(): Flow<LayoutSnapshot?> = map { record ->
  record?.toDomain()
}

private fun sanitizePinnedTools(tools: List<String>, isCompact: Boolean): List<String> {
  val sanitized = tools.filter { it.isNotBlank() }
  require(sanitized.size == tools.size) { "Pinned tool identifiers must be non-blank." }
  val unique = sanitized.distinct()
  require(unique.size == sanitized.size) { "Pinned tool identifiers must be unique." }
  require(unique.size <= LayoutSnapshot.MAX_PINNED_TOOLS) {
    "Pinned tools cannot exceed ${LayoutSnapshot.MAX_PINNED_TOOLS}."
  }
  if (isCompact) {
    require(unique.size <= VisualDensity.COMPACT_PINNED_TOOL_CAP) {
      "Compact layouts support up to ${VisualDensity.COMPACT_PINNED_TOOL_CAP} pinned tools."
    }
  }
  return unique
}
