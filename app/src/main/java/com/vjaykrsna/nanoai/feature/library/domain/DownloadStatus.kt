package com.vjaykrsna.nanoai.feature.library.domain

/** Status of a model download task. */
enum class DownloadStatus {
  /** Download is queued and waiting to start. */
  QUEUED,

  /** Download is actively in progress. */
  DOWNLOADING,

  /** Download is paused (can be resumed). */
  PAUSED,

  /** Download completed successfully. */
  COMPLETED,

  /** Download failed with an error. */
  FAILED,

  /** Download was cancelled by user. */
  CANCELLED,
}
