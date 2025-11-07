package com.vjaykrsna.nanoai.feature.library.presentation

import com.vjaykrsna.nanoai.core.domain.library.ModelCatalogUseCase
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.library.DownloadStatus
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import com.vjaykrsna.nanoai.feature.library.presentation.model.HuggingFaceSortOption
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryDownloadItem
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryFilterState
import com.vjaykrsna.nanoai.feature.library.presentation.model.ModelLibrarySections
import com.vjaykrsna.nanoai.feature.library.presentation.model.ModelLibrarySummary
import com.vjaykrsna.nanoai.feature.library.presentation.model.ModelLibraryTabSections
import com.vjaykrsna.nanoai.feature.library.presentation.model.ModelSort
import com.vjaykrsna.nanoai.feature.library.presentation.util.filterBy
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

internal class ModelLibraryStateStore
@Inject
constructor(
  private val modelCatalogUseCase: ModelCatalogUseCase,
  private val downloadManager: DownloadManager,
  private val huggingFaceLibraryViewModel: HuggingFaceLibraryViewModel,
  private val scope: CoroutineScope,
) {
  private val _filters = MutableStateFlow(LibraryFilterState())
  val filters: StateFlow<LibraryFilterState> = _filters.asStateFlow()

  val filtersController =
    LibraryFiltersController(
      filters = _filters,
      huggingFaceLibraryViewModel = huggingFaceLibraryViewModel,
    )

  private val _isRefreshing = MutableStateFlow(false)
  val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

  val isLoading: StateFlow<Boolean> =
    combine(isRefreshing, downloadManager.isLoading) { refreshing, downloading ->
        refreshing || downloading
      }
      .stateIn(scope, SharingStarted.Eagerly, false)

  val allModels: StateFlow<List<ModelPackage>> =
    modelCatalogUseCase.observeAllModels().stateIn(scope, SharingStarted.Eagerly, emptyList())

  val installedModels: StateFlow<List<ModelPackage>> =
    modelCatalogUseCase.observeInstalledModels().stateIn(scope, SharingStarted.Eagerly, emptyList())

  val providerOptions: StateFlow<List<ProviderType>> =
    allModels
      .map { models -> models.map(ModelPackage::providerType).distinct().sortedBy { it.name } }
      .stateIn(scope, SharingStarted.Eagerly, emptyList())

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
      .stateIn(scope, SharingStarted.Eagerly, emptyList())

  val huggingFaceModels = huggingFaceLibraryViewModel.models
  val huggingFaceFilters = huggingFaceLibraryViewModel.filters
  val huggingFacePipelineOptions = huggingFaceLibraryViewModel.pipelineOptions
  val huggingFaceLibraryOptions = huggingFaceLibraryViewModel.libraryOptions
  val isHuggingFaceLoading = huggingFaceLibraryViewModel.isLoading
  val huggingFaceDownloadableModelIds = huggingFaceLibraryViewModel.downloadableModelIds
  val pipelineOptions = huggingFaceLibraryViewModel.pipelineOptions

  private val downloadTasks = downloadManager.observeDownloadTasks()

  private val tabSections: StateFlow<ModelLibraryTabSections> =
    combine(allModels, filters, downloadTasks) { models, filterState, downloads ->
        val filtered = models.filterBy(filterState)

        val prioritizedDownloads =
          downloads
            .filter { it.status.isActiveDownload() }
            .sortedWith(
              compareBy<com.vjaykrsna.nanoai.core.domain.model.DownloadTask> {
                  downloadPriority(it.status)
                }
                .thenByDescending { it.progress }
                .thenBy { it.modelId }
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
      .stateIn(scope, SharingStarted.Eagerly, ModelLibraryTabSections())

  val localSections: StateFlow<ModelLibrarySections> =
    tabSections.map { it.local }.stateIn(scope, SharingStarted.Eagerly, ModelLibrarySections())

  val curatedSections: StateFlow<ModelLibrarySections> =
    tabSections.map { it.curated }.stateIn(scope, SharingStarted.Eagerly, ModelLibrarySections())

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
      .stateIn(scope, SharingStarted.Eagerly, ModelLibrarySummary())

  val hasActiveFilters: StateFlow<Boolean> =
    filters.map { it.hasActiveFilters }.stateIn(scope, SharingStarted.Eagerly, false)

  fun beginRefresh(): Boolean {
    if (_isRefreshing.value) return false
    _isRefreshing.value = true
    return true
  }

  fun completeRefresh() {
    _isRefreshing.value = false
  }
}

internal class LibraryFiltersController(
  private val filters: MutableStateFlow<LibraryFilterState>,
  private val huggingFaceLibraryViewModel: HuggingFaceLibraryViewModel,
) {
  fun updateSearchQuery(query: String) {
    filters.update { state ->
      when (state.tab) {
        ModelLibraryTab.HUGGING_FACE -> {
          huggingFaceLibraryViewModel.updateSearchQuery(query)
          state.copy(huggingFaceSearchQuery = query)
        }
        else -> state.copy(localSearchQuery = query)
      }
    }
  }

  fun setPipeline(pipelineTag: String?) {
    filters.update { it.copy(pipelineTag = pipelineTag) }
    huggingFaceLibraryViewModel.setPipeline(pipelineTag)
  }

  fun setLocalSort(sort: ModelSort) {
    filters.update { it.copy(localSort = sort) }
  }

  fun setHuggingFaceSort(sort: HuggingFaceSortOption) {
    filters.update { it.copy(huggingFaceSort = sort) }
    huggingFaceLibraryViewModel.setSort(sort)
  }

  fun selectLocalLibrary(providerType: ProviderType?) {
    filters.update { it.copy(localLibrary = providerType) }
  }

  fun toggleCapability(capability: String) {
    filters.update { state ->
      val current = state.selectedCapabilities
      val updated = if (current.contains(capability)) current - capability else current + capability
      state.copy(selectedCapabilities = updated)
    }
  }

  fun clearSelectedCapabilities() {
    filters.update { it.copy(selectedCapabilities = emptySet()) }
  }

  fun setHuggingFaceLibrary(library: String?) {
    filters.update { it.copy(huggingFaceLibrary = library) }
    huggingFaceLibraryViewModel.setLibrary(library)
  }

  fun clearFilters() {
    filters.update { state ->
      when (state.tab) {
        ModelLibraryTab.HUGGING_FACE -> state.clearHuggingFaceFilters(huggingFaceLibraryViewModel)
        else -> state.clearLocalFilters()
      }
    }
  }

  fun selectTab(tab: ModelLibraryTab) {
    filters.update { state ->
      if (state.tab == tab) {
        state
      } else {
        val newState = state.copy(tab = tab)
        if (tab == ModelLibraryTab.HUGGING_FACE) {
          huggingFaceLibraryViewModel.updateSearchQuery(newState.huggingFaceSearchQuery)
        }
        newState
      }
    }
  }
}

private fun LibraryFilterState.clearHuggingFaceFilters(
  huggingFaceLibraryViewModel: HuggingFaceLibraryViewModel
): LibraryFilterState {
  huggingFaceLibraryViewModel.clearFilters()
  return copy(
    huggingFaceSearchQuery = "",
    pipelineTag = null,
    huggingFaceSort = HuggingFaceSortOption.TRENDING,
    huggingFaceLibrary = null,
  )
}

private fun LibraryFilterState.clearLocalFilters(): LibraryFilterState =
  copy(
    localSearchQuery = "",
    pipelineTag = null,
    localSort = ModelSort.RECOMMENDED,
    localLibrary = null,
    selectedCapabilities = emptySet(),
  )

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
