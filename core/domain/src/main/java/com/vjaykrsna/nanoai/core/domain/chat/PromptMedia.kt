package com.vjaykrsna.nanoai.core.domain.chat

/** Domain-level representation of supported prompt attachments. */
data class PromptImage(
  val bytes: ByteArray,
  val mimeType: String? = null,
  val width: Int? = null,
  val height: Int? = null,
)

/** Audio payload accompanying a prompt. */
data class PromptAudio(
  val bytes: ByteArray,
  val mimeType: String? = null,
  val sampleRateHz: Int? = null,
)

/** Convenience wrapper for optional attachments. */
data class PromptAttachments(val image: PromptImage? = null, val audio: PromptAudio? = null) {
  val hasImage: Boolean
    get() = image != null

  val hasAudio: Boolean
    get() = audio != null

  val hasAttachments: Boolean
    get() = hasImage || hasAudio
}
