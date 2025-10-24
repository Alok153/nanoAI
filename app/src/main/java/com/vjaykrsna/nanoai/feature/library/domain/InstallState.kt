package com.vjaykrsna.nanoai.feature.library.domain

/** Installation state of a model package. */
enum class InstallState {
  /** Model is not installed on device. */
  NOT_INSTALLED,

  /** Model is currently being downloaded. */
  DOWNLOADING,

  /** Model is fully installed and ready to use. */
  INSTALLED,

  /** Download is paused (can be resumed). */
  PAUSED,

  /** Download or installation encountered an error. */
  ERROR,
}
