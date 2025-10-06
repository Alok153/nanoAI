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
  val highlight: String? = null,
) {
  /** Accessible content description combining title, badge, and status. */
  val contentDescription: String
    get() {
      val parts = buildList {
        add(title)
        subtitle?.let(::add)
        if (!enabled) add("Unavailable")
        badge?.let { add(badgeDescription(it)) }
        highlight?.let(::add)
      }
      return parts.joinToString(separator = ", ")
    }

  /** Primary quick action label shown in quick actions row. */
  val primaryActionLabel: String
    get() = primaryAction.title

  /** Whether the card should render a badge chip. */
  val hasBadge: Boolean
    get() = badge != null

  private fun badgeDescription(info: BadgeInfo): String =
    when (info.type) {
      BadgeType.NEW -> if (info.count > 0) "New (${info.count})" else "New"
      BadgeType.PRO -> "Pro"
      BadgeType.SYNCING -> if (info.count > 0) "Syncing (${info.count})" else "Syncing"
    }
}
