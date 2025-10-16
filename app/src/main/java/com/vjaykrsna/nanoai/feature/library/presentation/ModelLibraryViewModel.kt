@file:Suppress("LargeClass")

package com.vjaykrsna.nanoai.feature.library.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.data.ModelCatalogRepository
import com.vjaykrsna.nanoai.feature.library.domain.ModelDownloadsAndExportUseCaseInterface
import com.vjaykrsna.nanoai.feature.library.domain.RefreshModelCatalogUseCase
import com.vjaykrsna.nanoai.feature.library.model.DownloadStatus
import com.vjaykrsna.nanoai.feature.library.model.InstallState
import com.vjaykrsna.nanoai.feature.library.model.ProviderType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
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

@HiltViewModel
class ModelLibraryViewModel
@Inject
constructor(
  private val modelDownloadsAndExportUseCase: ModelDownloadsAndExportUseCaseInterface,
  private val modelCatalogRepository: ModelCatalogRepository,
  private val refreshModelCatalogUseCase: RefreshModelCatalogUseCase,
) : ViewModel() {
  private val downloadObservers = mutableMapOf<UUID, Job>()
  private val activeLoadingOperations = AtomicInteger(0)
  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _isRefreshing = MutableStateFlow(false)
  val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

  private val _errorEvents = MutableSharedFlow<LibraryError>()
  val errorEvents = _errorEvents.asSharedFlow()

  private val _filters = MutableStateFlow(LibraryFilterState())
  val filters: StateFlow<LibraryFilterState> = _filters.asStateFlow()

  val allModels: StateFlow<List<ModelPackage>> =
    modelCatalogRepository
      .observeAllModels()
      .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val installedModels: StateFlow<List<ModelPackage>> =
    modelCatalogRepository
      .observeInstalledModels()
      .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val providerOptions: StateFlow<List<ProviderType>> =
    allModels
      .map { models -> models.map(ModelPackage::providerType).distinct().sortedBy { it.name } }
      .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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
      .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val queuedDownloads: StateFlow<List<DownloadTask>> =
    modelDownloadsAndExportUseCase
      .getQueuedDownloads()
      .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val sections: StateFlow<ModelLibrarySections> =
    combine(allModels, filters, queuedDownloads) { models, filterState, downloads ->
        val filtered = models.filterBy(filterState)

        val activeTasks = downloads.filter { it.status.isActiveDownload() }
        val activeIds = activeTasks.map { it.modelId }.toSet()

        val orderedTasks =
          activeTasks.sortedWith(
            compareBy<DownloadTask> { downloadPriority(it.status) }
              .thenByDescending { it.progress }
              .thenBy { it.modelId },
          )

        val downloadItems =
          orderedTasks.map { task ->
            val associatedModel = models.firstOrNull { it.modelId == task.modelId }
            LibraryDownloadItem(task = task, model = associatedModel)
          }

        val remaining = filtered.filterNot { it.modelId in activeIds }

        ModelLibrarySections(
          downloads = downloadItems,
          attention = remaining.filter { it.installState == InstallState.ERROR },
          installed = remaining.filter { it.installState == InstallState.INSTALLED },
          available = remaining.filter { it.installState == InstallState.NOT_INSTALLED },
        )
      }
      .stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        ModelLibrarySections(),
      )

  val summary: StateFlow<ModelLibrarySummary> =
    combine(allModels, installedModels) { all, installed ->
        val attentionCount = all.count { it.installState == InstallState.ERROR }
        val availableCount = all.count { it.installState == InstallState.NOT_INSTALLED }
        ModelLibrarySummary(
          total = all.size,
          installed = installed.size,
          attention = attentionCount,
          available = availableCount,
          installedBytes = installed.sumOf(ModelPackage::sizeBytes),
        )
      }
      .stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        ModelLibrarySummary(),
      )

  val hasActiveFilters: StateFlow<Boolean> =
    filters.map { it.hasActiveFilters }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

  init {
    refreshCatalog()
  }

  fun refreshCatalog() {
    if (_isRefreshing.value) return

    val shouldShowInitialLoading = allModels.value.isEmpty()
    val startedLoading =
      if (shouldShowInitialLoading) {
        startLoadingOperation()
        true
      } else {
        false
      }

    _isRefreshing.value = true

    viewModelScope.launch {
      try {
        val result =
          runCatching { refreshModelCatalogUseCase() }.getOrElse { error -> Result.failure(error) }

        result.onFailure { error -> handleRefreshFailure(error) }
      } finally {
        _isRefreshing.value = false
        if (startedLoading) {
          stopLoadingOperation()
        }
      }
    }
  }

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
    startLoadingOperation()

    viewModelScope.launch {
      try {
        val result = modelDownloadsAndExportUseCase.downloadModel(modelId)
        result
          .onSuccess { taskId -> monitorDownloadTask(taskId, modelId) }
          .onFailure { error ->
            _errorEvents.emit(
              LibraryError.DownloadFailed(modelId, error.message ?: "Unknown error"),
            )
          }
      } catch (cancellation: CancellationException) {
        throw cancellation
      } catch (error: Throwable) {
        _errorEvents.emit(
          LibraryError.UnexpectedError(error.message ?: "Unexpected error"),
        )
      } finally {
        stopLoadingOperation()
      }
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
    startLoadingOperation()

    viewModelScope.launch {
      try {
        val result = modelDownloadsAndExportUseCase.deleteModel(modelId)
        result.onFailure { error ->
          _errorEvents.emit(
            LibraryError.DeleteFailed(modelId, error.message ?: "Unknown error"),
          )
        }
      } catch (cancellation: CancellationException) {
        throw cancellation
      } catch (error: Throwable) {
        _errorEvents.emit(
          LibraryError.UnexpectedError(error.message ?: "Unexpected error"),
        )
      } finally {
        stopLoadingOperation()
      }
    }
  }

  fun observeDownloadProgress(taskId: UUID): StateFlow<Float> =
    modelDownloadsAndExportUseCase
      .getDownloadProgress(taskId)
      .stateIn(viewModelScope, SharingStarted.Eagerly, 0f)

  private fun monitorDownloadTask(taskId: UUID, modelId: String) {
    downloadObservers[taskId]?.cancel()
    val job =
      viewModelScope.launch launch@{
        val taskFlow = modelDownloadsAndExportUseCase.observeDownloadTask(taskId)
        taskFlow.collect { task ->
          when (task?.status) {
            DownloadStatus.FAILED -> {
              _errorEvents.emit(
                LibraryError.DownloadFailed(modelId, task.errorMessage ?: "Unknown error"),
              )
              cancel()
            }
            DownloadStatus.COMPLETED,
            DownloadStatus.CANCELLED -> cancel()
            else -> Unit
          }
        }
      }
    downloadObservers[taskId] = job
    job.invokeOnCompletion { downloadObservers.remove(taskId) }
  }

  override fun onCleared() {
    super.onCleared()
    downloadObservers.values.forEach { observer -> observer.cancel() }
    downloadObservers.clear()
  }

  private suspend fun handleRefreshFailure(error: Throwable) {
    if (error is CancellationException) throw error

    val rawMessage = error.message?.takeIf { it.isNotBlank() }
    val userMessage = buildString {
      append("Failed to refresh model catalog")
      rawMessage?.let { append(": ").append(it) }
    }

    _errorEvents.emit(LibraryError.UnexpectedError(userMessage))

    val cachedCount = runCatching { modelCatalogRepository.getAllModels().size }.getOrDefault(0)
    modelCatalogRepository.recordOfflineFallback(
      reason = error::class.simpleName ?: "UnknownError",
      cachedCount = cachedCount,
      message = rawMessage,
    )
  }

  private fun startLoadingOperation() {
    val newCount = activeLoadingOperations.incrementAndGet()
    if (newCount == 1) {
      _isLoading.value = true
    }
  }

  private fun stopLoadingOperation() {
    val remaining =
      activeLoadingOperations.updateAndGet { current -> (current - 1).coerceAtLeast(0) }
    if (remaining == 0) {
      _isLoading.value = false
    }
  }

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

  private fun downloadPriority(status: DownloadStatus): Int =
    when (status) {
      DownloadStatus.DOWNLOADING -> 0
      DownloadStatus.PAUSED -> 1
      DownloadStatus.QUEUED -> 2
      DownloadStatus.FAILED -> 3
      DownloadStatus.COMPLETED -> 4
      DownloadStatus.CANCELLED -> 5
    }

  private fun DownloadStatus.isActiveDownload(): Boolean =
    this == DownloadStatus.DOWNLOADING ||
      this == DownloadStatus.PAUSED ||
      this == DownloadStatus.QUEUED ||
      this == DownloadStatus.FAILED
}

data class LibraryDownloadItem(
  val task: DownloadTask,
  val model: ModelPackage?,
)

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

  val activeFilterCount: Int
    get() {
      var count = 0
      if (provider != null) count++
      count += capabilities.size
      if (sort != ModelSort.RECOMMENDED) count++
      return count
    }
}

data class ModelLibrarySections(
  val downloads: List<LibraryDownloadItem> = emptyList(),
  val attention: List<ModelPackage> = emptyList(),
  val installed: List<ModelPackage> = emptyList(),
  val available: List<ModelPackage> = emptyList(),
)

data class ModelLibrarySummary(
  val total: Int = 0,
  val installed: Int = 0,
  val attention: Int = 0,
  val available: Int = 0,
  val installedBytes: Long = 0,
)

enum class ModelSort {
  RECOMMENDED,
  NAME,
  SIZE_DESC,
  UPDATED,
}
