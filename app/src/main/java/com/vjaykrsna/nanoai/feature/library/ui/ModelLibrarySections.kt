package com.vjaykrsna.nanoai.feature.library.ui

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.presentation.model.LibraryDownloadItem
import java.util.UUID

internal fun LazyListScope.localLibraryCalloutSection(
  showLocalCallout: Boolean,
  onImportLocalModel: (() -> Unit)?,
) {
  if (!showLocalCallout) return
  item(key = "local_cta") { LocalLibraryCallout(onImportLocalModel = onImportLocalModel) }
}

internal fun LazyListScope.downloadSection(
  downloads: List<LibraryDownloadItem>,
  onPause: (UUID) -> Unit,
  onResume: (UUID) -> Unit,
  onCancel: (UUID) -> Unit,
  onRetry: (UUID) -> Unit,
) {
  if (downloads.isEmpty()) return

  item(key = "downloads_header") {
    SectionHeader(
      title = "Active downloads",
      subtitle = "In-progress and queued runtime updates",
      modifier = Modifier.testTag(ModelLibraryUiConstants.DOWNLOAD_QUEUE_HEADER_TAG),
    )
  }
  items(
    items = downloads,
    key = { "download_${it.task.taskId}" },
    contentType = { "download_item" },
  ) { item ->
    DownloadQueueCard(
      item = item,
      onPause = onPause,
      onResume = onResume,
      onCancel = onCancel,
      onRetry = onRetry,
      modifier = Modifier.testTag(ModelLibraryUiConstants.DOWNLOAD_QUEUE_TAG),
    )
  }
}

internal fun LazyListScope.attentionSection(
  attention: List<ModelPackage>,
  onDownload: (ModelPackage) -> Unit,
  onDelete: (ModelPackage) -> Unit,
) {
  if (attention.isEmpty()) return

  item(key = "attention_header") {
    SectionHeader(
      title = "Needs attention",
      subtitle = "Downloads that require manual action",
      modifier = Modifier.testTag(ModelLibraryUiConstants.SECTION_ATTENTION_TAG),
    )
  }
  items(
    items = attention,
    key = { "attention_${it.modelId}_${it.providerType}_${it.version}" },
    contentType = { "model_attention_item" },
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

internal fun LazyListScope.installedSection(
  installed: List<ModelPackage>,
  onDelete: (ModelPackage) -> Unit,
) {
  if (installed.isEmpty()) return

  item(key = "installed_header") {
    SectionHeader(
      title = "Installed",
      subtitle = "Local runtimes ready for inference",
      modifier = Modifier.testTag(ModelLibraryUiConstants.SECTION_INSTALLED_TAG),
    )
  }
  items(
    items = installed,
    key = { "installed_${it.modelId}_${it.providerType}_${it.version}" },
    contentType = { "model_installed_item" },
  ) { model ->
    ModelManagementCard(
      model = model,
      primaryActionLabel = "Remove",
      onPrimaryAction = { onDelete(model) },
      primaryActionIcon = Icons.Filled.Delete,
    )
  }
}

internal fun LazyListScope.availableSection(
  available: List<ModelPackage>,
  onDownload: (ModelPackage) -> Unit,
) {
  if (available.isEmpty()) return

  item(key = "available_header") {
    SectionHeader(
      title = "Available",
      subtitle = "Models ready for download",
      modifier = Modifier.testTag(ModelLibraryUiConstants.SECTION_AVAILABLE_TAG),
    )
  }
  items(
    items = available,
    key = { "available_${it.modelId}_${it.providerType}_${it.version}" },
    contentType = { "model_available_item" },
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

internal fun LazyListScope.emptyStateSection(listHasContent: Boolean) {
  if (listHasContent) return
  item(key = "empty_state") { EmptyState() }
}
