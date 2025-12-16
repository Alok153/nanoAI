package com.vjaykrsna.nanoai.feature.library.presentation

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.core.common.error.NanoAIErrorEnvelope
import com.vjaykrsna.nanoai.core.common.error.withFallbackMessage
import com.vjaykrsna.nanoai.core.common.fold
import com.vjaykrsna.nanoai.core.common.onFailure
import com.vjaykrsna.nanoai.core.device.ConnectivityObserver
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceCatalogUseCase
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceModelCompatibilityChecker
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceModelSummary
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceToModelPackageConverter
import com.vjaykrsna.nanoai.core.domain.library.ModelCatalogUseCase
import com.vjaykrsna.nanoai.core.domain.library.RefreshModelCatalogUseCase
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityStatus
import com.vjaykrsna.nanoai.feature.library.domain.QueueModelDownloadUseCase
import com.vjaykrsna.nanoai.feature.library.presentation.model.HuggingFaceLibraryUiEvent
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
  private val downloadCoordinator: DownloadUiCoordinator,
  private val queueModelDownloadUseCase: QueueModelDownloadUseCase,
  private val hfToModelConverter: HuggingFaceToModelPackageConverter,
  private val huggingFaceCatalogUseCase: HuggingFaceCatalogUseCase,
  private val compatibilityChecker: HuggingFaceModelCompatibilityChecker,
  private val connectivityObserver: ConnectivityObserver,
  @MainImmediateDispatcher private val mainDispatcher: CoroutineDispatcher,
) :
  ViewModelStateHost<ModelLibraryUiState, ModelLibraryUiEvent>(
    initialState = ModelLibraryUiState(),
    dispatcher = mainDispatcher,
  ) {

  private val refreshing = AtomicBoolean(false)
  private val huggingFaceLibraryViewModel =
    HuggingFaceLibraryViewModel(
      huggingFaceCatalogUseCase = huggingFaceCatalogUseCase,
      compatibilityChecker = compatibilityChecker,
      mainDispatcher = mainDispatcher,
    )

  private val downloadActions =
    ModelDownloadActionHandler(
      downloadCoordinator = downloadCoordinator,
      dispatcher = mainDispatcher,
      scope = viewModelScope,
    )

  private val huggingFaceCoordinator =
    HuggingFaceDownloadCoordinator(
      converter = hfToModelConverter,
      modelCatalogUseCase = modelCatalogUseCase,
      queueModelDownloadUseCase = queueModelDownloadUseCase,
      emitError = { error -> emitError(error) },
    )

  init {
    observeModelCatalog()
    observeDownloadLoading()
    observeDownloadErrors()
    observeHuggingFaceState()
    observeConnectivity()
    refreshCatalog()
  }

  fun refreshCatalog() {
    if (!refreshing.compareAndSet(false, true)) return
    updateState { copy(isRefreshing = true, isLoading = true, lastErrorMessage = null) }

    viewModelScope.launch(mainDispatcher) {
      try {
        refreshModelCatalogUseCase().onFailure { error ->
          handleRefreshFailure(error.cause ?: Exception(error.message), modelCatalogUseCase) {
            emitError(it)
          }
        }
      } finally {
        refreshing.set(false)
        updateState { copy(isRefreshing = false, isLoading = downloadCoordinator.isLoading.value) }
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
    if (state.value.connectivityStatus == ConnectivityStatus.OFFLINE) {
      viewModelScope.launch(mainDispatcher) {
        emitError(LibraryError.UnexpectedError("You're offline. Connect to download models."))
      }
      return
    }
    downloadActions.downloadModel(modelId)
  }

  fun downloadHuggingFaceModel(model: HuggingFaceModelSummary) {
    if (state.value.connectivityStatus == ConnectivityStatus.OFFLINE) {
      viewModelScope.launch(mainDispatcher) {
        emitError(LibraryError.UnexpectedError("You're offline. Connect to download models."))
      }
      return
    }
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
          downloadCoordinator.observeDownloadTasks(),
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
      downloadCoordinator.isLoading.collect { isDownloading ->
        updateState { copy(isLoading = isDownloading || isRefreshing) }
      }
    }
  }

  private fun observeDownloadErrors() {
    viewModelScope.launch(mainDispatcher) {
      downloadCoordinator.errorEvents.collect { error -> emitError(error) }
    }
  }

  private fun observeConnectivity() {
    viewModelScope.launch(mainDispatcher) {
      connectivityObserver.status.collect { status ->
        updateState { copy(connectivityStatus = status) }
      }
    }
  }

  private fun observeHuggingFaceState() {
    viewModelScope.launch(mainDispatcher) {
      huggingFaceLibraryViewModel.state.collect { hfState ->
        updateState {
          copy(
            huggingFaceModels = hfState.models,
            huggingFaceFilterState = hfState.filters,
            pipelineOptions = hfState.pipelineOptions,
            huggingFaceLibraryOptions = hfState.libraryOptions,
            huggingFaceDownloadableModelIds = hfState.downloadableModelIds,
            isHuggingFaceLoading = hfState.isLoading,
          )
        }
      }
    }
    viewModelScope.launch(mainDispatcher) {
      huggingFaceLibraryViewModel.events.collect { event ->
        when (event) {
          is HuggingFaceLibraryUiEvent.DownloadRequested ->
            huggingFaceCoordinator.process(event.model)
          is HuggingFaceLibraryUiEvent.ErrorRaised -> emitError(event.error, event.envelope)
        }
      }
    }
  }

  private suspend fun emitError(
    error: LibraryError,
    envelopeOverride: NanoAIErrorEnvelope? = null,
  ) {
    val envelope = envelopeOverride ?: error.toEnvelope()
    updateState { copy(lastErrorMessage = envelope.userMessage) }
    emitEvent(ModelLibraryUiEvent.ErrorRaised(error, envelope))
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

private fun LibraryError.toEnvelope(): NanoAIErrorEnvelope {
  val (fallback, context, resolvedMessage) =
    when (this) {
      is LibraryError.DownloadFailed ->
        Triple(
          DOWNLOAD_FAILURE_MESSAGE,
          mapOf("operation" to "download", "modelId" to modelId),
          message,
        )
      is LibraryError.PauseFailed ->
        Triple(PAUSE_FAILURE_MESSAGE, mapOf("operation" to "pause", "taskId" to taskId), message)
      is LibraryError.ResumeFailed ->
        Triple(RESUME_FAILURE_MESSAGE, mapOf("operation" to "resume", "taskId" to taskId), message)
      is LibraryError.CancelFailed ->
        Triple(CANCEL_FAILURE_MESSAGE, mapOf("operation" to "cancel", "taskId" to taskId), message)
      is LibraryError.RetryFailed ->
        Triple(RETRY_FAILURE_MESSAGE, mapOf("operation" to "retry", "taskId" to taskId), message)
      is LibraryError.DeleteFailed ->
        Triple(
          DELETE_FAILURE_MESSAGE,
          mapOf("operation" to "delete", "modelId" to modelId),
          message,
        )
      is LibraryError.UnexpectedError -> Triple(MODEL_LIBRARY_ERROR, emptyMap(), message)
      is LibraryError.HuggingFaceLoadFailed ->
        Triple(HUGGING_FACE_LOAD_ERROR, mapOf("operation" to "huggingFaceFetch"), message)
    }

  return NanoAIErrorEnvelope(userMessage = resolvedMessage, context = context)
    .withFallbackMessage(fallback)
}

private const val DOWNLOAD_FAILURE_MESSAGE = "Unable to download model"
private const val PAUSE_FAILURE_MESSAGE = "Unable to pause download"
private const val RESUME_FAILURE_MESSAGE = "Unable to resume download"
private const val CANCEL_FAILURE_MESSAGE = "Unable to cancel download"
private const val RETRY_FAILURE_MESSAGE = "Unable to retry download"
private const val DELETE_FAILURE_MESSAGE = "Unable to delete model"
private const val HUGGING_FACE_LOAD_ERROR = "Unable to load Hugging Face catalog"
private const val MODEL_LIBRARY_ERROR = "Model library error"
