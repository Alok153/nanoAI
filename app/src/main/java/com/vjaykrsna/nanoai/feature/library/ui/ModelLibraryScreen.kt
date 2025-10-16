@file:Suppress("LongMethod")

package com.vjaykrsna.nanoai.feature.library.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vjaykrsna.nanoai.feature.library.presentation.LibraryError
import com.vjaykrsna.nanoai.feature.library.presentation.LibraryUiEvent
import com.vjaykrsna.nanoai.feature.library.presentation.ModelLibraryTab
import com.vjaykrsna.nanoai.feature.library.presentation.ModelLibraryViewModel
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryUiConstants.LOADING_INDICATOR_TAG
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ModelLibraryScreen(
  modifier: Modifier = Modifier,
  viewModel: ModelLibraryViewModel = hiltViewModel()
) {
  val filters by viewModel.filters.collectAsState()
  val summary by viewModel.summary.collectAsState()
  val localSections by viewModel.localSections.collectAsState()
  val curatedSections by viewModel.curatedSections.collectAsState()
  val huggingFaceModels by viewModel.huggingFaceModels.collectAsState()
  val huggingFaceFilters by viewModel.huggingFaceFilters.collectAsState()
  val huggingFacePipelineOptions by viewModel.huggingFacePipelineOptions.collectAsState()
  val huggingFaceLibraryOptions by viewModel.huggingFaceLibraryOptions.collectAsState()
  val providerOptions by viewModel.providerOptions.collectAsState()
  val capabilityOptions by viewModel.capabilityOptions.collectAsState()
  val hasActiveFilters by viewModel.hasActiveFilters.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()
  val isRefreshing by viewModel.isRefreshing.collectAsState()
  val isHuggingFaceLoading by viewModel.isHuggingFaceLoading.collectAsState()

  val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }

  val documentLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
      uri?.let(viewModel::importLocalModel)
    }

  LaunchedEffect(Unit) {
    viewModel.errorEvents.collectLatest { error ->
      val message =
        when (error) {
          is LibraryError.DownloadFailed -> "Download failed for ${error.modelId}: ${error.message}"
          is LibraryError.DeleteFailed -> "Delete failed for ${error.modelId}: ${error.message}"
          is LibraryError.PauseFailed -> "Pause failed: ${error.message}"
          is LibraryError.ResumeFailed -> "Resume failed: ${error.message}"
          is LibraryError.CancelFailed -> "Cancel failed: ${error.message}"
          is LibraryError.RetryFailed -> "Retry failed: ${error.message}"
          is LibraryError.UnexpectedError -> "Error: ${error.message}"
          is LibraryError.HuggingFaceLoadFailed -> "Hugging Face error: ${error.message}"
        }
      snackbarHostState.showSnackbar(message)
    }
  }

  LaunchedEffect(documentLauncher) {
    viewModel.uiEvents.collectLatest { event ->
      when (event) {
        LibraryUiEvent.RequestLocalModelImport ->
          documentLauncher.launch(
            arrayOf("application/octet-stream", "application/x-tflite", "*/*")
          )
      }
    }
  }

  val pullRefreshState =
    rememberPullRefreshState(
      refreshing = isRefreshing,
      onRefresh = viewModel::refreshCatalog,
    )

  Box(
    modifier =
      modifier.fillMaxSize().pullRefresh(pullRefreshState).semantics {
        contentDescription = "Model library screen with enhanced management controls"
      },
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      LibraryHeader(summary = summary)

      ModelLibraryToolbar(
        filters = filters,
        providers = providerOptions,
        capabilities = capabilityOptions,
        hasActiveFilters =
          if (filters.tab == ModelLibraryTab.HUGGING_FACE) false else hasActiveFilters,
        showModelFilters = filters.tab != ModelLibraryTab.HUGGING_FACE,
        onSearchChange = viewModel::updateSearchQuery,
        onProviderSelect = viewModel::selectProvider,
        onCapabilityToggle = viewModel::toggleCapability,
        onSortSelect = viewModel::setSort,
        onClearFilters = viewModel::clearFilters,
      )

      ModelLibraryTabs(
        selectedTab = filters.tab,
        onTabSelected = viewModel::selectTab,
      )

      when (filters.tab) {
        ModelLibraryTab.LOCAL -> {
          if (isLoading) {
            Box(
              modifier = Modifier.fillMaxWidth().weight(1f),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator(
                modifier =
                  Modifier.testTag(LOADING_INDICATOR_TAG).semantics {
                    contentDescription = "Loading models"
                  },
              )
            }
          } else {
            ModelLibraryContent(
              modifier = Modifier.weight(1f),
              sections = localSections,
              selectedTab = ModelLibraryTab.LOCAL,
              onDownload = { model -> viewModel.downloadModel(model.modelId) },
              onDelete = { model -> viewModel.deleteModel(model.modelId) },
              onPause = viewModel::pauseDownload,
              onResume = viewModel::resumeDownload,
              onCancel = viewModel::cancelDownload,
              onRetry = viewModel::retryDownload,
              onImportLocalModel = viewModel::requestLocalModelImport,
            )
          }
        }
        ModelLibraryTab.CURATED -> {
          if (isLoading) {
            Box(
              modifier = Modifier.fillMaxWidth().weight(1f),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator(
                modifier =
                  Modifier.testTag(LOADING_INDICATOR_TAG).semantics {
                    contentDescription = "Loading curated models"
                  },
              )
            }
          } else {
            ModelLibraryContent(
              modifier = Modifier.weight(1f),
              sections = curatedSections,
              selectedTab = ModelLibraryTab.CURATED,
              onDownload = { model -> viewModel.downloadModel(model.modelId) },
              onDelete = { model -> viewModel.deleteModel(model.modelId) },
              onPause = viewModel::pauseDownload,
              onResume = viewModel::resumeDownload,
              onCancel = viewModel::cancelDownload,
              onRetry = viewModel::retryDownload,
            )
          }
        }
        ModelLibraryTab.HUGGING_FACE -> {
          HuggingFaceFilterBar(
            filters = huggingFaceFilters,
            pipelineOptions = huggingFacePipelineOptions,
            libraryOptions = huggingFaceLibraryOptions,
            onSortSelect = viewModel::setHuggingFaceSort,
            onPipelineSelect = viewModel::setHuggingFacePipeline,
            onLibrarySelect = viewModel::setHuggingFaceLibrary,
            onClearFilters = viewModel::clearHuggingFaceFilters,
          )
          HuggingFaceLibraryContent(
            modifier = Modifier.weight(1f),
            models = huggingFaceModels,
            isLoading = isHuggingFaceLoading,
          )
        }
      }
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
    )

    PullRefreshIndicator(
      refreshing = isRefreshing,
      state = pullRefreshState,
      modifier = Modifier.align(Alignment.TopCenter),
    )
  }
}
