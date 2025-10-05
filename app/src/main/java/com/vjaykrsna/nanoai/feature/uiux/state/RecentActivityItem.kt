package com.vjaykrsna.nanoai.feature.uiux.state

import java.time.Instant

/** Entry displayed in the home hub recent activity list. */
data class RecentActivityItem(
  val id: String,
  val modeId: ModeId,
  val title: String,
  val timestamp: Instant,
  val status: RecentStatus,
)
