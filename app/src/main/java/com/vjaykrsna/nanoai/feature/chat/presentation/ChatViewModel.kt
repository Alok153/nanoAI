package com.vjaykrsna.nanoai.feature.chat.presentation

import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.error.NanoAIErrorEnvelope
import com.vjaykrsna.nanoai.core.common.error.toErrorEnvelope
import com.vjaykrsna.nanoai.core.common.onFailure
import com.vjaykrsna.nanoai.core.common.onSuccess
import com.vjaykrsna.nanoai.core.domain.chat.PromptAttachments
import com.vjaykrsna.nanoai.core.domain.chat.PromptAudio
import com.vjaykrsna.nanoai.core.domain.chat.PromptImage
import com.vjaykrsna.nanoai.core.domain.library.Model
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.domain.model.Message
import com.vjaykrsna.nanoai.core.model.MessageRole
import com.vjaykrsna.nanoai.core.model.MessageSource
import com.vjaykrsna.nanoai.core.model.PersonaSwitchAction
import com.vjaykrsna.nanoai.feature.chat.domain.ChatFeatureCoordinator
import com.vjaykrsna.nanoai.feature.chat.presentation.state.ChatAudioAttachment
import com.vjaykrsna.nanoai.feature.chat.presentation.state.ChatComposerAttachments
import com.vjaykrsna.nanoai.feature.chat.presentation.state.ChatImageAttachment
import com.vjaykrsna.nanoai.feature.chat.presentation.state.ChatUiState
import com.vjaykrsna.nanoai.shared.state.NanoAIViewEvent
import com.vjaykrsna.nanoai.shared.state.ViewModelStateHost
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

private const val PNG_COMPRESSION_QUALITY = 100

@HiltViewModel
class ChatViewModel
@Inject
constructor(
  private val chatFeatureCoordinator: ChatFeatureCoordinator,
  @MainImmediateDispatcher mainDispatcher: CoroutineDispatcher,
) :
  ViewModelStateHost<ChatUiState, ChatUiEvent>(
    initialState = ChatUiState(),
    dispatcher = mainDispatcher,
  ) {
  private companion object {
    private const val SAVE_MESSAGE_ERROR = "Failed to save message"
    private const val INFERENCE_ERROR = "Failed to start inference"
    private const val PERSONA_SWITCH_ERROR = "Failed to switch persona"
    private const val THREAD_CREATION_ERROR = "Failed to create thread"
    private const val THREAD_ARCHIVE_ERROR = "Failed to archive conversation"
    private const val THREAD_DELETE_ERROR = "Failed to delete conversation"
    private const val MODEL_SELECTION_ERROR = "Failed to select model"
  }

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
        val promptAttachments = attachmentsSnapshot.toPromptAttachments()

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
              attachments = promptAttachments,
            )

          val saveResult = chatFeatureCoordinator.saveMessage(userMessage)
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
      chatFeatureCoordinator
        .updateThread(thread.copy(activeModelId = model.modelId))
        .onFailure { result ->
          val envelope = result.toErrorEnvelope(MODEL_SELECTION_ERROR)
          emitError(ChatError.UnexpectedError(envelope.userMessage), envelope)
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
      chatFeatureCoordinator
        .switchPersona(threadId, newPersonaId, action)
        .onSuccess { newThreadId ->
          if (action == PersonaSwitchAction.START_NEW_THREAD) {
            setActiveThread(newThreadId)
          }
        }
        .onFailure { result ->
          val envelope = result.toErrorEnvelope(PERSONA_SWITCH_ERROR)
          emitError(ChatError.PersonaSwitchFailed(envelope.userMessage), envelope)
        }
    }
  }

  fun createNewThread(personaId: UUID?, title: String? = null) {
    viewModelScope.launch(dispatcher) {
      val defaultPersonaId =
        personaId ?: chatFeatureCoordinator.getDefaultPersona()?.personaId ?: UUID.randomUUID()
      chatFeatureCoordinator
        .createThread(defaultPersonaId, title)
        .onSuccess { threadId -> setActiveThread(threadId) }
        .onFailure { result ->
          val envelope = result.toErrorEnvelope(THREAD_CREATION_ERROR)
          emitError(ChatError.ThreadCreationFailed(envelope.userMessage), envelope)
        }
    }
  }

  fun archiveThread(threadId: UUID) {
    viewModelScope.launch(dispatcher) {
      chatFeatureCoordinator
        .archiveThread(threadId)
        .onSuccess {
          if (state.value.activeThreadId == threadId) {
            setActiveThread(null)
          }
        }
        .onFailure { result ->
          val envelope = result.toErrorEnvelope(THREAD_ARCHIVE_ERROR)
          emitError(ChatError.ThreadArchiveFailed(envelope.userMessage), envelope)
        }
    }
  }

  fun deleteThread(threadId: UUID) {
    viewModelScope.launch(dispatcher) {
      chatFeatureCoordinator
        .deleteThread(threadId)
        .onSuccess {
          if (state.value.activeThreadId == threadId) {
            setActiveThread(null)
          }
        }
        .onFailure { result ->
          val envelope = result.toErrorEnvelope(THREAD_DELETE_ERROR)
          emitError(ChatError.ThreadDeletionFailed(envelope.userMessage), envelope)
        }
    }
  }

  private fun observeThreads() {
    viewModelScope.launch(dispatcher) {
      chatFeatureCoordinator.observeThreads().collect { threads ->
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
          threadId?.let { chatFeatureCoordinator.observeMessages(it) } ?: flowOf(emptyList())
        }
        .collect { messages -> updateState { copy(messages = messages.toPersistentList()) } }
    }
  }

  private fun observePersonas() {
    viewModelScope.launch(dispatcher) {
      chatFeatureCoordinator.observePersonas().collect { personas ->
        updateState { copy(personas = personas.toPersistentList()) }
      }
    }
  }

  private fun observeInstalledModels() {
    viewModelScope.launch(dispatcher) {
      chatFeatureCoordinator.observeInstalledModels().collect { models ->
        updateState { copy(installedModels = models.toPersistentList()) }
      }
    }
  }

  private fun setActiveThread(threadId: UUID?) {
    currentThreadId.value = threadId
    updateState {
      val activeThread = threadId?.let { id -> threads.firstOrNull { it.threadId == id } }
      copy(activeThreadId = threadId, activeThread = activeThread)
    }
  }

  private suspend fun emitError(
    error: ChatError,
    envelope: NanoAIErrorEnvelope = NanoAIErrorEnvelope(error.message),
  ) {
    updateState { copy(pendingErrorMessage = envelope.userMessage) }
    emitEvent(ChatUiEvent.ErrorRaised(error, envelope))
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
      else -> {
        val envelope = saveResult.toErrorEnvelope(SAVE_MESSAGE_ERROR)
        emitError(ChatError.UnexpectedError(envelope.userMessage), envelope)
        updateState { copy(isSendingMessage = false) }
      }
    }
  }

  private suspend fun handleInference(messageData: MessageData) {
    val inferenceResult =
      chatFeatureCoordinator.sendPrompt(
        messageData.threadId,
        messageData.text,
        messageData.personaId,
        messageData.attachments,
      )
    when (inferenceResult) {
      is NanoAIResult.Success -> clearAttachments()
      else -> {
        val envelope = inferenceResult.toErrorEnvelope(INFERENCE_ERROR)
        emitError(ChatError.InferenceFailed(envelope.userMessage), envelope)
      }
    }
  }

  private data class MessageData(
    val threadId: UUID,
    val text: String,
    val personaId: UUID,
    val attachments: PromptAttachments,
  )
}

private fun ChatComposerAttachments.toPromptAttachments(): PromptAttachments {
  val promptImage =
    image?.let { attachment ->
      val bytes = attachment.bitmap.toCompressedPng()
      bytes?.let {
        PromptImage(
          bytes = it,
          mimeType = "image/png",
          width = attachment.bitmap.width,
          height = attachment.bitmap.height,
        )
      }
    }
  val promptAudio = audio?.let { PromptAudio(bytes = it.data, mimeType = it.mimeType) }
  return PromptAttachments(image = promptImage, audio = promptAudio)
}

private fun Bitmap.toCompressedPng(): ByteArray? {
  return runCatching {
      ByteArrayOutputStream().use { stream ->
        compress(Bitmap.CompressFormat.PNG, PNG_COMPRESSION_QUALITY, stream)
        stream.toByteArray()
      }
    }
    .getOrNull()
}

sealed interface ChatUiEvent : NanoAIViewEvent {
  data class ErrorRaised(val error: ChatError, val envelope: NanoAIErrorEnvelope) : ChatUiEvent

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
