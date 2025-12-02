package com.vjaykrsna.nanoai.core.domain.settings.model

import kotlinx.datetime.Instant

/**
 * Snapshot describing the user's exposure to the privacy disclaimer dialog.
 *
 * @property shouldShowDialog True when the dialog must be presented on next launch.
 * @property acknowledged True once the user has acknowledged the disclaimer.
 * @property acknowledgedAt Timestamp of the last acknowledgement, or null if never acknowledged.
 * @property shownCount Total number of times the dialog has been displayed.
 */
data class DisclaimerExposureState(
  val shouldShowDialog: Boolean,
  val acknowledged: Boolean,
  val acknowledgedAt: Instant?,
  val shownCount: Int,
)
