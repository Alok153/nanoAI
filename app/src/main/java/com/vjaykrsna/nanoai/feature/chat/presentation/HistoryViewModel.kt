package com.vjaykrsna.nanoai.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.domain.repository.ConversationRepository
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
    observeThreads()
    loadThreads()
  }

  fun loadThreads() {
    _isLoading.value = true
    viewModelScope.launch(dispatcher) {
      try {
        conversationRepository.getAllThreads()
      } catch (e: Exception) {
        _errors.emit(HistoryError.LoadFailed(e.message ?: "Unknown error"))
      }
      _isLoading.value = false
    }
  }

  fun archiveThread(threadId: UUID) {
    viewModelScope.launch(dispatcher) {
      try {
        conversationRepository.archiveThread(threadId)
        loadThreads() // Reload after archive
      } catch (e: Exception) {
        _errors.emit(HistoryError.ArchiveFailed(e.message ?: "Unknown error"))
      }
    }
  }

  fun deleteThread(threadId: UUID) {
    viewModelScope.launch(dispatcher) {
      try {
        conversationRepository.deleteThread(threadId)
        loadThreads() // Reload after delete
      } catch (e: Exception) {
        _errors.emit(HistoryError.DeleteFailed(e.message ?: "Unknown error"))
      }
    }
  }

  private fun observeThreads() {
    viewModelScope.launch(dispatcher) {
      conversationRepository.getAllThreadsFlow().collectLatest { threads ->
        _threads.value = threads
      }
    }
  }
}

sealed class HistoryError {
  data class LoadFailed(val message: String) : HistoryError()

  data class ArchiveFailed(val message: String) : HistoryError()

  data class DeleteFailed(val message: String) : HistoryError()
}
