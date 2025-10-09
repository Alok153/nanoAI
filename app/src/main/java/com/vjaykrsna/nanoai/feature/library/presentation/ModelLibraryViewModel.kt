package com.vjaykrsna.nanoai.feature.library.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.data.ModelCatalogRepository
import com.vjaykrsna.nanoai.feature.library.domain.ModelDownloadsAndExportUseCase
import com.vjaykrsna.nanoai.feature.library.model.InstallState
import com.vjaykrsna.nanoai.feature.library.model.ProviderType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val FLOW_STOP_TIMEOUT_MS = 5_000L

@HiltViewModel
class ModelLibraryViewModel
@Inject
constructor(
  private val modelDownloadsAndExportUseCase: ModelDownloadsAndExportUseCase,
  private val modelCatalogRepository: ModelCatalogRepository,
) : ViewModel() {
  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _errorEvents = MutableSharedFlow<LibraryError>()
  val errorEvents = _errorEvents.asSharedFlow()

  private val _filters = MutableStateFlow(LibraryFilterState())
  val filters: StateFlow<LibraryFilterState> = _filters.asStateFlow()

  val allModels: StateFlow<List<ModelPackage>> =
    modelCatalogRepository
      .observeAllModels()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MS), emptyList())

  val installedModels: StateFlow<List<ModelPackage>> =
    modelCatalogRepository
      .observeInstalledModels()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MS), emptyList())

  val providerOptions: StateFlow<List<ProviderType>> =
    allModels
      .map { models -> models.map(ModelPackage::providerType).distinct().sortedBy { it.name } }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MS), emptyList())

  val capabilityOptions: StateFlow<List<String>> =
    allModels
      .map { models ->
        models
          .flatMap { it.capabilities }
          .map { it.trim() }
          .filter { it.isNotBlank() }
          .map { it.lowercase(Locale.US) }
          .distinct()
          .sorted()
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MS), emptyList())

  val sections: StateFlow<ModelLibrarySections> =
    combine(allModels, filters) { models, filterState ->
        val filtered = models.filterBy(filterState)
        ModelLibrarySections(
          activeDownloads =
            filtered.filter {
              it.installState == InstallState.DOWNLOADING || it.installState == InstallState.PAUSED
            },
          attention = filtered.filter { it.installState == InstallState.ERROR },
          installed = filtered.filter { it.installState == InstallState.INSTALLED },
          available = filtered.filter { it.installState == InstallState.NOT_INSTALLED },
        )
      }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MS),
        ModelLibrarySections(),
      )

  val summary: StateFlow<ModelLibrarySummary> =
    combine(allModels, installedModels) { all, installed ->
        val activeCount =
          all.count {
            it.installState == InstallState.DOWNLOADING || it.installState == InstallState.PAUSED
          }
        val attentionCount = all.count { it.installState == InstallState.ERROR }
        ModelLibrarySummary(
          total = all.size,
          installed = installed.size,
          active = activeCount,
          attention = attentionCount,
          installedBytes = installed.sumOf(ModelPackage::sizeBytes),
        )
      }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MS),
        ModelLibrarySummary(),
      )

  val hasActiveFilters: StateFlow<Boolean> =
    filters
      .map { it.hasActiveFilters }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MS), false)

  val queuedDownloads: StateFlow<List<DownloadTask>> =
    modelDownloadsAndExportUseCase
      .getQueuedDownloads()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MS), emptyList())

  fun updateSearchQuery(query: String) {
    _filters.update { it.copy(searchQuery = query) }
  }

  fun selectProvider(providerType: ProviderType?) {
    _filters.update { it.copy(provider = providerType) }
  }

  fun toggleCapability(capability: String) {
    _filters.update { state ->
      val normalized = capability.lowercase(Locale.US)
      val updated =
        if (normalized in state.capabilities) state.capabilities - normalized
        else state.capabilities + normalized
      state.copy(capabilities = updated)
    }
  }

  fun setSort(sort: ModelSort) {
    _filters.update { it.copy(sort = sort) }
  }

  fun clearFilters() {
    _filters.value = LibraryFilterState()
  }

  fun downloadModel(modelId: String) {
    viewModelScope.launch {
      _isLoading.value = true
      runCatching { modelDownloadsAndExportUseCase.downloadModel(modelId) }
        .onSuccess { result ->
          result.onFailure { error ->
            _errorEvents.emit(
              LibraryError.DownloadFailed(modelId, error.message ?: "Unknown error"),
            )
          }
        }
        .onFailure { error ->
          _errorEvents.emit(LibraryError.UnexpectedError(error.message ?: "Unexpected error"))
        }
      _isLoading.value = false
    }
  }

  fun pauseDownload(taskId: UUID) {
    viewModelScope.launch {
      runCatching { modelDownloadsAndExportUseCase.pauseDownload(taskId) }
        .onFailure { error ->
          _errorEvents.emit(
            LibraryError.PauseFailed(taskId.toString(), error.message ?: "Failed to pause"),
          )
        }
    }
  }

  fun resumeDownload(taskId: UUID) {
    viewModelScope.launch {
      runCatching { modelDownloadsAndExportUseCase.resumeDownload(taskId) }
        .onFailure { error ->
          _errorEvents.emit(
            LibraryError.ResumeFailed(taskId.toString(), error.message ?: "Failed to resume"),
          )
        }
    }
  }

  fun cancelDownload(taskId: UUID) {
    viewModelScope.launch {
      runCatching { modelDownloadsAndExportUseCase.cancelDownload(taskId) }
        .onFailure { error ->
          _errorEvents.emit(
            LibraryError.CancelFailed(taskId.toString(), error.message ?: "Failed to cancel"),
          )
        }
    }
  }

  fun retryDownload(taskId: UUID) {
    viewModelScope.launch {
      runCatching { modelDownloadsAndExportUseCase.retryFailedDownload(taskId) }
        .onFailure { error ->
          _errorEvents.emit(
            LibraryError.RetryFailed(taskId.toString(), error.message ?: "Failed to retry"),
          )
        }
    }
  }

  fun deleteModel(modelId: String) {
    viewModelScope.launch {
      _isLoading.value = true
      runCatching { modelDownloadsAndExportUseCase.deleteModel(modelId) }
        .onSuccess { result ->
          result.onFailure { error ->
            _errorEvents.emit(
              LibraryError.DeleteFailed(modelId, error.message ?: "Unknown error"),
            )
          }
        }
        .onFailure { error ->
          _errorEvents.emit(LibraryError.UnexpectedError(error.message ?: "Unexpected error"))
        }
      _isLoading.value = false
    }
  }

  fun observeDownloadProgress(taskId: UUID): StateFlow<Float> =
    modelDownloadsAndExportUseCase
      .getDownloadProgress(taskId)
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MS), 0f)

  private fun List<ModelPackage>.filterBy(filters: LibraryFilterState): List<ModelPackage> {
    var current = this
    val query = filters.searchQuery.trim()
    if (query.isNotEmpty()) {
      val normalized = query.lowercase(Locale.US)
      current =
        current.filter { model ->
          model.displayName.lowercase(Locale.US).contains(normalized) ||
            model.modelId.lowercase(Locale.US).contains(normalized) ||
            model.capabilities.any { capability ->
              capability.lowercase(Locale.US).contains(normalized)
            }
        }
    }

    filters.provider?.let { provider -> current = current.filter { it.providerType == provider } }

    if (filters.capabilities.isNotEmpty()) {
      current =
        current.filter { model ->
          model.capabilities
            .map { it.lowercase(Locale.US) }
            .any { capability -> capability in filters.capabilities }
        }
    }

    return current.sortBy(filters.sort)
  }

  private fun List<ModelPackage>.sortBy(sort: ModelSort): List<ModelPackage> =
    when (sort) {
      ModelSort.RECOMMENDED ->
        sortedWith(
          compareBy<ModelPackage> { it.installState != InstallState.INSTALLED }
            .thenBy {
              it.installState != InstallState.DOWNLOADING && it.installState != InstallState.PAUSED
            }
            .thenBy { it.displayName.lowercase(Locale.US) },
        )
      ModelSort.NAME -> sortedBy { it.displayName.lowercase(Locale.US) }
      ModelSort.SIZE_DESC -> sortedByDescending(ModelPackage::sizeBytes)
      ModelSort.UPDATED -> sortedByDescending(ModelPackage::updatedAt)
    }
}

sealed class LibraryError {
  data class DownloadFailed(
    val modelId: String,
    val message: String,
  ) : LibraryError()

  data class PauseFailed(
    val taskId: String,
    val message: String,
  ) : LibraryError()

  data class ResumeFailed(
    val taskId: String,
    val message: String,
  ) : LibraryError()

  data class CancelFailed(
    val taskId: String,
    val message: String,
  ) : LibraryError()

  data class RetryFailed(
    val taskId: String,
    val message: String,
  ) : LibraryError()

  data class DeleteFailed(
    val modelId: String,
    val message: String,
  ) : LibraryError()

  data class UnexpectedError(
    val message: String,
  ) : LibraryError()
}

data class LibraryFilterState(
  val searchQuery: String = "",
  val provider: ProviderType? = null,
  val capabilities: Set<String> = emptySet(),
  val sort: ModelSort = ModelSort.RECOMMENDED,
) {
  val hasActiveFilters: Boolean
    get() =
      searchQuery.isNotBlank() ||
        provider != null ||
        capabilities.isNotEmpty() ||
        sort != ModelSort.RECOMMENDED
}

data class ModelLibrarySections(
  val activeDownloads: List<ModelPackage> = emptyList(),
  val attention: List<ModelPackage> = emptyList(),
  val installed: List<ModelPackage> = emptyList(),
  val available: List<ModelPackage> = emptyList(),
)

data class ModelLibrarySummary(
  val total: Int = 0,
  val installed: Int = 0,
  val active: Int = 0,
  val attention: Int = 0,
  val installedBytes: Long = 0,
)

enum class ModelSort {
  RECOMMENDED,
  NAME,
  SIZE_DESC,
  UPDATED,
}
