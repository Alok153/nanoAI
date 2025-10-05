package com.vjaykrsna.nanoai.feature.uiux.state

/** Lifecycle phases for jobs displayed in the progress center. */
enum class JobStatus {
  PENDING,
  RUNNING,
  PAUSED,
  FAILED,
  COMPLETED,
  STREAMING,
}
