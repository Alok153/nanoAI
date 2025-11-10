package com.vjaykrsna.nanoai.feature.library.presentation

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.core.common.fold
import com.vjaykrsna.nanoai.core.common.onFailure
import com.vjaykrsna.nanoai.core.domain.library.DownloadModelUseCase
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceCatalogUseCase
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceModelCompatibilityChecker
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceModelSummary
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceToModelPackageConverter
import com.vjaykrsna.nanoai.core.domain.library.ModelCatalogUseCase
import com.vjaykrsna.nanoai.core.domain.library.RefreshModelCatalogUseCase
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import com.vjaykrsna.nanoai.feature.library.presentation.model.HuggingFaceSortOption
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryError
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryFilterState
import com.vjaykrsna.nanoai.feature.library.presentation.model.ModelLibraryUiEvent
import com.vjaykrsna.nanoai.feature.library.presentation.model.ModelSort
import com.vjaykrsna.nanoai.feature.library.presentation.state.ModelLibraryUiState
import com.vjaykrsna.nanoai.shared.state.ViewModelStateHost
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class ModelLibraryViewModel
@Inject
constructor(
  private val modelCatalogUseCase: ModelCatalogUseCase,
  private val refreshModelCatalogUseCase: RefreshModelCatalogUseCase,
  private val downloadManager: DownloadManager,
  private val downloadModelUseCase: DownloadModelUseCase,
  private val hfToModelConverter: HuggingFaceToModelPackageConverter,
  private val huggingFaceCatalogUseCase: HuggingFaceCatalogUseCase,
  private val compatibilityChecker: HuggingFaceModelCompatibilityChecker,
  @MainImmediateDispatcher private val mainDispatcher: CoroutineDispatcher,
) :
  ViewModelStateHost<ModelLibraryUiState, ModelLibraryUiEvent>(
    initialState = ModelLibraryUiState(),
    dispatcher = mainDispatcher,
  ) {

  private val refreshing = AtomicBoolean(false)
  private val huggingFaceLibraryViewModel =
    HuggingFaceLibraryViewModel(huggingFaceCatalogUseCase, compatibilityChecker)

  private val downloadActions =
    ModelDownloadActionHandler(
      downloadModelUseCase = downloadModelUseCase,
      downloadManager = downloadManager,
      dispatcher = mainDispatcher,
      scope = viewModelScope,
      emitError = { error -> emitError(error) },
    )

  private val huggingFaceCoordinator =
    HuggingFaceDownloadCoordinator(
      converter = hfToModelConverter,
      modelCatalogUseCase = modelCatalogUseCase,
      downloadModelUseCase = downloadModelUseCase,
      emitError = { error -> emitError(error) },
    )

  init {
    observeModelCatalog()
    observeDownloadLoading()
    observeDownloadErrors()
    observeHuggingFaceState()
    observeHuggingFaceDownloads()
    refreshCatalog()
  }

  fun refreshCatalog() {
    if (!refreshing.compareAndSet(false, true)) return
    updateState { copy(isRefreshing = true, isLoading = true) }

    viewModelScope.launch(mainDispatcher) {
      try {
        refreshModelCatalogUseCase().onFailure { error ->
          handleRefreshFailure(error.cause ?: Exception(error.message), modelCatalogUseCase) {
            emitError(it)
          }
        }
      } finally {
        refreshing.set(false)
        updateState { copy(isRefreshing = false, isLoading = downloadManager.isLoading.value) }
      }
    }
  }

  fun updateSearchQuery(query: String) {
    val currentFilters = state.value.filters
    when (currentFilters.tab) {
      ModelLibraryTab.HUGGING_FACE -> {
        huggingFaceLibraryViewModel.updateSearchQuery(query)
        updateFilters { it.copy(huggingFaceSearchQuery = query) }
      }
      else -> updateFilters { it.copy(localSearchQuery = query) }
    }
  }

  fun setPipeline(pipelineTag: String?) {
    huggingFaceLibraryViewModel.setPipeline(pipelineTag)
    updateFilters { it.copy(pipelineTag = pipelineTag) }
  }

  fun setLocalSort(sort: ModelSort) {
    updateFilters { it.copy(localSort = sort) }
  }

  fun setHuggingFaceSort(sort: HuggingFaceSortOption) {
    huggingFaceLibraryViewModel.setSort(sort)
    updateFilters { it.copy(huggingFaceSort = sort) }
  }

  fun selectLocalLibrary(providerType: ProviderType?) {
    updateFilters { it.copy(localLibrary = providerType) }
  }

  fun toggleCapability(capability: String) {
    updateFilters { state ->
      val updated =
        if (state.selectedCapabilities.contains(capability)) {
          state.selectedCapabilities - capability
        } else {
          state.selectedCapabilities + capability
        }
      state.copy(selectedCapabilities = updated)
    }
  }

  fun clearSelectedCapabilities() {
    updateFilters { it.copy(selectedCapabilities = emptySet()) }
  }

  fun setHuggingFaceLibrary(library: String?) {
    huggingFaceLibraryViewModel.setLibrary(library)
    updateFilters { it.copy(huggingFaceLibrary = library) }
  }

  fun clearFilters() {
    val currentFilters = state.value.filters
    val cleared =
      when (currentFilters.tab) {
        ModelLibraryTab.HUGGING_FACE -> {
          huggingFaceLibraryViewModel.clearFilters()
          currentFilters.clearHuggingFaceFilters()
        }
        else -> currentFilters.clearLocalFilters()
      }
    updateFilters { cleared }
  }

  fun selectTab(tab: ModelLibraryTab) {
    val current = state.value.filters
    if (current.tab == tab) return
    val updated = current.copy(tab = tab)
    if (tab == ModelLibraryTab.HUGGING_FACE) {
      huggingFaceLibraryViewModel.updateSearchQuery(updated.huggingFaceSearchQuery)
    }
    updateFilters { updated }
  }

  fun requestLocalModelImport() {
    viewModelScope.launch(mainDispatcher) { emitEvent(ModelLibraryUiEvent.RequestLocalModelImport) }
  }

  fun importLocalModel(@Suppress("UnusedParameter") uri: Uri) {
    viewModelScope.launch(mainDispatcher) {
      emitError(
        LibraryError.UnexpectedError(
          "Manual import isn't available yet. Check curated or Hugging Face tabs for downloads."
        )
      )
    }
  }

  fun downloadModel(modelId: String) {
    downloadActions.downloadModel(modelId)
  }

  fun downloadHuggingFaceModel(model: HuggingFaceModelSummary) {
    huggingFaceLibraryViewModel.requestDownload(model)
  }

  fun pauseDownload(taskId: UUID) {
    downloadActions.pauseDownload(taskId)
  }

  fun resumeDownload(taskId: UUID) {
    downloadActions.resumeDownload(taskId)
  }

  fun cancelDownload(taskId: UUID) {
    downloadActions.cancelDownload(taskId)
  }

  fun retryDownload(taskId: UUID) {
    downloadActions.retryDownload(taskId)
  }

  fun deleteModel(modelId: String) {
    downloadActions.deleteModel(modelId)
  }

  fun observeDownloadProgress(taskId: UUID) = downloadActions.observeDownloadProgress(taskId)

  private fun observeModelCatalog() {
    val filtersFlow = state.map { it.filters }.distinctUntilChanged()
    viewModelScope.launch(mainDispatcher) {
      combine(
          modelCatalogUseCase.observeAllModels(),
          modelCatalogUseCase.observeInstalledModels(),
          downloadManager.observeDownloadTasks(),
          filtersFlow,
        ) { allModels, installedModels, downloadTasks, filters ->
          deriveModelLibraryContent(allModels, installedModels, downloadTasks, filters)
        }
        .collect { derived ->
          updateState {
            copy(
              summary = derived.summary,
              localSections = derived.localSections,
              curatedSections = derived.curatedSections,
              providerOptions = derived.providerOptions,
              capabilityOptions = derived.capabilityOptions,
            )
          }
        }
    }
  }

  private fun observeDownloadLoading() {
    viewModelScope.launch(mainDispatcher) {
      downloadManager.isLoading.collect { isDownloading ->
        updateState { copy(isLoading = isDownloading || isRefreshing) }
      }
    }
  }

  private fun observeDownloadErrors() {
    viewModelScope.launch(mainDispatcher) {
      downloadManager.errorEvents.collect { error -> emitError(error) }
    }
  }

  private fun observeHuggingFaceState() {
    viewModelScope.launch(mainDispatcher) {
      huggingFaceLibraryViewModel.models.collect { models ->
        updateState { copy(huggingFaceModels = models) }
      }
    }
    viewModelScope.launch(mainDispatcher) {
      huggingFaceLibraryViewModel.filters.collect { filters ->
        updateState { copy(huggingFaceFilterState = filters) }
      }
    }
    viewModelScope.launch(mainDispatcher) {
      huggingFaceLibraryViewModel.pipelineOptions.collect { options ->
        updateState { copy(pipelineOptions = options) }
      }
    }
    viewModelScope.launch(mainDispatcher) {
      huggingFaceLibraryViewModel.libraryOptions.collect { options ->
        updateState { copy(huggingFaceLibraryOptions = options) }
      }
    }
    viewModelScope.launch(mainDispatcher) {
      huggingFaceLibraryViewModel.downloadableModelIds.collect { ids ->
        updateState { copy(huggingFaceDownloadableModelIds = ids) }
      }
    }
    viewModelScope.launch(mainDispatcher) {
      huggingFaceLibraryViewModel.isLoading.collect { loading ->
        updateState { copy(isHuggingFaceLoading = loading) }
      }
    }
    viewModelScope.launch(mainDispatcher) {
      huggingFaceLibraryViewModel.errorEvents.collect { error -> emitError(error) }
    }
  }

  private fun observeHuggingFaceDownloads() {
    viewModelScope.launch(mainDispatcher) {
      huggingFaceLibraryViewModel.downloadRequests.collect { model ->
        huggingFaceCoordinator.process(model)
      }
    }
  }

  private suspend fun emitError(error: LibraryError) {
    emitEvent(ModelLibraryUiEvent.ErrorRaised(error))
  }

  private fun updateFilters(transform: (LibraryFilterState) -> LibraryFilterState) {
    updateState { copy(filters = transform(filters)) }
  }

  private fun LibraryFilterState.clearHuggingFaceFilters(): LibraryFilterState =
    copy(
      huggingFaceSearchQuery = "",
      pipelineTag = null,
      huggingFaceSort = HuggingFaceSortOption.TRENDING,
      huggingFaceLibrary = null,
    )

  private fun LibraryFilterState.clearLocalFilters(): LibraryFilterState =
    copy(
      localSearchQuery = "",
      pipelineTag = null,
      localSort = ModelSort.RECOMMENDED,
      localLibrary = null,
      selectedCapabilities = emptySet(),
    )
}

private suspend fun handleRefreshFailure(
  error: Throwable,
  modelCatalogUseCase: ModelCatalogUseCase,
  emitError: suspend (LibraryError) -> Unit,
) {
  if (error is CancellationException) throw error
  val rawMessage = error.message?.takeIf { it.isNotBlank() }
  val userMessage = buildString {
    append("Failed to refresh model catalog")
    rawMessage?.let { append(": ").append(it) }
  }

  emitError(LibraryError.UnexpectedError(userMessage))

  val cachedCount =
    modelCatalogUseCase.getAllModels().fold(onSuccess = { it.size }, onFailure = { 0 })
  modelCatalogUseCase.recordOfflineFallback(
    reason = error::class.simpleName ?: "UnknownError",
    cachedCount = cachedCount,
    message = rawMessage,
  )
}
