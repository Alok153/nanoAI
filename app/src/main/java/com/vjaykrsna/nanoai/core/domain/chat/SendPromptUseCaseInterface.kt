package com.vjaykrsna.nanoai.core.domain.chat

import android.graphics.Bitmap
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import java.util.UUID

/** Interface for sending prompts and generating AI responses. */
interface SendPromptUseCaseInterface {
  suspend operator fun invoke(
    threadId: UUID,
    prompt: String,
    personaId: UUID,
    image: Bitmap? = null,
    audio: ByteArray? = null,
  ): NanoAIResult<Unit>
}
