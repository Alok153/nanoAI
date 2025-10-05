package com.vjaykrsna.nanoai.feature.uiux.state

import androidx.compose.ui.graphics.vector.ImageVector

/** Representation of a primary mode displayed on the home hub grid. */
data class ModeCard(
  val id: ModeId,
  val title: String,
  val subtitle: String? = null,
  val icon: ImageVector,
  val primaryAction: CommandAction,
  val enabled: Boolean = true,
  val badge: BadgeInfo? = null,
)
