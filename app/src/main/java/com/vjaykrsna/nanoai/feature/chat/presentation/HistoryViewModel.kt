package com.vjaykrsna.nanoai.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.domain.usecase.GetConversationHistoryUseCase
import com.vjaykrsna.nanoai.feature.chat.domain.ConversationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
  private val getConversationHistoryUseCase: GetConversationHistoryUseCase,
  private val conversationUseCase: ConversationUseCase,
  @Suppress("UnusedPrivateProperty")
  @MainImmediateDispatcher
  private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {

  private val _threads = MutableStateFlow<List<ChatThread>>(emptyList())
  val threads: StateFlow<List<ChatThread>> = _threads.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _errors = MutableSharedFlow<HistoryError>()
  val errors = _errors.asSharedFlow()

  init {
    loadThreads()
  }

  fun loadThreads() {
    _isLoading.value = true
    viewModelScope.launch {
      when (val result = getConversationHistoryUseCase()) {
        is NanoAIResult.Success -> {
          _threads.value = result.value
        }
        is NanoAIResult.RecoverableError -> {
          _errors.emit(HistoryError.LoadFailed(result.message ?: "Unknown error"))
        }
        is NanoAIResult.FatalError -> {
          _errors.emit(HistoryError.LoadFailed(result.message ?: "Unknown error"))
        }
      }
      _isLoading.value = false
    }
  }

  fun archiveThread(threadId: java.util.UUID) {
    viewModelScope.launch {
      when (conversationUseCase.archiveThread(threadId)) {
        is NanoAIResult.Success -> {
          loadThreads() // Reload after archive
        }
        is NanoAIResult.RecoverableError -> {
          _errors.emit(HistoryError.ArchiveFailed("Failed to archive thread"))
        }
        is NanoAIResult.FatalError -> {
          _errors.emit(HistoryError.ArchiveFailed("Failed to archive thread"))
        }
      }
    }
  }

  fun deleteThread(threadId: java.util.UUID) {
    viewModelScope.launch {
      when (conversationUseCase.deleteThread(threadId)) {
        is NanoAIResult.Success -> {
          loadThreads() // Reload after delete
        }
        is NanoAIResult.RecoverableError -> {
          _errors.emit(HistoryError.DeleteFailed("Failed to delete thread"))
        }
        is NanoAIResult.FatalError -> {
          _errors.emit(HistoryError.DeleteFailed("Failed to delete thread"))
        }
      }
    }
  }
}

sealed class HistoryError {
  data class LoadFailed(val message: String) : HistoryError()

  data class ArchiveFailed(val message: String) : HistoryError()

  data class DeleteFailed(val message: String) : HistoryError()
}
