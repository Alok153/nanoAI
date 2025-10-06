@file:Suppress(
  "LongMethod",
  "CyclomaticComplexMethod",
  "LongParameterList",
) // Complex UI with many composables

package com.vjaykrsna.nanoai.feature.library.ui

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.model.DownloadStatus
import com.vjaykrsna.nanoai.feature.library.presentation.InstallStateFilter
import com.vjaykrsna.nanoai.feature.library.presentation.LibraryError
import com.vjaykrsna.nanoai.feature.library.presentation.ModelLibraryViewModel
import java.util.UUID
import kotlinx.coroutines.flow.collectLatest

private const val PERCENTAGE_MULTIPLIER = 100

@Composable
fun ModelLibraryScreen(
  modifier: Modifier = Modifier,
  viewModel: ModelLibraryViewModel = hiltViewModel()
) {
  val allModels by viewModel.allModels.collectAsState()
  val installedModels by viewModel.installedModels.collectAsState()
  val filteredModels by viewModel.filteredModels.collectAsState()
  val queuedDownloads by viewModel.queuedDownloads.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()

  val snackbarHostState = remember { SnackbarHostState() }

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

  Box(
    modifier =
      modifier.fillMaxSize().semantics {
        contentDescription = "Model library screen with available and installed models"
      },
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      LibraryHeader(modifier = Modifier.fillMaxWidth())

      FilterSection(
        onFilterChange = { filter -> viewModel.setFilter(filter) },
        modifier = Modifier.fillMaxWidth(),
      )

      if (queuedDownloads.isNotEmpty()) {
        DownloadQueueSection(
          downloads = queuedDownloads,
          onPause = { viewModel.pauseDownload(it) },
          onResume = { viewModel.resumeDownload(it) },
          onCancel = { viewModel.cancelDownload(it) },
          onRetry = { viewModel.retryDownload(it) },
          modifier = Modifier.fillMaxWidth(),
        )
      }

      if (isLoading) {
        Box(
          modifier = Modifier.fillMaxWidth().weight(1f, fill = true),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator(
            modifier = Modifier.semantics { contentDescription = "Loading models" },
          )
        }
      } else {
        ModelsGrid(
          models = filteredModels,
          installedModels = installedModels,
          onDownload = { viewModel.downloadModel(it.modelId) },
          onDelete = { viewModel.deleteModel(it.modelId) },
          modifier = Modifier.fillMaxWidth().weight(1f, fill = true),
        )
      }
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier =
        Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 24.dp),
    )
  }
}

@Composable
private fun LibraryHeader(modifier: Modifier = Modifier) {
  Column(modifier = modifier) {
    Text(
      text = "Model Library",
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = "Browse and download AI models for offline use",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSection(
  onFilterChange: (InstallStateFilter) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedFilter by remember { mutableStateOf(InstallStateFilter.ALL) }
  val filters =
    listOf(
      InstallStateFilter.ALL to "All",
      InstallStateFilter.INSTALLED to "Installed",
      InstallStateFilter.DOWNLOADING to "Downloading",
      InstallStateFilter.AVAILABLE to "Available",
    )

  LazyRow(
    contentPadding = PaddingValues(horizontal = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier,
  ) {
    items(filters) { (filter, label) ->
      FilterChip(
        selected = selectedFilter == filter,
        onClick = {
          selectedFilter = filter
          onFilterChange(filter)
        },
        label = { Text(label) },
        modifier = Modifier.semantics { contentDescription = "$label filter chip" },
      )
    }
  }
}

@Composable
private fun DownloadQueueSection(
  downloads: List<DownloadTask>,
  onPause: (UUID) -> Unit,
  onResume: (UUID) -> Unit,
  onCancel: (UUID) -> Unit,
  onRetry: (UUID) -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier,
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
      ),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
      Text(
        text = "Download Queue",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )

      Spacer(modifier = Modifier.height(12.dp))

      downloads.forEach { download ->
        DownloadTaskItem(
          download = download,
          onPause = onPause,
          onResume = onResume,
          onCancel = onCancel,
          onRetry = onRetry,
        )
        Spacer(modifier = Modifier.height(8.dp))
      }
    }
  }
}

@VisibleForTesting
@Composable
internal fun DownloadTaskItem(
  download: DownloadTask,
  onPause: (UUID) -> Unit,
  onResume: (UUID) -> Unit,
  onCancel: (UUID) -> Unit,
  onRetry: (UUID) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = download.modelId,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
        )

        val progressPercent = (download.progress * PERCENTAGE_MULTIPLIER).toInt()
        val statusText =
          when (download.status) {
            DownloadStatus.QUEUED -> "Queued"
            DownloadStatus.DOWNLOADING -> "Downloading $progressPercent%"
            DownloadStatus.PAUSED -> "Paused at $progressPercent%"
            DownloadStatus.COMPLETED -> "Completed"
            DownloadStatus.FAILED -> "Failed: ${download.errorMessage ?: "Unknown error"}"
            DownloadStatus.CANCELLED -> "Cancelled"
          }

        Text(
          text = statusText,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        when (download.status) {
          DownloadStatus.DOWNLOADING -> {
            IconButton(
              onClick = { onPause(download.taskId) },
              modifier = Modifier.semantics { contentDescription = "Pause download" },
            ) {
              Icon(Icons.Default.PlayArrow, "Pause")
            }
            IconButton(
              onClick = { onCancel(download.taskId) },
              modifier = Modifier.semantics { contentDescription = "Cancel download" },
            ) {
              Icon(Icons.Default.Close, "Cancel")
            }
          }
          DownloadStatus.PAUSED -> {
            IconButton(
              onClick = { onResume(download.taskId) },
              modifier = Modifier.semantics { contentDescription = "Resume download" },
            ) {
              Icon(Icons.Default.PlayArrow, "Resume")
            }
            IconButton(
              onClick = { onCancel(download.taskId) },
              modifier = Modifier.semantics { contentDescription = "Cancel download" },
            ) {
              Icon(Icons.Default.Close, "Cancel")
            }
          }
          DownloadStatus.FAILED -> {
            IconButton(
              onClick = { onRetry(download.taskId) },
              modifier = Modifier.semantics { contentDescription = "Retry download" },
            ) {
              Icon(Icons.Default.Refresh, "Retry")
            }
          }
          else -> {}
        }
      }
    }

    if (download.status == DownloadStatus.DOWNLOADING || download.status == DownloadStatus.PAUSED) {
      Spacer(modifier = Modifier.height(8.dp))
      LinearProgressIndicator(
        progress = { download.progress },
        modifier =
          Modifier.fillMaxWidth().semantics {
            val progressPercent = (download.progress * PERCENTAGE_MULTIPLIER).toInt()
            contentDescription = "Download progress $progressPercent%"
          },
      )
    }
  }
}

@Composable
private fun ModelsGrid(
  models: List<ModelPackage>,
  installedModels: List<ModelPackage>,
  onDownload: (ModelPackage) -> Unit,
  onDelete: (ModelPackage) -> Unit,
  modifier: Modifier = Modifier
) {
  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = modifier.semantics { contentDescription = "Models list" },
  ) {
    items(
      items = models,
      key = { it.modelId },
      contentType = { "model_card" },
    ) { model ->
      val isInstalled = installedModels.any { it.modelId == model.modelId }
      ModelCard(
        model = model,
        isInstalled = isInstalled,
        onDownload = { onDownload(model) },
        onDelete = { onDelete(model) },
      )
    }
  }
}

@VisibleForTesting
@Composable
internal fun ModelCard(
  model: ModelPackage,
  isInstalled: Boolean,
  onDownload: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = model.displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = "${model.providerType.name} · ${model.capabilities.joinToString(", ")}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            Text(
              text = "Version: ${model.version}",
              style = MaterialTheme.typography.bodySmall,
            )
            Text(
              text = "Size: ${formatSize(model.sizeBytes)}",
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }

        Spacer(modifier = Modifier.width(16.dp))

        if (isInstalled) {
          Row {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = "Installed",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
              onClick = onDelete,
              modifier = Modifier.semantics { contentDescription = "Delete ${model.displayName}" },
            ) {
              Icon(Icons.Default.Delete, "Delete")
            }
          }
        } else {
          IconButton(
            onClick = onDownload,
            modifier = Modifier.semantics { contentDescription = "Download ${model.displayName}" },
          ) {
            Icon(Icons.Default.Refresh, "Download")
          }
        }
      }
    }
  }
}

private const val BYTES_PER_KB = 1024.0
private const val BYTES_PER_MB = BYTES_PER_KB * 1024.0
private const val BYTES_PER_GB = BYTES_PER_MB * 1024.0

private fun formatSize(bytes: Long): String {
  val kb = bytes / BYTES_PER_KB
  val mb = bytes / BYTES_PER_MB
  val gb = bytes / BYTES_PER_GB

  return when {
    gb >= 1 -> "%.2f GB".format(gb)
    mb >= 1 -> "%.2f MB".format(mb)
    else -> "%.2f KB".format(kb)
  }
}
