package com.vjaykrsna.nanoai.core.domain.model.uiux

/** View state for the global command palette overlay. */
data class CommandPaletteState(
  val query: String = "",
  val results: List<CommandAction> = emptyList(),
  val recentCommands: List<CommandAction> = emptyList(),
  val selectedIndex: Int = -1,
  val surfaceTarget: CommandCategory = CommandCategory.MODES,
) {
  /** True when the palette currently contains a search query. */
  val hasQuery: Boolean
    get() = query.isNotBlank()

  /** Currently highlighted command based on [selectedIndex]. */
  val selectedCommand: CommandAction?
    get() = results.getOrNull(selectedIndex)

  /** Returns a copy with selection moved by [delta], wrapping within the current results. */
  fun moveSelection(delta: Int): CommandPaletteState {
    if (results.isEmpty()) return copy(selectedIndex = -1)
    val size = results.size
    val current = if (selectedIndex in results.indices) selectedIndex else 0
    val newIndex = (current + delta).floorMod(size)
    return copy(selectedIndex = newIndex)
  }

  /** Clears the current selection while keeping the existing results. */
  fun clearSelection(): CommandPaletteState = copy(selectedIndex = -1)

  /** Resets the palette to default state with no query or results. */
  fun cleared(): CommandPaletteState = CommandPaletteState(surfaceTarget = surfaceTarget)

  /**
   * Updates the query and results simultaneously, making sure selection stays within bounds and
   * falls back to the first result when available.
   */
  fun withResults(query: String, results: List<CommandAction>): CommandPaletteState {
    val normalizedResults = results.distinctBy(CommandAction::id)
    val nextIndex =
      when {
        normalizedResults.isEmpty() -> -1
        selectedIndex in normalizedResults.indices -> selectedIndex
        else -> 0
      }
    return copy(query = query, results = normalizedResults, selectedIndex = nextIndex)
  }

  private fun Int.floorMod(modulus: Int): Int {
    val mod = this % modulus
    return if (mod < 0) mod + modulus else mod
  }

  companion object {
    val Empty: CommandPaletteState = CommandPaletteState()
  }
}
