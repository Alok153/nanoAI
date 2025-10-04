package com.vjaykrsna.nanoai.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.data.repository.ConversationRepository
import com.vjaykrsna.nanoai.core.data.repository.PersonaRepository
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.domain.model.Message
import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import com.vjaykrsna.nanoai.core.model.PersonaSwitchAction
import com.vjaykrsna.nanoai.feature.chat.domain.SendPromptAndPersonaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val FLOW_STOP_TIMEOUT_MS = 5_000L

@HiltViewModel
class ChatViewModel
@Inject
constructor(
  private val sendPromptAndPersonaUseCase: SendPromptAndPersonaUseCase,
  private val conversationRepository: ConversationRepository,
  private val personaRepository: PersonaRepository,
) : ViewModel() {
  private val _currentThreadId = MutableStateFlow<UUID?>(null)
  val currentThreadId: StateFlow<UUID?> = _currentThreadId.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _errorEvents = MutableSharedFlow<ChatError>()
  val errorEvents = _errorEvents.asSharedFlow()

  val messages: StateFlow<List<Message>> =
    _currentThreadId
      .combine(conversationRepository.getAllThreadsFlow()) { threadId, _ ->
        threadId?.let { conversationRepository.getMessages(it) } ?: emptyList()
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MS), emptyList())

  val currentThread: StateFlow<ChatThread?> =
    _currentThreadId
      .combine(conversationRepository.getAllThreadsFlow()) { threadId, _ ->
        threadId?.let { conversationRepository.getThread(it) }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MS), null)

  val availablePersonas: StateFlow<List<PersonaProfile>> =
    personaRepository
      .observeAllPersonas()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MS), emptyList())

  fun selectThread(threadId: UUID) {
    _currentThreadId.value = threadId
  }

  fun sendMessage(text: String, personaId: UUID) {
    val threadId = _currentThreadId.value ?: return
    viewModelScope.launch {
      _isLoading.value = true
      runCatching {
          val userMessage =
            Message(
              messageId = UUID.randomUUID(),
              threadId = threadId,
              role = com.vjaykrsna.nanoai.core.model.Role.USER,
              text = text,
              source = com.vjaykrsna.nanoai.core.model.MessageSource.LOCAL_MODEL,
              latencyMs = null,
              createdAt = kotlinx.datetime.Clock.System.now(),
            )
          conversationRepository.saveMessage(userMessage)
          sendPromptAndPersonaUseCase.sendPrompt(threadId, text, personaId)
        }
        .onSuccess { result ->
          result.onFailure { error ->
            _errorEvents.emit(ChatError.InferenceFailed(error.message ?: "Unknown error"))
          }
        }
        .onFailure { error ->
          _errorEvents.emit(ChatError.UnexpectedError(error.message ?: "Unexpected error"))
        }
      _isLoading.value = false
    }
  }

  fun switchPersona(newPersonaId: UUID, action: PersonaSwitchAction) {
    val threadId = _currentThreadId.value ?: return
    viewModelScope.launch {
      runCatching { sendPromptAndPersonaUseCase.switchPersona(threadId, newPersonaId, action) }
        .onSuccess { newThreadId ->
          if (action == PersonaSwitchAction.START_NEW_THREAD) {
            _currentThreadId.value = newThreadId
          }
        }
        .onFailure { error ->
          _errorEvents.emit(
            ChatError.PersonaSwitchFailed(error.message ?: "Failed to switch persona"),
          )
        }
    }
  }

  fun createNewThread(personaId: UUID?, title: String? = null) {
    viewModelScope.launch {
      runCatching {
          conversationRepository.createNewThread(
            personaId ?: personaRepository.getDefaultPersona()?.personaId ?: UUID.randomUUID(),
            title,
          )
        }
        .onSuccess { threadId -> _currentThreadId.value = threadId }
        .onFailure { error ->
          _errorEvents.emit(
            ChatError.ThreadCreationFailed(error.message ?: "Failed to create thread"),
          )
        }
    }
  }

  fun archiveThread(threadId: UUID) {
    viewModelScope.launch {
      runCatching { conversationRepository.archiveThread(threadId) }
        .onSuccess {
          if (_currentThreadId.value == threadId) {
            _currentThreadId.value = null
          }
        }
        .onFailure { error ->
          _errorEvents.emit(
            ChatError.ThreadArchiveFailed(error.message ?: "Failed to archive thread"),
          )
        }
    }
  }

  fun deleteThread(threadId: UUID) {
    viewModelScope.launch {
      runCatching { conversationRepository.deleteThread(threadId) }
        .onSuccess {
          if (_currentThreadId.value == threadId) {
            _currentThreadId.value = null
          }
        }
        .onFailure { error ->
          _errorEvents.emit(
            ChatError.ThreadDeletionFailed(error.message ?: "Failed to delete thread"),
          )
        }
    }
  }
}

sealed class ChatError {
  data class InferenceFailed(
    val message: String,
  ) : ChatError()

  data class PersonaSwitchFailed(
    val message: String,
  ) : ChatError()

  data class ThreadCreationFailed(
    val message: String,
  ) : ChatError()

  data class ThreadArchiveFailed(
    val message: String,
  ) : ChatError()

  data class ThreadDeletionFailed(
    val message: String,
  ) : ChatError()

  data class UnexpectedError(
    val message: String,
  ) : ChatError()
}
