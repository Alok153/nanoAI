package com.vjaykrsna.nanoai.feature.chat.presentation

import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.onFailure
import com.vjaykrsna.nanoai.core.common.onSuccess
import com.vjaykrsna.nanoai.core.domain.chat.ConversationUseCase
import com.vjaykrsna.nanoai.core.domain.chat.SendPromptUseCase
import com.vjaykrsna.nanoai.core.domain.chat.SwitchPersonaUseCase
import com.vjaykrsna.nanoai.core.domain.library.Model
import com.vjaykrsna.nanoai.core.domain.library.ModelCatalogUseCase
import com.vjaykrsna.nanoai.core.domain.library.toModel
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.domain.model.Message
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.usecase.GetDefaultPersonaUseCase
import com.vjaykrsna.nanoai.core.domain.usecase.ObservePersonasUseCase
import com.vjaykrsna.nanoai.core.model.MessageRole
import com.vjaykrsna.nanoai.core.model.MessageSource
import com.vjaykrsna.nanoai.core.model.PersonaSwitchAction
import com.vjaykrsna.nanoai.feature.chat.presentation.state.ChatAudioAttachment
import com.vjaykrsna.nanoai.feature.chat.presentation.state.ChatComposerAttachments
import com.vjaykrsna.nanoai.feature.chat.presentation.state.ChatImageAttachment
import com.vjaykrsna.nanoai.feature.chat.presentation.state.ChatUiState
import com.vjaykrsna.nanoai.shared.state.NanoAIViewEvent
import com.vjaykrsna.nanoai.shared.state.ViewModelStateHost
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@HiltViewModel
class ChatViewModel
@Inject
constructor(
  private val sendPromptUseCase: SendPromptUseCase,
  private val switchPersonaUseCase: SwitchPersonaUseCase,
  private val conversationUseCase: ConversationUseCase,
  private val observePersonasUseCase: ObservePersonasUseCase,
  private val getDefaultPersonaUseCase: GetDefaultPersonaUseCase,
  private val modelCatalogUseCase: ModelCatalogUseCase,
  @MainImmediateDispatcher mainDispatcher: CoroutineDispatcher,
) :
  ViewModelStateHost<ChatUiState, ChatUiEvent>(
    initialState = ChatUiState(),
    dispatcher = mainDispatcher,
  ) {
  private val currentThreadId = MutableStateFlow<UUID?>(null)

  init {
    observeThreads()
    observeMessages()
    observePersonas()
    observeInstalledModels()
  }

  fun onComposerTextChanged(newText: String) {
    updateState { copy(composerText = newText, pendingErrorMessage = null) }
  }

  fun onSendMessage() {
    val latestState = state.value
    val trimmed = latestState.composerText.trim()
    if (trimmed.isEmpty()) {
      return
    }

    val threadId = latestState.activeThreadId
    val personaId = latestState.activeThread?.personaId

    when {
      threadId == null -> {
        viewModelScope.launch(dispatcher) {
          emitError(ChatError.ThreadCreationFailed("No active thread available"))
        }
      }
      personaId == null -> {
        viewModelScope.launch(dispatcher) {
          emitError(ChatError.PersonaSelectionFailed("Choose a persona before sending messages"))
        }
      }
      else -> {
        val attachmentsSnapshot = latestState.attachments

        viewModelScope.launch(dispatcher) {
          updateState {
            copy(isSendingMessage = true, pendingErrorMessage = null, composerText = "")
          }

          val userMessage =
            Message(
              messageId = UUID.randomUUID(),
              threadId = threadId,
              role = MessageRole.USER,
              text = trimmed,
              source = MessageSource.LOCAL_MODEL,
              latencyMs = null,
              createdAt = Clock.System.now(),
            )

          val messageData =
            MessageData(
              threadId = threadId,
              text = trimmed,
              personaId = personaId,
              image = attachmentsSnapshot.image?.bitmap,
              audio = attachmentsSnapshot.audio?.data,
            )

          val saveResult = conversationUseCase.saveMessage(userMessage)
          handleSaveMessageResult(saveResult, messageData)
          updateState { copy(isSendingMessage = false) }
        }
      }
    }
  }

  fun showModelPicker() {
    updateState { copy(isModelPickerVisible = true) }
  }

  fun dismissModelPicker() {
    updateState { copy(isModelPickerVisible = false) }
  }

  fun selectModel(model: Model) {
    val thread = state.value.activeThread ?: return
    viewModelScope.launch(dispatcher) {
      conversationUseCase
        .updateThread(thread.copy(activeModelId = model.modelId))
        .onFailure { error ->
          emitError(ChatError.UnexpectedError(error.message ?: "Failed to select model"))
        }
        .onSuccess {
          updateState { copy(isModelPickerVisible = false) }
          emitEvent(ChatUiEvent.ModelSelected(model.displayName))
        }
    }
  }

  fun selectThread(threadId: UUID) {
    setActiveThread(threadId)
  }

  fun onImageSelected(bitmap: Bitmap) {
    updateState { copy(attachments = attachments.copy(image = ChatImageAttachment(bitmap))) }
  }

  fun onAudioRecorded(audioData: ByteArray, mimeType: String? = null) {
    updateState {
      copy(attachments = attachments.copy(audio = ChatAudioAttachment(audioData, mimeType)))
    }
  }

  fun clearPendingError() {
    updateState { copy(pendingErrorMessage = null) }
  }

  private fun clearAttachments() {
    updateState { copy(attachments = ChatComposerAttachments()) }
  }

  fun switchPersona(newPersonaId: UUID, action: PersonaSwitchAction) {
    val threadId = state.value.activeThreadId ?: return
    viewModelScope.launch(dispatcher) {
      switchPersonaUseCase(threadId, newPersonaId, action)
        .onSuccess { newThreadId ->
          if (action == PersonaSwitchAction.START_NEW_THREAD) {
            setActiveThread(newThreadId)
          }
        }
        .onFailure { error ->
          emitError(ChatError.PersonaSwitchFailed(error.message ?: "Failed to switch persona"))
        }
    }
  }

  fun createNewThread(personaId: UUID?, title: String? = null) {
    viewModelScope.launch(dispatcher) {
      val defaultPersonaId = personaId ?: getDefaultPersonaUseCase()?.personaId ?: UUID.randomUUID()
      conversationUseCase
        .createNewThread(defaultPersonaId, title)
        .onSuccess { threadId -> setActiveThread(threadId) }
        .onFailure { error ->
          emitError(ChatError.ThreadCreationFailed(error.message ?: "Failed to create thread"))
        }
    }
  }

  fun archiveThread(threadId: UUID) {
    viewModelScope.launch(dispatcher) {
      conversationUseCase
        .archiveThread(threadId)
        .onSuccess {
          if (state.value.activeThreadId == threadId) {
            setActiveThread(null)
          }
        }
        .onFailure { error ->
          emitError(ChatError.ThreadArchiveFailed(error.message ?: "Failed to archive thread"))
        }
    }
  }

  fun deleteThread(threadId: UUID) {
    viewModelScope.launch(dispatcher) {
      conversationUseCase
        .deleteThread(threadId)
        .onSuccess {
          if (state.value.activeThreadId == threadId) {
            setActiveThread(null)
          }
        }
        .onFailure { error ->
          emitError(ChatError.ThreadDeletionFailed(error.message ?: "Failed to delete thread"))
        }
    }
  }

  private fun observeThreads() {
    viewModelScope.launch(dispatcher) {
      conversationUseCase.getAllThreadsFlow().collect { threads ->
        val (resolvedId, resolvedThread) = resolveActiveThread(threads, currentThreadId.value)
        currentThreadId.value = resolvedId
        updateState {
          copy(
            threads = threads.toPersistentList(),
            activeThreadId = resolvedId,
            activeThread = resolvedThread,
          )
        }
      }
    }
  }

  private fun observeMessages() {
    viewModelScope.launch(dispatcher) {
      currentThreadId
        .flatMapLatest { threadId ->
          threadId?.let { conversationUseCase.getMessagesFlow(it) } ?: flowOf(emptyList())
        }
        .collect { messages -> updateState { copy(messages = messages.toPersistentList()) } }
    }
  }

  private fun observePersonas() {
    viewModelScope.launch(dispatcher) {
      observePersonasUseCase().collect { personas ->
        updateState { copy(personas = personas.toPersistentList()) }
      }
    }
  }

  private fun observeInstalledModels() {
    viewModelScope.launch(dispatcher) {
      modelCatalogUseCase
        .observeInstalledModels()
        .map { list: List<ModelPackage> -> list.map { it.toModel() } }
        .collect { models -> updateState { copy(installedModels = models.toPersistentList()) } }
    }
  }

  private fun setActiveThread(threadId: UUID?) {
    currentThreadId.value = threadId
    updateState {
      val activeThread = threadId?.let { id -> threads.firstOrNull { it.threadId == id } }
      copy(activeThreadId = threadId, activeThread = activeThread)
    }
  }

  private suspend fun emitError(error: ChatError) {
    updateState { copy(pendingErrorMessage = error.message) }
    emitEvent(ChatUiEvent.ErrorRaised(error))
  }

  private fun resolveActiveThread(
    threads: List<ChatThread>,
    requestedId: UUID?,
  ): Pair<UUID?, ChatThread?> {
    if (requestedId == null) return null to null
    val activeThread = threads.firstOrNull { it.threadId == requestedId }
    return if (activeThread != null) requestedId to activeThread else null to null
  }

  private suspend fun handleSaveMessageResult(
    saveResult: NanoAIResult<Unit>,
    messageData: MessageData,
  ) {
    when (saveResult) {
      is NanoAIResult.Success -> handleInference(messageData)
      is NanoAIResult.RecoverableError -> {
        emitError(ChatError.UnexpectedError("Failed to save message: ${saveResult.message}"))
        updateState { copy(isSendingMessage = false) }
      }
      is NanoAIResult.FatalError -> {
        emitError(ChatError.UnexpectedError("Failed to save message: ${saveResult.message}"))
        updateState { copy(isSendingMessage = false) }
      }
    }
  }

  private suspend fun handleInference(messageData: MessageData) {
    val inferenceResult =
      sendPromptUseCase(
        messageData.threadId,
        messageData.text,
        messageData.personaId,
        messageData.image,
        messageData.audio,
      )
    when (inferenceResult) {
      is NanoAIResult.Success -> clearAttachments()
      is NanoAIResult.RecoverableError ->
        emitError(ChatError.InferenceFailed(inferenceResult.message ?: "Failed to start inference"))
      is NanoAIResult.FatalError ->
        emitError(ChatError.InferenceFailed(inferenceResult.message ?: "Failed to start inference"))
    }
  }

  private data class MessageData(
    val threadId: UUID,
    val text: String,
    val personaId: UUID,
    val image: Bitmap?,
    val audio: ByteArray?,
  )
}

sealed interface ChatUiEvent : NanoAIViewEvent {
  data class ErrorRaised(val error: ChatError) : ChatUiEvent

  data class ModelSelected(val modelName: String) : ChatUiEvent
}

sealed class ChatError(open val message: String) {
  data class InferenceFailed(override val message: String) : ChatError(message)

  data class PersonaSwitchFailed(override val message: String) : ChatError(message)

  data class PersonaSelectionFailed(override val message: String) : ChatError(message)

  data class ThreadCreationFailed(override val message: String) : ChatError(message)

  data class ThreadArchiveFailed(override val message: String) : ChatError(message)

  data class ThreadDeletionFailed(override val message: String) : ChatError(message)

  data class UnexpectedError(override val message: String) : ChatError(message)
}
