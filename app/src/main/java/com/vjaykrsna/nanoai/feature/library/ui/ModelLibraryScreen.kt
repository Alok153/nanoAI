@file:Suppress("LongMethod")

package com.vjaykrsna.nanoai.feature.library.ui

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
  val sections by viewModel.sections.collectAsState()
  val providerOptions by viewModel.providerOptions.collectAsState()
  val capabilityOptions by viewModel.capabilityOptions.collectAsState()
  val hasActiveFilters by viewModel.hasActiveFilters.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()
  val isRefreshing by viewModel.isRefreshing.collectAsState()

  val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }

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
        }
      snackbarHostState.showSnackbar(message)
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
        hasActiveFilters = hasActiveFilters,
        onSearchChange = viewModel::updateSearchQuery,
        onProviderSelect = viewModel::selectProvider,
        onCapabilityToggle = viewModel::toggleCapability,
        onSortSelect = viewModel::setSort,
        onClearFilters = viewModel::clearFilters,
      )

      if (isLoading) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
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
          sections = sections,
          onDownload = { model -> viewModel.downloadModel(model.modelId) },
          onDelete = { model -> viewModel.deleteModel(model.modelId) },
          onPause = viewModel::pauseDownload,
          onResume = viewModel::resumeDownload,
          onCancel = viewModel::cancelDownload,
          onRetry = viewModel::retryDownload,
        )
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
