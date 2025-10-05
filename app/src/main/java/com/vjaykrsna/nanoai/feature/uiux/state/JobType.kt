package com.vjaykrsna.nanoai.feature.uiux.state

/** Domain type describing the nature of an async job surfaced in the progress center. */
enum class JobType {
  IMAGE_GENERATION,
  AUDIO_RECORDING,
  MODEL_DOWNLOAD,
  TEXT_GENERATION,
  TRANSLATION,
  OTHER,
}
