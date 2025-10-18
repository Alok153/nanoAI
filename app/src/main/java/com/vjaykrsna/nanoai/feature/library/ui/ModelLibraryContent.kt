package com.vjaykrsna.nanoai.feature.library.ui

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.domain.model.HuggingFaceModelSummary
import com.vjaykrsna.nanoai.feature.library.model.DownloadStatus
import com.vjaykrsna.nanoai.feature.library.presentation.ModelLibraryTab
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryDownloadItem
import com.vjaykrsna.nanoai.feature.library.presentation.model.ModelLibrarySections
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryUiConstants.DOWNLOAD_QUEUE_HEADER_TAG
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryUiConstants.DOWNLOAD_QUEUE_TAG
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryUiConstants.LIST_TAG
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryUiConstants.PERCENTAGE_MULTIPLIER
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryUiConstants.SECTION_ATTENTION_TAG
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryUiConstants.SECTION_AVAILABLE_TAG
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryUiConstants.SECTION_INSTALLED_TAG
import java.util.UUID

@Composable
internal fun ModelLibraryContent(
  sections: ModelLibrarySections,
  selectedTab: ModelLibraryTab,
  onDownload: (ModelPackage) -> Unit,
  onDelete: (ModelPackage) -> Unit,
  onPause: (UUID) -> Unit,
  onResume: (UUID) -> Unit,
  onCancel: (UUID) -> Unit,
  onRetry: (UUID) -> Unit,
  onImportLocalModel: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val listHasContent =
    sections.downloads.isNotEmpty() ||
      sections.attention.isNotEmpty() ||
      sections.installed.isNotEmpty() ||
      sections.available.isNotEmpty()

  val showLocalCallout =
    selectedTab == ModelLibraryTab.LOCAL &&
      sections.downloads.isEmpty() &&
      sections.attention.isEmpty() &&
      sections.installed.isEmpty()

  LazyColumn(
    modifier = modifier.fillMaxWidth().testTag(LIST_TAG),
    contentPadding = PaddingValues(bottom = 32.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    if (showLocalCallout) {
      item(key = "local_cta") { LocalLibraryCallout(onImportLocalModel = onImportLocalModel) }
    }

    if (sections.downloads.isNotEmpty()) {
      item(key = "downloads_header") {
        SectionHeader(
          title = "Active downloads",
          subtitle = "In-progress and queued runtime updates",
          modifier = Modifier.testTag(DOWNLOAD_QUEUE_HEADER_TAG),
        )
      }
      items(items = sections.downloads, key = { "download_${it.task.taskId}" }) { item ->
        DownloadQueueCard(
          item = item,
          onPause = onPause,
          onResume = onResume,
          onCancel = onCancel,
          onRetry = onRetry,
          modifier = Modifier.testTag(DOWNLOAD_QUEUE_TAG),
        )
      }
    }

    if (sections.attention.isNotEmpty()) {
      item(key = "attention_header") {
        SectionHeader(
          title = "Needs attention",
          subtitle = "Downloads that require manual action",
          modifier = Modifier.testTag(SECTION_ATTENTION_TAG),
        )
      }
      items(
        items = sections.attention,
        key = { "attention_${it.modelId}_${it.providerType}_${it.version}" }
      ) { model ->
        ModelManagementCard(
          model = model,
          primaryActionLabel = "Retry",
          onPrimaryAction = { onDownload(model) },
          secondaryActionLabel = "Delete",
          onSecondaryAction = { onDelete(model) },
          primaryActionIcon = Icons.Filled.Refresh,
        )
      }
    }

    if (sections.installed.isNotEmpty()) {
      item(key = "installed_header") {
        SectionHeader(
          title = "Installed",
          subtitle = "Local runtimes ready for inference",
          modifier = Modifier.testTag(SECTION_INSTALLED_TAG),
        )
      }
      items(
        items = sections.installed,
        key = { "installed_${it.modelId}_${it.providerType}_${it.version}" }
      ) { model ->
        ModelManagementCard(
          model = model,
          primaryActionLabel = "Remove",
          onPrimaryAction = { onDelete(model) },
          primaryActionIcon = Icons.Filled.Delete,
        )
      }
    }

    if (sections.available.isNotEmpty()) {
      item(key = "available_header") {
        SectionHeader(
          title = "Available",
          subtitle = "Models ready for download",
          modifier = Modifier.testTag(SECTION_AVAILABLE_TAG),
        )
      }
      items(
        items = sections.available,
        key = { "available_${it.modelId}_${it.providerType}_${it.version}" }
      ) { model ->
        ModelManagementCard(
          model = model,
          primaryActionLabel = "Download",
          onPrimaryAction = { onDownload(model) },
          secondaryActionLabel = null,
          onSecondaryAction = {},
          primaryActionIcon = Icons.Filled.Download,
        )
      }
    }

    if (!listHasContent) {
      item(key = "empty_state") { EmptyState() }
    }
  }
}

@Composable
internal fun HuggingFaceLibraryContent(
  models: List<HuggingFaceModelSummary>,
  isLoading: Boolean,
  modifier: Modifier = Modifier,
) {
  val rootModifier = modifier.fillMaxWidth()
  when {
    isLoading && models.isEmpty() -> {
      Box(modifier = rootModifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    }
    models.isEmpty() -> {
      Box(modifier = rootModifier, contentAlignment = Alignment.Center) {
        EmptyState(
          title = "No Hugging Face models found",
          message = "Try a different search term or explore the curated tab.",
        )
      }
    }
    else -> {
      LazyColumn(
        modifier = rootModifier.testTag(LIST_TAG),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        items(models, key = { it.modelId }) { model -> HuggingFaceModelCard(model) }
      }
    }
  }
}

@Composable
private fun DownloadQueueCard(
  item: LibraryDownloadItem,
  onPause: (UUID) -> Unit,
  onResume: (UUID) -> Unit,
  onCancel: (UUID) -> Unit,
  onRetry: (UUID) -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Text(
        text = item.model?.displayName ?: item.task.modelId,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
      )

      DownloadTaskItem(
        download = item.task,
        onPause = onPause,
        onResume = onResume,
        onCancel = onCancel,
        onRetry = onRetry,
      )
    }
  }
}

@Composable
private fun SectionHeader(
  title: String,
  subtitle: String,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold
    )
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
private fun LocalLibraryCallout(onImportLocalModel: (() -> Unit)?) {
  Surface(shape = MaterialTheme.shapes.large, tonalElevation = 2.dp) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "Your local library is empty",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text =
          "Download curated picks or browse Hugging Face to find something great. You can also import a local model file to get started immediately.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (onImportLocalModel != null) {
        FilledTonalButton(onClick = onImportLocalModel) {
          Icon(Icons.Filled.FileUpload, contentDescription = null)
          Spacer(modifier = Modifier.size(8.dp))
          Text(text = "Import local model")
        }
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
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = download.modelId,
          style = MaterialTheme.typography.titleSmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = downloadStatusLabel(download),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        when (download.status) {
          DownloadStatus.DOWNLOADING -> {
            IconButton(onClick = { onPause(download.taskId) }) {
              Icon(Icons.Filled.Pause, contentDescription = "Pause download")
            }
            IconButton(onClick = { onCancel(download.taskId) }) {
              Icon(Icons.Filled.Close, contentDescription = "Cancel download")
            }
          }
          DownloadStatus.PAUSED -> {
            IconButton(onClick = { onResume(download.taskId) }) {
              Icon(Icons.Filled.PlayArrow, contentDescription = "Resume download")
            }
            IconButton(onClick = { onCancel(download.taskId) }) {
              Icon(Icons.Filled.Close, contentDescription = "Cancel download")
            }
          }
          DownloadStatus.FAILED -> {
            IconButton(onClick = { onRetry(download.taskId) }) {
              Icon(Icons.Filled.Refresh, contentDescription = "Retry download")
            }
          }
          else -> Unit
        }
      }
    }

    if (download.status == DownloadStatus.DOWNLOADING || download.status == DownloadStatus.PAUSED) {
      LinearDownloadIndicator(progress = download.progress)
    }
  }
}

@Composable
private fun LinearDownloadIndicator(progress: Float) {
  androidx.compose.material3.LinearProgressIndicator(
    progress = { progress.coerceIn(0f, 1f) },
    modifier =
      Modifier.fillMaxWidth().semantics {
        val percent = (progress * PERCENTAGE_MULTIPLIER).toInt()
        contentDescription = "Downloading ${percent}%"
      },
    trackColor = MaterialTheme.colorScheme.surface,
  )
}

@Composable
private fun EmptyState(
  title: String = "No models to show",
  message: String = "Adjust your filters or connect to the catalog to discover new runtimes.",
) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(text = title, style = MaterialTheme.typography.titleMedium)
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}
