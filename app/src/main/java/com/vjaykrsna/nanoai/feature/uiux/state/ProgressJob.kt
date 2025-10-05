package com.vjaykrsna.nanoai.feature.uiux.state

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
)
