package com.vjaykrsna.nanoai.feature.uiux.state

/** View state for the global command palette overlay. */
data class CommandPaletteState(
  val query: String = "",
  val results: List<CommandAction> = emptyList(),
  val recentCommands: List<CommandAction> = emptyList(),
  val selectedIndex: Int = -1,
  val surfaceTarget: CommandCategory = CommandCategory.MODES,
)
