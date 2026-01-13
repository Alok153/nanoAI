package com.vjaykrsna.nanoai.feature.chat.presentation

import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.error.NanoAIErrorEnvelope
import com.vjaykrsna.nanoai.core.common.error.toErrorEnvelope
import com.vjaykrsna.nanoai.core.common.error.withFallbackMessage
import com.vjaykrsna.nanoai.core.domain.chat.ConversationUseCase
import com.vjaykrsna.nanoai.feature.chat.presentation.state.HistoryUiState
import com.vjaykrsna.nanoai.shared.state.NanoAIViewEvent
import com.vjaykrsna.nanoai.shared.state.ViewModelStateHost
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel for chat history screen.
 *
 * Manages loading and displaying chat threads.
 */
@HiltViewModel
class HistoryViewModel
@Inject
constructor(
  private val conversationUseCase: ConversationUseCase,
  @MainImmediateDispatcher mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) :
  ViewModelStateHost<HistoryUiState, HistoryUiEvent>(
    initialState = HistoryUiState(),
    dispatcher = mainDispatcher,
  ) {

  companion object {
    private const val LOAD_THREADS_ERROR = "Unable to load chat history"
    private const val ARCHIVE_THREAD_ERROR = "Unable to archive conversation"
    private const val DELETE_THREAD_ERROR = "Unable to delete conversation"
  }

  init {
    observeThreads()
    loadThreads()
  }

  fun loadThreads() {
    updateState { copy(isLoading = true, lastErrorMessage = null) }
    viewModelScope.launch(dispatcher) {
      val result = refreshThreadsSnapshot()
      if (result !is NanoAIResult.Success) {
        emitError(
          result.toErrorEnvelope(LOAD_THREADS_ERROR).withFallbackMessage(LOAD_THREADS_ERROR)
        )
      }
      updateState { copy(isLoading = false) }
    }
  }

  fun archiveThread(threadId: UUID) {
    viewModelScope.launch(dispatcher) {
      when (val result = conversationUseCase.archiveThread(threadId)) {
        is NanoAIResult.Success -> Unit
        else ->
          emitError(
            result.toErrorEnvelope(ARCHIVE_THREAD_ERROR).withFallbackMessage(ARCHIVE_THREAD_ERROR)
          )
      }
    }
  }

  fun deleteThread(threadId: UUID) {
    viewModelScope.launch(dispatcher) {
      when (val result = conversationUseCase.deleteThread(threadId)) {
        is NanoAIResult.Success -> Unit
        else ->
          emitError(
            result.toErrorEnvelope(DELETE_THREAD_ERROR).withFallbackMessage(DELETE_THREAD_ERROR)
          )
      }
    }
  }

  fun clearErrorMessage() {
    updateState { copy(lastErrorMessage = null) }
  }

  private fun observeThreads() {
    viewModelScope.launch(dispatcher) {
      conversationUseCase.getAllThreadsFlow().collectLatest { threads ->
        updateState { copy(threads = threads.toPersistentList()) }
      }
    }
  }

  private suspend fun emitError(envelope: NanoAIErrorEnvelope) {
    updateState { copy(lastErrorMessage = envelope.userMessage) }
    emitEvent(HistoryUiEvent.ErrorRaised(envelope))
  }

  private suspend fun refreshThreadsSnapshot(): NanoAIResult<Unit> =
    when (val result = conversationUseCase.getAllThreads()) {
      is NanoAIResult.Success -> {
        updateState { copy(threads = result.value.toPersistentList()) }
        NanoAIResult.success(Unit)
      }
      is NanoAIResult.RecoverableError ->
        NanoAIResult.recoverable(
          message = result.message.ifBlank { LOAD_THREADS_ERROR },
          cause = result.cause,
          retryAfterSeconds = result.retryAfterSeconds,
          telemetryId = result.telemetryId,
          context = result.context.withOperationFallback("loadThreads"),
        )
      is NanoAIResult.FatalError ->
        NanoAIResult.fatal(
          message = result.message.ifBlank { LOAD_THREADS_ERROR },
          supportContact = result.supportContact,
          cause = result.cause,
          telemetryId = result.telemetryId,
          context = result.context.withOperationFallback("loadThreads"),
        )
    }
}

sealed interface HistoryUiEvent : NanoAIViewEvent {
  data class ErrorRaised(val error: NanoAIErrorEnvelope) : HistoryUiEvent
}

private fun Map<String, String>.withOperationFallback(operation: String): Map<String, String> {
  if (get("operation") == operation) return this
  return this + ("operation" to operation)
}
