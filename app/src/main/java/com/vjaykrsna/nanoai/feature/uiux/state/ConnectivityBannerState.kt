package com.vjaykrsna.nanoai.feature.uiux.state

import java.time.Instant

/** State container for the connectivity banner displayed at the top of the shell. */
data class ConnectivityBannerState(
  val status: ConnectivityStatus,
  val lastDismissedAt: Instant? = null,
  val queuedActionCount: Int = 0,
  val cta: CommandAction? = null,
)
