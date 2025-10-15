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

/** Human-readable display name for the job type. */
val JobType.label: String
  get() =
    when (this) {
      JobType.IMAGE_GENERATION -> "Image Generation"
      JobType.AUDIO_RECORDING -> "Audio Recording"
      JobType.MODEL_DOWNLOAD -> "Model Download"
      JobType.TEXT_GENERATION -> "Text Generation"
      JobType.TRANSLATION -> "Translation"
      JobType.OTHER -> "Background Task"
    }
