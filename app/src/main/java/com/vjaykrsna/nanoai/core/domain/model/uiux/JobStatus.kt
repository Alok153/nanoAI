package com.vjaykrsna.nanoai.core.domain.model.uiux

/** Lifecycle phases for jobs displayed in the progress center. */
enum class JobStatus {
  PENDING,
  RUNNING,
  PAUSED,
  FAILED,
  COMPLETED,
  STREAMING,
}

/** Human-readable display name for the job status. */
val JobStatus.displayName: String
  get() =
    when (this) {
      JobStatus.PENDING -> "Waiting"
      JobStatus.RUNNING -> "Running"
      JobStatus.PAUSED -> "Paused"
      JobStatus.FAILED -> "Failed"
      JobStatus.COMPLETED -> "Completed"
      JobStatus.STREAMING -> "Streaming"
    }
