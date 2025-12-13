package com.vjaykrsna.nanoai.feature.chat.presentation.state

import android.graphics.Bitmap
import com.vjaykrsna.nanoai.core.domain.library.Model
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.domain.model.Message
import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityBannerState
import com.vjaykrsna.nanoai.feature.chat.model.ChatPersonaSummary
import com.vjaykrsna.nanoai.feature.chat.model.LocalInferenceUiState
import com.vjaykrsna.nanoai.shared.state.NanoAIViewState
import java.util.UUID
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

/**
 * Immutable snapshot of the chat presentation layer.
 *
 * Aggregates every value that the UI needs so composables can render from a single source of truth.
 */
data class ChatUiState(
  val threads: PersistentList<ChatThread> = persistentListOf(),
  val activeThreadId: UUID? = null,
  val activeThread: ChatThread? = null,
  val messages: PersistentList<Message> = persistentListOf(),
  val personas: PersistentList<PersonaProfile> = persistentListOf(),
  val installedModels: PersistentList<Model> = persistentListOf(),
  val activePersonaSummary: ChatPersonaSummary? = null,
  val composerText: String = "",
  val isModelPickerVisible: Boolean = false,
  val isSendingMessage: Boolean = false,
  val attachments: ChatComposerAttachments = ChatComposerAttachments(),
  val pendingErrorMessage: String? = null,
  val connectivityBanner: ConnectivityBannerState? = null,
  val localInferenceUi: LocalInferenceUiState = LocalInferenceUiState(),
) : NanoAIViewState

/** Container for composer attachments so we can track image/audio selections atomically. */
data class ChatComposerAttachments(
  val image: ChatImageAttachment? = null,
  val audio: ChatAudioAttachment? = null,
)

/** Represents the currently selected preview image. */
data class ChatImageAttachment(val bitmap: Bitmap)

/** Represents the currently recorded audio clip. */
data class ChatAudioAttachment(val data: ByteArray, val mimeType: String? = null) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ChatAudioAttachment) return false
    if (!data.contentEquals(other.data)) return false
    return mimeType == other.mimeType
  }

  override fun hashCode(): Int {
    var result = data.contentHashCode()
    result = 31 * result + (mimeType?.hashCode() ?: 0)
    return result
  }
}
