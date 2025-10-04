package com.vjaykrsna.nanoai.feature.sidebar.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.data.repository.ConversationRepository
import com.vjaykrsna.nanoai.core.data.repository.InferencePreferenceRepository
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.domain.model.InferencePreference
import com.vjaykrsna.nanoai.core.model.InferenceMode
import com.vjaykrsna.nanoai.feature.uiux.domain.ObserveUserProfileUseCase
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val FLOW_STOP_TIMEOUT_MS = 5_000L

@HiltViewModel
class SidebarViewModel
@Inject
constructor(
  private val conversationRepository: ConversationRepository,
  private val inferencePreferenceRepository: InferencePreferenceRepository,
  private val observeUserProfileUseCase: ObserveUserProfileUseCase,
) : ViewModel() {
  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _showArchived = MutableStateFlow(false)
  val showArchived: StateFlow<Boolean> = _showArchived.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _errorEvents = MutableSharedFlow<SidebarError>()
  val errorEvents = _errorEvents.asSharedFlow()

  private val _drawerOpen = MutableStateFlow(false)
  val drawerOpen: StateFlow<Boolean> = _drawerOpen.asStateFlow()

  private val _pinnedTools = MutableStateFlow<List<String>>(emptyList())
  val pinnedTools: StateFlow<List<String>> = _pinnedTools.asStateFlow()

  private val _navigationEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
  val navigationEvents = _navigationEvents.asSharedFlow()

  private val allThreadsFlow = conversationRepository.getAllThreadsFlow()

  private val inferencePreferenceFlow = inferencePreferenceRepository.observeInferencePreference()

  val inferencePreference: StateFlow<InferencePreference> =
    inferencePreferenceFlow.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MS),
      InferencePreference(),
    )

  val threads: StateFlow<List<ChatThread>> =
    combine(
        allThreadsFlow,
        _searchQuery,
        _showArchived,
      ) { threads, query, archived ->
        threads
          .filter { thread ->
            // Filter by archive status
            if (archived) thread.isArchived else !thread.isArchived
          }
          .filter { thread ->
            // Filter by search query
            if (query.isBlank()) {
              true
            } else {
              thread.title?.contains(query, ignoreCase = true) ?: false
            }
          }
          .sortedByDescending { it.updatedAt }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MS), emptyList())

  val archivedThreads: StateFlow<List<ChatThread>> =
    allThreadsFlow
      .combine(_searchQuery) { threads, query ->
        threads
          .filter { it.isArchived }
          .filter { thread ->
            if (query.isBlank()) {
              true
            } else {
              thread.title?.contains(query, ignoreCase = true) ?: false
            }
          }
          .sortedByDescending { it.updatedAt }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MS), emptyList())

  init {
    observeUserProfileUseCase.flow
      .onEach { result ->
        val pinned = result.userProfile?.pinnedTools ?: emptyList()
        _pinnedTools.value = pinned
      }
      .launchIn(viewModelScope)
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun toggleShowArchived() {
    _showArchived.value = !_showArchived.value
  }

  fun archiveThread(threadId: UUID) {
    viewModelScope.launch {
      _isLoading.value = true
      runCatching { conversationRepository.archiveThread(threadId) }
        .onFailure { error ->
          _errorEvents.emit(
            SidebarError.ArchiveFailed(error.message ?: "Failed to archive thread"),
          )
        }
      _isLoading.value = false
    }
  }

  fun deleteThread(threadId: UUID) {
    viewModelScope.launch {
      _isLoading.value = true
      runCatching { conversationRepository.deleteThread(threadId) }
        .onFailure { error ->
          _errorEvents.emit(
            SidebarError.DeleteFailed(error.message ?: "Failed to delete thread"),
          )
        }
      _isLoading.value = false
    }
  }

  fun createNewThread(personaId: UUID?, title: String? = null) {
    viewModelScope.launch {
      runCatching { conversationRepository.createNewThread(personaId ?: UUID.randomUUID(), title) }
        .onFailure { error ->
          _errorEvents.emit(
            SidebarError.CreateFailed(error.message ?: "Failed to create thread"),
          )
        }
    }
  }

  fun setInferenceMode(mode: InferenceMode) {
    viewModelScope.launch {
      runCatching { inferencePreferenceRepository.setInferenceMode(mode) }
        .onFailure { error ->
          _errorEvents.emit(
            SidebarError.PreferenceUpdateFailed(
              error.message ?: "Failed to update inference preference",
            ),
          )
        }
    }
  }

  fun openDrawer() {
    _drawerOpen.value = true
  }

  fun closeDrawer() {
    _drawerOpen.value = false
  }

  fun emitNavigation(route: String) {
    _navigationEvents.tryEmit(route)
  }

  fun reorderPinnedTools(order: List<String>) {
    _pinnedTools.value = order
  }
}

sealed class SidebarError {
  data class ArchiveFailed(
    val message: String,
  ) : SidebarError()

  data class DeleteFailed(
    val message: String,
  ) : SidebarError()

  data class CreateFailed(
    val message: String,
  ) : SidebarError()

  data class PreferenceUpdateFailed(
    val message: String,
  ) : SidebarError()
}
