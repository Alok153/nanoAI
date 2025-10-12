package com.vjaykrsna.nanoai.core.domain.model.uiux

import com.vjaykrsna.nanoai.core.data.preferences.UiPreferencesStore

internal fun sanitizePinnedTools(tools: List<String>): List<String> {
  val sanitized = tools.filter { it.isNotBlank() }
  require(sanitized.size == tools.size) { "Pinned tool identifiers must be non-blank." }
  val unique = sanitized.distinct()
  require(unique.size == sanitized.size) { "Pinned tool identifiers must be unique." }
  require(unique.size <= UserProfile.MAX_PINNED_TOOLS) {
    "Pinned tools cannot exceed ${UserProfile.MAX_PINNED_TOOLS}."
  }
  return unique
}

internal fun sanitizeCommandPaletteRecents(commands: List<String>): List<String> {
  val sanitized = commands.filter { it.isNotBlank() }
  require(sanitized.size == commands.size) { "Command palette recents must be non-blank." }
  return sanitized.distinct().take(UiPreferencesStore.MAX_RECENT_COMMANDS)
}

internal fun sanitizeSavedLayouts(layouts: List<LayoutSnapshot>): List<LayoutSnapshot> =
  try {
    val distinct = layouts.distinctBy(LayoutSnapshot::id)
    require(distinct.size == layouts.size) { "Saved layouts must have unique identifiers." }
    distinct
  } catch (error: ClassCastException) {
    throw IllegalArgumentException("Saved layouts must contain LayoutSnapshot entries.", error)
  }
