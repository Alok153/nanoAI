package com.vjaykrsna.nanoai.feature.uiux.ui.commandpalette

import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandAction

private const val PRIORITY_PREFIX_MATCH = 0
private const val PRIORITY_TITLE_CONTAINS = 1
private const val PRIORITY_SUBTITLE_CONTAINS = 2

internal fun List<CommandAction>.firstEnabledIndex(): Int =
  indexOfFirst(CommandAction::enabled).takeIf { it >= 0 } ?: -1

internal fun List<CommandAction>.lastEnabledIndex(): Int =
  indexOfLast(CommandAction::enabled).takeIf { it >= 0 } ?: -1

internal fun List<CommandAction>.nextEnabledIndex(current: Int): Int? {
  if (isEmpty()) return null
  val startIndex = (current + 1).coerceAtLeast(0)
  return (startIndex until size).firstOrNull { index -> this[index].enabled }
}

internal fun List<CommandAction>.previousEnabledIndex(current: Int): Int? {
  if (isEmpty()) return null
  return (current - 1 downTo 0).firstOrNull { index -> this[index].enabled }
}

internal fun List<CommandAction>.resolveNextIndex(current: Int): Int? {
  return if (isEmpty()) {
    null
  } else {
    when (current) {
      -1 -> firstEnabledIndex().takeIf { it != -1 }
      else -> nextEnabledIndex(current) ?: firstEnabledIndex().takeIf { it != -1 }
    }
  }
}

internal fun List<CommandAction>.resolvePreviousIndex(current: Int): Int? {
  return if (isEmpty()) {
    null
  } else {
    when (current) {
      -1 -> lastEnabledIndex().takeIf { it != -1 }
      else -> previousEnabledIndex(current) ?: lastEnabledIndex().takeIf { it != -1 }
    }
  }
}

internal fun filterCommands(actions: List<CommandAction>, query: String): List<CommandAction> {
  if (query.isBlank()) return actions
  val needle = query.lowercase()
  return actions
    .mapNotNull { action ->
      val title = action.title.lowercase()
      val subtitle = action.subtitle?.lowercase()
      val priority =
        when {
          title.startsWith(needle) -> PRIORITY_PREFIX_MATCH
          title.contains(needle) -> PRIORITY_TITLE_CONTAINS
          subtitle?.contains(needle) == true -> PRIORITY_SUBTITLE_CONTAINS
          else -> null
        }
      priority?.let { it to action }
    }
    .sortedWith(compareBy({ it.first }, { it.second.title.length }))
    .map { it.second }
}
