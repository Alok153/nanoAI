@file:Suppress("LargeClass")

package com.vjaykrsna.nanoai.feature.library.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.data.ModelCatalogRepository
import com.vjaykrsna.nanoai.feature.library.domain.ListHuggingFaceModelsUseCase
import com.vjaykrsna.nanoai.feature.library.domain.ModelDownloadsAndExportUseCaseInterface
import com.vjaykrsna.nanoai.feature.library.domain.RefreshModelCatalogUseCase
import com.vjaykrsna.nanoai.feature.library.domain.model.HuggingFaceCatalogQuery
import com.vjaykrsna.nanoai.feature.library.domain.model.HuggingFaceModelSummary
import com.vjaykrsna.nanoai.feature.library.domain.model.HuggingFaceSortDirection
import com.vjaykrsna.nanoai.feature.library.domain.model.HuggingFaceSortField
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val HUGGING_FACE_SEARCH_DEBOUNCE_MS = 350L

// Download priority constants (lower number = higher priority)
private const val DOWNLOAD_PRIORITY_DOWNLOADING = 0
private const val DOWNLOAD_PRIORITY_PAUSED = 1
private const val DOWNLOAD_PRIORITY_QUEUED = 2
private const val DOWNLOAD_PRIORITY_FAILED = 3
private const val DOWNLOAD_PRIORITY_COMPLETED = 4
private const val DOWNLOAD_PRIORITY_CANCELLED = 5

@HiltViewModel
class ModelLibraryViewModel
@Inject
constructor(
  private val modelDownloadsAndExportUseCase: ModelDownloadsAndExportUseCaseInterface,
  private val modelCatalogRepository: ModelCatalogRepository,
  private val refreshModelCatalogUseCase: RefreshModelCatalogUseCase,
  private val listHuggingFaceModelsUseCase: ListHuggingFaceModelsUseCase,
) : ViewModel() {
  private val downloadObservers = mutableMapOf<UUID, Job>()
  private val activeLoadingOperations = AtomicInteger(0)
  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _isRefreshing = MutableStateFlow(false)
  val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

  private val _errorEvents = MutableSharedFlow<LibraryError>()
  val errorEvents = _errorEvents.asSharedFlow()

  private val _uiEvents = MutableSharedFlow<LibraryUiEvent>()
  val uiEvents = _uiEvents.asSharedFlow()

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

  private val _huggingFaceModels = MutableStateFlow<List<HuggingFaceModelSummary>>(emptyList())
  val huggingFaceModels: StateFlow<List<HuggingFaceModelSummary>> = _huggingFaceModels.asStateFlow()

  val huggingFaceFilters: StateFlow<HuggingFaceFilterState> =
    filters
      .map { it.toHuggingFaceFilterState() }
      .stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        LibraryFilterState().toHuggingFaceFilterState(),
      )

  val huggingFacePipelineOptions: StateFlow<List<String>> =
    huggingFaceModels
      .map { models ->
        models
          .mapNotNull { it.pipelineTag?.takeIf(String::isNotBlank) }
          .distinctBy { it.lowercase(Locale.US) }
          .sortedBy { it.lowercase(Locale.US) }
      }
      .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val pipelineOptions: StateFlow<List<String>> = huggingFacePipelineOptions

  val huggingFaceLibraryOptions: StateFlow<List<String>> =
    huggingFaceModels
      .map { models ->
        models
          .mapNotNull { it.libraryName?.takeIf(String::isNotBlank) }
          .distinctBy { it.lowercase(Locale.US) }
          .sortedBy { it.lowercase(Locale.US) }
      }
      .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  private val _isHuggingFaceLoading = MutableStateFlow(false)
  val isHuggingFaceLoading: StateFlow<Boolean> = _isHuggingFaceLoading.asStateFlow()

  private var huggingFaceInitialized = false
  private var lastHuggingFaceFilters: HuggingFaceFilterState? = null

  private val downloadTasks: StateFlow<List<DownloadTask>> =
    modelDownloadsAndExportUseCase
      .observeDownloadTasks()
      .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  private val tabSections: StateFlow<ModelLibraryTabSections> =
    combine(allModels, filters, downloadTasks) { models, filterState, downloads ->
        val filtered = models.filterBy(filterState)

        val prioritizedDownloads =
          downloads
            .filter { it.status.isActiveDownload() }
            .sortedWith(
              compareBy<DownloadTask> { downloadPriority(it.status) }
                .thenByDescending { it.progress }
                .thenBy { it.modelId },
            )
        val downloadItems =
          prioritizedDownloads.map { task ->
            val associatedModel = models.firstOrNull { it.modelId == task.modelId }
            LibraryDownloadItem(task = task, model = associatedModel)
          }

        val activeIds = prioritizedDownloads.map { it.modelId }.toSet()

        val localModels =
          filtered.filter { model ->
            model.installState == InstallState.INSTALLED || model.installState == InstallState.ERROR
          }

        val curatedAvailable =
          filtered
            .filter { it.installState == InstallState.NOT_INSTALLED }
            .filterNot { it.modelId in activeIds }
        val curatedInstalled = filtered.filter { it.installState == InstallState.INSTALLED }
        val curatedAttention = filtered.filter { it.installState == InstallState.ERROR }

        ModelLibraryTabSections(
          local =
            ModelLibrarySections(
              downloads = downloadItems,
              attention = localModels.filter { it.installState == InstallState.ERROR },
              installed = localModels.filter { it.installState == InstallState.INSTALLED },
              available = emptyList(),
            ),
          curated =
            ModelLibrarySections(
              downloads = downloadItems,
              attention = curatedAttention,
              installed = curatedInstalled,
              available = curatedAvailable,
            ),
        )
      }
      .stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        ModelLibraryTabSections(),
      )

  val localSections: StateFlow<ModelLibrarySections> =
    tabSections
      .map { it.local }
      .stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        ModelLibrarySections(),
      )

  val curatedSections: StateFlow<ModelLibrarySections> =
    tabSections
      .map { it.curated }
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
    observeHuggingFaceFilters()
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

  private fun observeHuggingFaceFilters() {
    viewModelScope.launch {
      combine(filters, huggingFaceFilters) { state, hf -> state.tab to hf }
        .distinctUntilChanged()
        .debounce(HUGGING_FACE_SEARCH_DEBOUNCE_MS)
        .collect { (tab, hfFilters) ->
          if (tab == ModelLibraryTab.HUGGING_FACE) {
            fetchHuggingFaceModels(hfFilters)
          }
        }
    }
  }

  private fun fetchHuggingFaceModels(filters: HuggingFaceFilterState, force: Boolean = false) {
    if (!force && !shouldFetchHuggingFace(filters)) return

    huggingFaceInitialized = true
    lastHuggingFaceFilters = filters

    viewModelScope.launch {
      _isHuggingFaceLoading.value = true
      val query = filters.toQuery()
      val result = listHuggingFaceModelsUseCase(query)
      result
        .onSuccess { models ->
          _huggingFaceModels.value =
            models.map { model -> model.copy(tags = applyTagVisibilityRules(model.tags)) }
        }
        .onFailure { error ->
          _errorEvents.emit(
            LibraryError.HuggingFaceLoadFailed(error.message ?: "Failed to load Hugging Face")
          )
        }
      _isHuggingFaceLoading.value = false
    }
  }

  private fun shouldFetchHuggingFace(filters: HuggingFaceFilterState): Boolean {
    if (!huggingFaceInitialized) return true
    return lastHuggingFaceFilters != filters
  }

  fun updateSearchQuery(query: String) {
    _filters.update { state ->
      when (state.tab) {
        ModelLibraryTab.HUGGING_FACE -> state.copy(huggingFaceSearchQuery = query)
        else -> state.copy(localSearchQuery = query)
      }
    }
  }

  fun setPipeline(pipelineTag: String?) {
    _filters.update { it.copy(pipelineTag = pipelineTag) }
  }

  fun setLocalSort(sort: ModelSort) {
    _filters.update { it.copy(localSort = sort) }
  }

  fun setHuggingFaceSort(sort: HuggingFaceSortOption) {
    _filters.update { it.copy(huggingFaceSort = sort) }
  }

  fun selectLocalLibrary(providerType: ProviderType?) {
    _filters.update { it.copy(localLibrary = providerType) }
  }

  fun setHuggingFaceLibrary(library: String?) {
    _filters.update { it.copy(huggingFaceLibrary = library) }
  }

  fun clearFilters() {
    _filters.update { state ->
      when (state.tab) {
        ModelLibraryTab.HUGGING_FACE ->
          state.copy(
            huggingFaceSearchQuery = "",
            pipelineTag = null,
            huggingFaceSort = HuggingFaceSortOption.TRENDING,
            huggingFaceLibrary = null,
          )
        else ->
          state.copy(
            localSearchQuery = "",
            pipelineTag = null,
            localSort = ModelSort.RECOMMENDED,
            localLibrary = null,
          )
      }
    }
  }

  fun selectTab(tab: ModelLibraryTab) {
    _filters.update { state ->
      if (state.tab == tab) {
        state
      } else {
        state.copy(tab = tab)
      }
    }
  }

  fun requestLocalModelImport() {
    viewModelScope.launch { _uiEvents.emit(LibraryUiEvent.RequestLocalModelImport) }
  }

  @Suppress("UnusedParameter")
  fun importLocalModel(uri: Uri) {
    viewModelScope.launch {
      _errorEvents.emit(
        LibraryError.UnexpectedError(
          "Manual import isn't available yet. Check curated or Hugging Face tabs for downloads.",
        )
      )
    }
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
    val query = filters.localSearchQuery.trim()
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

    filters.localLibrary?.let { provider ->
      current = current.filter { it.providerType == provider }
    }

    filters.pipelineTag?.let { pipeline ->
      val normalized = pipeline.lowercase(Locale.US)
      current =
        current.filter { model ->
          model.capabilities.any { capability -> capability.lowercase(Locale.US) == normalized }
        }
    }

    return current.sortBy(filters.localSort)
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
      DownloadStatus.DOWNLOADING -> DOWNLOAD_PRIORITY_DOWNLOADING
      DownloadStatus.PAUSED -> DOWNLOAD_PRIORITY_PAUSED
      DownloadStatus.QUEUED -> DOWNLOAD_PRIORITY_QUEUED
      DownloadStatus.FAILED -> DOWNLOAD_PRIORITY_FAILED
      DownloadStatus.COMPLETED -> DOWNLOAD_PRIORITY_COMPLETED
      DownloadStatus.CANCELLED -> DOWNLOAD_PRIORITY_CANCELLED
    }

  private fun DownloadStatus.isActiveDownload(): Boolean =
    this == DownloadStatus.DOWNLOADING ||
      this == DownloadStatus.PAUSED ||
      this == DownloadStatus.QUEUED ||
      this == DownloadStatus.FAILED

  private fun applyTagVisibilityRules(rawTags: Collection<String>): List<String> {
    if (rawTags.isEmpty()) return emptyList()
    val normalized = rawTags.map { it.trim() }.filter { it.isNotEmpty() }
    val hasMultimodal = normalized.any { it.equals("multimodal", ignoreCase = true) }
    return normalized.filterNot { hasMultimodal && it.equals("text-generation", ignoreCase = true) }
  }
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

  data class HuggingFaceLoadFailed(
    val message: String,
  ) : LibraryError()
}

sealed class LibraryUiEvent {
  data object RequestLocalModelImport : LibraryUiEvent()
}

data class LibraryFilterState(
  val tab: ModelLibraryTab = ModelLibraryTab.LOCAL,
  val localSearchQuery: String = "",
  val huggingFaceSearchQuery: String = "",
  val pipelineTag: String? = null,
  val localSort: ModelSort = ModelSort.RECOMMENDED,
  val localLibrary: ProviderType? = null,
  val huggingFaceSort: HuggingFaceSortOption = HuggingFaceSortOption.TRENDING,
  val huggingFaceLibrary: String? = null,
) {
  fun currentSearchQuery(): String =
    when (tab) {
      ModelLibraryTab.HUGGING_FACE -> huggingFaceSearchQuery
      else -> localSearchQuery
    }

  fun hasActiveFiltersFor(targetTab: ModelLibraryTab = tab): Boolean =
    when (targetTab) {
      ModelLibraryTab.HUGGING_FACE ->
        huggingFaceSearchQuery.isNotBlank() ||
          pipelineTag != null ||
          huggingFaceLibrary != null ||
          huggingFaceSort != HuggingFaceSortOption.TRENDING
      else ->
        localSearchQuery.isNotBlank() ||
          pipelineTag != null ||
          localLibrary != null ||
          localSort != ModelSort.RECOMMENDED
    }

  fun activeFilterCountFor(targetTab: ModelLibraryTab = tab): Int {
    var count = 0
    if (pipelineTag != null) count++
    when (targetTab) {
      ModelLibraryTab.HUGGING_FACE -> {
        if (huggingFaceLibrary != null) count++
        if (huggingFaceSort != HuggingFaceSortOption.TRENDING) count++
      }
      else -> {
        if (localLibrary != null) count++
        if (localSort != ModelSort.RECOMMENDED) count++
      }
    }
    return count
  }

  val hasActiveFilters: Boolean
    get() = hasActiveFiltersFor(tab)

  val activeFilterCount: Int
    get() = activeFilterCountFor(tab)
}

internal fun LibraryFilterState.toHuggingFaceFilterState(): HuggingFaceFilterState =
  HuggingFaceFilterState(
    searchQuery = huggingFaceSearchQuery,
    sort = huggingFaceSort,
    pipelineTag = pipelineTag,
    library = huggingFaceLibrary,
    includePrivate = false,
  )

data class ModelLibrarySections(
  val downloads: List<LibraryDownloadItem> = emptyList(),
  val attention: List<ModelPackage> = emptyList(),
  val installed: List<ModelPackage> = emptyList(),
  val available: List<ModelPackage> = emptyList(),
)

data class ModelLibraryTabSections(
  val local: ModelLibrarySections = ModelLibrarySections(),
  val curated: ModelLibrarySections = ModelLibrarySections(),
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

enum class HuggingFaceSortOption(
  val sortField: HuggingFaceSortField?,
  val direction: HuggingFaceSortDirection?,
) {
  TRENDING(null, null),
  MOST_DOWNLOADED(HuggingFaceSortField.DOWNLOADS, HuggingFaceSortDirection.DESCENDING),
  MOST_LIKED(HuggingFaceSortField.LIKES, HuggingFaceSortDirection.DESCENDING),
  RECENTLY_UPDATED(HuggingFaceSortField.LAST_MODIFIED, HuggingFaceSortDirection.DESCENDING),
  NEWEST(HuggingFaceSortField.CREATED, HuggingFaceSortDirection.DESCENDING);

  fun label(): String =
    when (this) {
      TRENDING -> "Trending"
      MOST_DOWNLOADED -> "Most downloaded"
      MOST_LIKED -> "Most liked"
      RECENTLY_UPDATED -> "Recently updated"
      NEWEST -> "Newest"
    }
}

data class HuggingFaceFilterState(
  val searchQuery: String = "",
  val sort: HuggingFaceSortOption = HuggingFaceSortOption.TRENDING,
  val pipelineTag: String? = null,
  val library: String? = null,
  val includePrivate: Boolean = false,
) {
  val hasActiveFilters: Boolean
    get() =
      sort != HuggingFaceSortOption.TRENDING ||
        pipelineTag != null ||
        library != null ||
        includePrivate
}

private fun HuggingFaceFilterState.toQuery(): HuggingFaceCatalogQuery {
  val normalizedSearch = searchQuery.trim().takeIf { it.isNotEmpty() }
  val sortField = sort.sortField
  val sortDirection = sort.direction?.takeIf { sortField != null }
  return HuggingFaceCatalogQuery(
    search = normalizedSearch,
    sortField = sortField,
    sortDirection = sortDirection,
    pipelineTag = pipelineTag,
    library = library,
    includePrivate = includePrivate,
  )
}
