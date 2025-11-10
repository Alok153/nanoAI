package com.vjaykrsna.nanoai.feature.library.ui

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
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
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceModelSummary
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import com.vjaykrsna.nanoai.feature.library.presentation.ModelLibraryTab
import com.vjaykrsna.nanoai.feature.library.presentation.ModelLibraryViewModel
import com.vjaykrsna.nanoai.feature.library.presentation.model.HuggingFaceSortOption
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryError
import com.vjaykrsna.nanoai.feature.library.presentation.model.ModelLibraryUiEvent
import com.vjaykrsna.nanoai.feature.library.presentation.model.ModelSort
import com.vjaykrsna.nanoai.feature.library.presentation.state.ModelLibraryUiState
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryUiConstants.LOADING_INDICATOR_TAG
import java.util.UUID
import kotlinx.coroutines.flow.collectLatest

/**
 * Model library screen for browsing, downloading, and managing AI models.
 *
 * Features:
 * - Tabbed interface for local vs remote models
 * - Search and filtering capabilities
 * - Download progress tracking
 * - Model management (delete, retry downloads)
 * - Pull-to-refresh functionality
 *
 * @param modifier Modifier to apply to the screen
 * @param viewModel ModelLibraryViewModel for managing library state and operations
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ModelLibraryScreen(
  modifier: Modifier = Modifier,
  viewModel: ModelLibraryViewModel = hiltViewModel(),
) {
  val uiState by viewModel.state.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val documentLauncher = rememberImportModelLauncher(viewModel::importLocalModel)
  val actions = rememberModelLibraryActions(viewModel)

  LaunchedEffect(viewModel, snackbarHostState, documentLauncher) {
    viewModel.events.collectLatest { event ->
      when (event) {
        is ModelLibraryUiEvent.ErrorRaised ->
          snackbarHostState.showSnackbar(event.error.toDisplayMessage())
        is ModelLibraryUiEvent.Message -> snackbarHostState.showSnackbar(event.message)
        ModelLibraryUiEvent.RequestLocalModelImport ->
          documentLauncher.launch(
            arrayOf("application/octet-stream", "application/x-tflite", "*/*")
          )
      }
    }
  }

  val pullRefreshState =
    rememberPullRefreshState(refreshing = uiState.isRefreshing, onRefresh = actions.onRefresh)

  ModelLibraryLayout(
    modifier = modifier,
    state = uiState,
    snackbarHostState = snackbarHostState,
    pullRefreshState = pullRefreshState,
    actions = actions,
  )
}

@Composable
private fun rememberModelLibraryActions(viewModel: ModelLibraryViewModel): ModelLibraryActions {
  return remember(viewModel) {
    ModelLibraryActions(
      onRefresh = { viewModel.refreshCatalog() },
      onDownloadModel = { model -> viewModel.downloadModel(model.modelId) },
      onDeleteModel = { model -> viewModel.deleteModel(model.modelId) },
      onPauseDownload = viewModel::pauseDownload,
      onResumeDownload = viewModel::resumeDownload,
      onCancelDownload = viewModel::cancelDownload,
      onRetryDownload = viewModel::retryDownload,
      onImportLocalModel = viewModel::requestLocalModelImport,
      onSelectTab = viewModel::selectTab,
      onUpdateSearch = viewModel::updateSearchQuery,
      onSelectPipeline = viewModel::setPipeline,
      onSelectLocalSort = viewModel::setLocalSort,
      onSelectHuggingFaceSort = viewModel::setHuggingFaceSort,
      onSelectLocalLibrary = viewModel::selectLocalLibrary,
      onSelectHuggingFaceLibrary = viewModel::setHuggingFaceLibrary,
      onToggleCapability = viewModel::toggleCapability,
      onDownloadHuggingFaceModel = viewModel::downloadHuggingFaceModel,
    )
  }
}

@Composable
private fun rememberImportModelLauncher(
  onImport: (Uri) -> Unit
): ManagedActivityResultLauncher<Array<String>, Uri?> {
  return rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    uri?.let(onImport)
  }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ModelLibraryLayout(
  modifier: Modifier = Modifier,
  state: ModelLibraryUiState,
  snackbarHostState: SnackbarHostState,
  pullRefreshState: PullRefreshState,
  actions: ModelLibraryActions,
) {
  Box(
    modifier =
      modifier.fillMaxSize().pullRefresh(pullRefreshState).semantics {
        contentDescription = "Model library screen with enhanced management controls"
      }
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      LibraryHeader(summary = state.summary)

      ModelLibraryToolbar(
        tab = state.filters.tab,
        searchQuery = state.filters.currentSearchQuery(),
        pipelineOptions = state.pipelineOptions,
        selectedPipeline = state.filters.pipelineTag,
        localSort = state.filters.localSort,
        huggingFaceSort = state.filters.huggingFaceSort,
        localLibraryOptions = state.providerOptions,
        selectedLocalLibrary = state.filters.localLibrary,
        huggingFaceLibraryOptions = state.huggingFaceLibraryOptions,
        selectedHuggingFaceLibrary = state.filters.huggingFaceLibrary,
        capabilityOptions = state.capabilityOptions,
        selectedCapabilities = state.filters.selectedCapabilities,
        activeFilterCount = state.filters.activeFilterCount,
        onSearchChange = actions.onUpdateSearch,
        onPipelineSelect = actions.onSelectPipeline,
        onSelectLocalSort = actions.onSelectLocalSort,
        onSelectHuggingFaceSort = actions.onSelectHuggingFaceSort,
        onSelectLocalLibrary = actions.onSelectLocalLibrary,
        onSelectHuggingFaceLibrary = actions.onSelectHuggingFaceLibrary,
        onToggleCapability = actions.onToggleCapability,
      )

      ModelLibraryTabs(selectedTab = state.filters.tab, onTabSelect = actions.onSelectTab)

      ModelLibraryTabContent(modifier = Modifier.weight(1f), state = state, actions = actions)
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier =
        Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp).semantics {
          contentDescription = "Model library notifications and messages"
        },
    )

    PullRefreshIndicator(
      refreshing = state.isRefreshing,
      state = pullRefreshState,
      modifier = Modifier.align(Alignment.TopCenter),
    )
  }
}

@Composable
private fun ModelLibraryTabContent(
  modifier: Modifier = Modifier,
  state: ModelLibraryUiState,
  actions: ModelLibraryActions,
) {
  when (state.filters.tab) {
    ModelLibraryTab.LOCAL -> {
      if (state.isLoading) {
        ModelLibraryLoadingIndicator(
          modifier = modifier.fillMaxWidth(),
          testTag = LOADING_INDICATOR_TAG,
          contentDescription = "Loading models",
        )
      } else {
        ModelLibraryContent(
          modifier = modifier,
          sections = state.localSections,
          selectedTab = ModelLibraryTab.LOCAL,
          onDownload = actions.onDownloadModel,
          onDelete = actions.onDeleteModel,
          onPause = actions.onPauseDownload,
          onResume = actions.onResumeDownload,
          onCancel = actions.onCancelDownload,
          onRetry = actions.onRetryDownload,
          onImportLocalModel = actions.onImportLocalModel,
        )
      }
    }
    ModelLibraryTab.CURATED -> {
      if (state.isLoading) {
        ModelLibraryLoadingIndicator(
          modifier = modifier.fillMaxWidth(),
          testTag = LOADING_INDICATOR_TAG,
          contentDescription = "Loading curated models",
        )
      } else {
        ModelLibraryContent(
          modifier = modifier,
          sections = state.curatedSections,
          selectedTab = ModelLibraryTab.CURATED,
          onDownload = actions.onDownloadModel,
          onDelete = actions.onDeleteModel,
          onPause = actions.onPauseDownload,
          onResume = actions.onResumeDownload,
          onCancel = actions.onCancelDownload,
          onRetry = actions.onRetryDownload,
        )
      }
    }
    ModelLibraryTab.HUGGING_FACE -> {
      HuggingFaceLibraryContent(
        modifier = modifier,
        models = state.huggingFaceModels,
        isLoading = state.isHuggingFaceLoading,
        onDownloadModel = actions.onDownloadHuggingFaceModel,
        downloadableModelIds = state.huggingFaceDownloadableModelIds,
      )
    }
  }
}

@Composable
private fun ModelLibraryLoadingIndicator(
  modifier: Modifier = Modifier,
  testTag: String,
  contentDescription: String,
) {
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    CircularProgressIndicator(
      modifier =
        Modifier.testTag(testTag).semantics { this.contentDescription = contentDescription }
    )
  }
}

private fun LibraryError.toDisplayMessage(): String =
  when (this) {
    is LibraryError.DownloadFailed -> "Download failed for ${modelId}: ${message}"
    is LibraryError.DeleteFailed -> "Delete failed for ${modelId}: ${message}"
    is LibraryError.PauseFailed -> "Pause failed: ${message}"
    is LibraryError.ResumeFailed -> "Resume failed: ${message}"
    is LibraryError.CancelFailed -> "Cancel failed: ${message}"
    is LibraryError.RetryFailed -> "Retry failed: ${message}"
    is LibraryError.UnexpectedError -> "Error: ${message}"
    is LibraryError.HuggingFaceLoadFailed -> "Hugging Face error: ${message}"
  }

private data class ModelLibraryActions(
  val onRefresh: () -> Unit,
  val onDownloadModel: (ModelPackage) -> Unit,
  val onDeleteModel: (ModelPackage) -> Unit,
  val onPauseDownload: (UUID) -> Unit,
  val onResumeDownload: (UUID) -> Unit,
  val onCancelDownload: (UUID) -> Unit,
  val onRetryDownload: (UUID) -> Unit,
  val onImportLocalModel: () -> Unit,
  val onSelectTab: (ModelLibraryTab) -> Unit,
  val onUpdateSearch: (String) -> Unit,
  val onSelectPipeline: (String?) -> Unit,
  val onSelectLocalSort: (ModelSort) -> Unit,
  val onSelectHuggingFaceSort: (HuggingFaceSortOption) -> Unit,
  val onSelectLocalLibrary: (ProviderType?) -> Unit,
  val onSelectHuggingFaceLibrary: (String?) -> Unit,
  val onToggleCapability: (String) -> Unit,
  val onDownloadHuggingFaceModel: (HuggingFaceModelSummary) -> Unit,
)
