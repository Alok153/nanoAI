package com.vjaykrsna.nanoai.feature.uiux.state

/** Destination triggered when executing a command palette entry. */
sealed interface CommandDestination {
  data class Navigate(val route: String) : CommandDestination

  data class OpenRightPanel(val panel: RightPanel) : CommandDestination

  data object None : CommandDestination
}
