package com.vjaykrsna.nanoai.feature.chat.presentation

import android.database.sqlite.SQLiteException
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.core.domain.repository.ConversationRepository
import com.vjaykrsna.nanoai.feature.chat.presentation.state.HistoryUiState
import com.vjaykrsna.nanoai.shared.state.NanoAIViewEvent
import com.vjaykrsna.nanoai.shared.state.ViewModelStateHost
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
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
  private val conversationRepository: ConversationRepository,
  @MainImmediateDispatcher mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) :
  ViewModelStateHost<HistoryUiState, HistoryUiEvent>(
    initialState = HistoryUiState(),
    dispatcher = mainDispatcher,
  ) {

  init {
    observeThreads()
    loadThreads()
  }

  fun loadThreads() {
    updateState { copy(isLoading = true, pendingErrorMessage = null) }
    viewModelScope.launch(dispatcher) {
      try {
        conversationRepository.getAllThreads()
      } catch (throwable: Throwable) {
        handleRepositoryFailure(throwable) { message -> HistoryError.LoadFailed(message) }
      } finally {
        updateState { copy(isLoading = false) }
      }
    }
  }

  fun archiveThread(threadId: UUID) {
    viewModelScope.launch(dispatcher) {
      try {
        conversationRepository.archiveThread(threadId)
      } catch (throwable: Throwable) {
        handleRepositoryFailure(throwable) { message -> HistoryError.ArchiveFailed(message) }
      }
    }
  }

  fun deleteThread(threadId: UUID) {
    viewModelScope.launch(dispatcher) {
      try {
        conversationRepository.deleteThread(threadId)
      } catch (throwable: Throwable) {
        handleRepositoryFailure(throwable) { message -> HistoryError.DeleteFailed(message) }
      }
    }
  }

  fun clearPendingError() {
    updateState { copy(pendingErrorMessage = null) }
  }

  private fun observeThreads() {
    viewModelScope.launch(dispatcher) {
      conversationRepository.getAllThreadsFlow().collectLatest { threads ->
        updateState { copy(threads = threads) }
      }
    }
  }

  private suspend fun handleRepositoryFailure(
    throwable: Throwable,
    builder: (String) -> HistoryError,
  ) {
    when (throwable) {
      is CancellationException -> throw throwable
      is SQLiteException,
      is IOException,
      is IllegalStateException -> emitError(builder(throwable.message ?: "Unknown error"))
      else -> throw throwable
    }
  }

  private suspend fun emitError(error: HistoryError) {
    updateState { copy(pendingErrorMessage = error.message) }
    emitEvent(HistoryUiEvent.ErrorRaised(error))
  }
}

sealed interface HistoryUiEvent : NanoAIViewEvent {
  data class ErrorRaised(val error: HistoryError) : HistoryUiEvent
}

sealed class HistoryError(open val message: String) {
  data class LoadFailed(override val message: String) : HistoryError(message)

  data class ArchiveFailed(override val message: String) : HistoryError(message)

  data class DeleteFailed(override val message: String) : HistoryError(message)
}
