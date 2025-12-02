package com.vjaykrsna.nanoai.core.domain.model.uiux

/** Destination triggered when executing a command palette entry. */
sealed interface CommandDestination {
  data class Navigate(val route: String) : CommandDestination

  data class OpenRightPanel(val panel: RightPanel) : CommandDestination

  data object None : CommandDestination
}
