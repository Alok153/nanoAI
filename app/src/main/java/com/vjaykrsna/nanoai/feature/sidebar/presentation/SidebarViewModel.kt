package com.vjaykrsna.nanoai.feature.sidebar.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.data.repository.ConversationRepository
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
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

@HiltViewModel
class SidebarViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showArchived = MutableStateFlow(false)
    val showArchived: StateFlow<Boolean> = _showArchived.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorEvents = MutableSharedFlow<SidebarError>()
    val errorEvents = _errorEvents.asSharedFlow()

    private val allThreadsFlow = conversationRepository.getAllThreadsFlow()

    val threads: StateFlow<List<ChatThread>> = combine(
        allThreadsFlow,
        _searchQuery,
        _showArchived
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedThreads: StateFlow<List<ChatThread>> = allThreadsFlow
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleShowArchived() {
        _showArchived.value = !_showArchived.value
    }

    fun archiveThread(threadId: UUID) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                conversationRepository.archiveThread(threadId)
            } catch (e: Exception) {
                _errorEvents.emit(SidebarError.ArchiveFailed(e.message ?: "Failed to archive thread"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteThread(threadId: UUID) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                conversationRepository.deleteThread(threadId)
            } catch (e: Exception) {
                _errorEvents.emit(SidebarError.DeleteFailed(e.message ?: "Failed to delete thread"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createNewThread(personaId: UUID?, title: String? = null) {
        viewModelScope.launch {
            try {
                conversationRepository.createNewThread(personaId ?: UUID.randomUUID(), title)
            } catch (e: Exception) {
                _errorEvents.emit(SidebarError.CreateFailed(e.message ?: "Failed to create thread"))
            }
        }
    }
}

sealed class SidebarError {
    data class ArchiveFailed(val message: String) : SidebarError()
    data class DeleteFailed(val message: String) : SidebarError()
    data class CreateFailed(val message: String) : SidebarError()
}
