package com.vjaykrsna.nanoai.core.domain.model.uiux

import java.time.Duration
import java.time.Instant
import java.util.UUID

/** Async job metadata shown in the progress center. */
data class ProgressJob(
  val jobId: UUID,
  val type: JobType,
  val status: JobStatus,
  val progress: Float,
  val eta: Duration? = null,
  val canRetry: Boolean = false,
  val queuedAt: Instant = Instant.EPOCH,
  val subtitle: String? = null,
  val accessibilityLabelOverride: String? = null,
) {
  /** Normalised progress value, clamped between 0f and 1f. */
  val normalizedProgress: Float
    get() = progress.coerceIn(0f, 1f)

  /** True when the job is currently active (running or streaming). */
  val isActive: Boolean
    get() = status == JobStatus.RUNNING || status == JobStatus.STREAMING

  /** True when the job is waiting to start. */
  val isPending: Boolean
    get() = status == JobStatus.PENDING

  /** True when the job reached a terminal state (completed or failed). */
  val isTerminal: Boolean
    get() = status == JobStatus.COMPLETED || status == JobStatus.FAILED

  /** Whether retry CTA should be enabled for the job. */
  val canRetryNow: Boolean
    get() = canRetry && status == JobStatus.FAILED

  /** Returns a human-readable summary describing the job state. */
  val statusLabel: String
    get() =
      when (status) {
        JobStatus.PENDING -> "Waiting"
        JobStatus.RUNNING -> "In progress"
        JobStatus.PAUSED -> "Paused"
        JobStatus.FAILED -> "Failed"
        JobStatus.COMPLETED -> "Completed"
        JobStatus.STREAMING -> "Streaming"
      }

  /** Localised subtitle describing progress details (eta, queued time, etc.). */
  val subtitleOrDefault: String?
    get() = subtitle

  /** Content description to use for screen reader announcements. */
  val accessibilityLabel: String
    get() = accessibilityLabelOverride ?: "${type.label}, $statusLabel"
}
