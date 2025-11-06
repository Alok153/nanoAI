package com.vjaykrsna.nanoai.core.domain.model.uiux

import java.time.Instant

/** Entry displayed in the home hub recent activity list. */
data class RecentActivityItem(
  val id: String,
  val modeId: ModeId,
  val title: String,
  val timestamp: Instant,
  val status: RecentStatus,
) {
  /** Human-friendly label for the current [status]. */
  val statusLabel: String
    get() =
      when (status) {
        RecentStatus.COMPLETED -> "Completed"
        RecentStatus.IN_PROGRESS -> "In progress"
        RecentStatus.FAILED -> "Failed"
      }

  /** True when the item is still being processed. */
  val isActive: Boolean
    get() = status == RecentStatus.IN_PROGRESS

  /** Returns the age in seconds relative to [reference]. */
  fun ageSeconds(reference: Instant = Instant.now()): Long =
    kotlin.math.max(0L, reference.epochSecond - timestamp.epochSecond)
}
