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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceModelSummary
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.library.DownloadStatus
import com.vjaykrsna.nanoai.feature.library.presentation.ModelLibraryTab
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryDownloadItem
import com.vjaykrsna.nanoai.feature.library.presentation.model.ModelLibrarySections
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryUiConstants.LIST_TAG
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryUiConstants.PERCENTAGE_MULTIPLIER
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
  modifier: Modifier = Modifier,
  onImportLocalModel: (() -> Unit)? = null,
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
    localLibraryCalloutSection(showLocalCallout, onImportLocalModel)
    downloadSection(sections.downloads, onPause, onResume, onCancel, onRetry)
    attentionSection(sections.attention, onDownload, onDelete)
    installedSection(sections.installed, onDelete)
    availableSection(sections.available, onDownload)
    emptyStateSection(listHasContent)
  }
}

@Composable
internal fun HuggingFaceLibraryContent(
  models: List<HuggingFaceModelSummary>,
  isLoading: Boolean,
  modifier: Modifier = Modifier,
  onDownloadModel: ((HuggingFaceModelSummary) -> Unit)? = null,
  downloadableModelIds: Set<String> = emptySet(),
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
        items(items = models, key = { it.modelId }, contentType = { "huggingface_model_item" }) {
          model ->
          val isDownloadable = model.modelId in downloadableModelIds
          HuggingFaceModelCard(
            model = model,
            isDownloadable = isDownloadable,
            onDownload =
              if (onDownloadModel != null) {
                { onDownloadModel(model) }
              } else {
                null
              },
          )
        }
      }
    }
  }
}

@Composable
internal fun DownloadQueueCard(
  item: LibraryDownloadItem,
  onPause: (UUID) -> Unit,
  onResume: (UUID) -> Unit,
  onCancel: (UUID) -> Unit,
  onRetry: (UUID) -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = item.model?.displayName ?: item.task.modelId,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
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
internal fun SectionHeader(title: String, subtitle: String, modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.semantics { heading() },
    )
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
internal fun LocalLibraryCallout(onImportLocalModel: (() -> Unit)?) {
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
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = download.modelId,
          style = MaterialTheme.typography.titleSmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = downloadStatusLabel(download),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
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
internal fun EmptyState(
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
