package com.vjaykrsna.nanoai.feature.uiux.presentation

/** Representation of an executable action surfaced in the command palette. */
data class CommandAction(
  val id: String,
  val title: String,
  val subtitle: String? = null,
  val shortcut: String? = null,
  val enabled: Boolean = true,
  val category: CommandCategory = CommandCategory.MODES,
  val destination: CommandDestination = CommandDestination.None,
)
