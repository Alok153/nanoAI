package com.vjaykrsna.nanoai.feature.library.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.core.domain.library.DownloadModelUseCase
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceCatalogUseCase
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceModelCompatibilityChecker
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceModelSummary
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceToModelPackageConverter
import com.vjaykrsna.nanoai.core.domain.library.ModelCatalogUseCase
import com.vjaykrsna.nanoai.core.domain.library.RefreshModelCatalogUseCase
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import com.vjaykrsna.nanoai.feature.library.presentation.model.HuggingFaceSortOption
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryFilterState
import com.vjaykrsna.nanoai.feature.library.presentation.model.ModelLibrarySections
import com.vjaykrsna.nanoai.feature.library.presentation.model.ModelLibrarySummary
import com.vjaykrsna.nanoai.feature.library.presentation.model.ModelSort
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.StateFlow

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
  @MainImmediateDispatcher private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

  private val huggingFaceLibraryViewModel =
    HuggingFaceLibraryViewModel(huggingFaceCatalogUseCase, compatibilityChecker)

  private val stateStore =
    ModelLibraryStateStore(
      modelCatalogUseCase = modelCatalogUseCase,
      downloadManager = downloadManager,
      huggingFaceLibraryViewModel = huggingFaceLibraryViewModel,
      scope = viewModelScope,
    )

  private val eventDelegate =
    ModelLibraryEventDelegate(
      modelCatalogUseCase = modelCatalogUseCase,
      refreshModelCatalogUseCase = refreshModelCatalogUseCase,
      downloadModelUseCase = downloadModelUseCase,
      hfToModelConverter = hfToModelConverter,
      huggingFaceLibraryViewModel = huggingFaceLibraryViewModel,
      downloadManager = downloadManager,
      stateStore = stateStore,
      dispatcher = dispatcher,
      scope = viewModelScope,
    )

  init {
    eventDelegate.start()
    refreshCatalog()
  }

  val isRefreshing: StateFlow<Boolean> = stateStore.isRefreshing
  val isLoading: StateFlow<Boolean> = stateStore.isLoading
  val errorEvents = eventDelegate.errorEvents
  val uiEvents = eventDelegate.uiEvents
  val filters: StateFlow<LibraryFilterState> = stateStore.filters
  val allModels = stateStore.allModels
  val installedModels = stateStore.installedModels
  val providerOptions: StateFlow<List<ProviderType>> = stateStore.providerOptions
  val capabilityOptions = stateStore.capabilityOptions
  val huggingFaceModels = stateStore.huggingFaceModels
  val huggingFaceFilters = stateStore.huggingFaceFilters
  val huggingFacePipelineOptions = stateStore.huggingFacePipelineOptions
  val huggingFaceLibraryOptions = stateStore.huggingFaceLibraryOptions
  val isHuggingFaceLoading = stateStore.isHuggingFaceLoading
  val huggingFaceDownloadableModelIds = stateStore.huggingFaceDownloadableModelIds
  val pipelineOptions = stateStore.pipelineOptions
  val localSections: StateFlow<ModelLibrarySections> = stateStore.localSections
  val curatedSections: StateFlow<ModelLibrarySections> = stateStore.curatedSections
  val summary: StateFlow<ModelLibrarySummary> = stateStore.summary
  val hasActiveFilters: StateFlow<Boolean> = stateStore.hasActiveFilters

  fun refreshCatalog() = eventDelegate.refreshCatalog()

  fun updateSearchQuery(query: String) = stateStore.filtersController.updateSearchQuery(query)

  fun setPipeline(pipelineTag: String?) = stateStore.filtersController.setPipeline(pipelineTag)

  fun setLocalSort(sort: ModelSort) = stateStore.filtersController.setLocalSort(sort)

  fun setHuggingFaceSort(sort: HuggingFaceSortOption) =
    stateStore.filtersController.setHuggingFaceSort(sort)

  fun selectLocalLibrary(providerType: ProviderType?) =
    stateStore.filtersController.selectLocalLibrary(providerType)

  fun toggleCapability(capability: String) =
    stateStore.filtersController.toggleCapability(capability)

  fun clearSelectedCapabilities() = stateStore.filtersController.clearSelectedCapabilities()

  fun setHuggingFaceLibrary(library: String?) =
    stateStore.filtersController.setHuggingFaceLibrary(library)

  fun clearFilters() = stateStore.filtersController.clearFilters()

  fun selectTab(tab: ModelLibraryTab) = stateStore.filtersController.selectTab(tab)

  fun requestLocalModelImport() = eventDelegate.requestLocalModelImport()

  fun importLocalModel(uri: Uri) = eventDelegate.importLocalModel(uri)

  fun downloadModel(modelId: String) = eventDelegate.downloadActions.downloadModel(modelId)

  fun downloadHuggingFaceModel(model: HuggingFaceModelSummary) =
    eventDelegate.requestHuggingFaceDownload(model)

  fun pauseDownload(taskId: UUID) = eventDelegate.downloadActions.pauseDownload(taskId)

  fun resumeDownload(taskId: UUID) = eventDelegate.downloadActions.resumeDownload(taskId)

  fun cancelDownload(taskId: UUID) = eventDelegate.downloadActions.cancelDownload(taskId)

  fun retryDownload(taskId: UUID) = eventDelegate.downloadActions.retryDownload(taskId)

  fun deleteModel(modelId: String) = eventDelegate.downloadActions.deleteModel(modelId)

  fun observeDownloadProgress(taskId: UUID) =
    eventDelegate.downloadActions.observeDownloadProgress(taskId)
}
