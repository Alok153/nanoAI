package com.vjaykrsna.nanoai.core.common

/** Enumeration of model capabilities. */
enum class Capability(val value: String) {
  TEXT_GEN("TEXT_GEN"),
  CODE_GEN("CODE_GEN"),
  IMAGE_GEN("IMAGE_GEN"),
  AUDIO_IN("AUDIO_IN"),
  AUDIO_OUT("AUDIO_OUT"),
}
