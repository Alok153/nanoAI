package com.vjaykrsna.nanoai.feature.chat.presentation

import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.core.common.error.NanoAIErrorEnvelope
import com.vjaykrsna.nanoai.core.common.error.toErrorEnvelope
import com.vjaykrsna.nanoai.core.common.onFailure
import com.vjaykrsna.nanoai.core.common.onSuccess
import com.vjaykrsna.nanoai.core.domain.chat.PromptAttachments
import com.vjaykrsna.nanoai.core.domain.chat.SendPromptUseCase
import com.vjaykrsna.nanoai.shared.state.NanoAIViewEvent
import com.vjaykrsna.nanoai.shared.state.NanoAIViewState
import com.vjaykrsna.nanoai.shared.state.ViewModelStateHost
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel for message composition in chat.
 *
 * Manages message text, attachments, and sending state.
 */
@HiltViewModel
class MessageComposerViewModel
@Inject
constructor(
  private val sendPromptUseCase: SendPromptUseCase,
  @MainImmediateDispatcher
  private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) :
  ViewModelStateHost<MessageComposerUiState, MessageComposerUiEvent>(
    initialState = MessageComposerUiState(),
    dispatcher = mainDispatcher,
  ) {

  private companion object {
    private const val DEFAULT_SEND_FAILURE_MESSAGE = "Failed to send message"
  }

  fun updateMessageText(text: String) {
    updateState { copy(messageText = text) }
  }

  fun sendMessage(threadId: UUID, personaId: UUID) {
    val text = state.value.messageText.trim()
    if (text.isEmpty()) {
      viewModelScope.launch(mainDispatcher) {
        val envelope = NanoAIErrorEnvelope(MessageComposerError.EmptyMessage.message)
        emitError(MessageComposerError.EmptyMessage, envelope)
      }
      return
    }

    updateState { copy(isSending = true, sendError = null) }
    viewModelScope.launch(mainDispatcher) {
      sendPromptUseCase(threadId, text, personaId, PromptAttachments())
        .onSuccess {
          updateState { copy(messageText = "", isSending = false, sendError = null) }
          emitEvent(MessageComposerUiEvent.MessageSent)
        }
        .onFailure { error ->
          val envelope = error.toErrorEnvelope(DEFAULT_SEND_FAILURE_MESSAGE)
          updateState { copy(isSending = false, sendError = envelope.userMessage) }
          emitError(MessageComposerError.SendFailed(envelope.userMessage), envelope)
        }
    }
  }

  fun clearMessage() {
    updateState { copy(messageText = "") }
  }

  fun clearError() {
    updateState { copy(sendError = null) }
  }

  private suspend fun emitError(error: MessageComposerError, envelope: NanoAIErrorEnvelope) {
    emitEvent(MessageComposerUiEvent.ErrorRaised(error, envelope))
  }
}

data class MessageComposerUiState(
  val messageText: String = "",
  val isSending: Boolean = false,
  val sendError: String? = null,
) : NanoAIViewState

sealed class MessageComposerError(open val message: String) {
  data object EmptyMessage : MessageComposerError("Message cannot be empty")

  data class SendFailed(override val message: String) : MessageComposerError(message)
}

sealed interface MessageComposerUiEvent : NanoAIViewEvent {
  data class ErrorRaised(val error: MessageComposerError, val envelope: NanoAIErrorEnvelope) :
    MessageComposerUiEvent

  data object MessageSent : MessageComposerUiEvent
}
