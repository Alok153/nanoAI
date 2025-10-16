@file:OptIn(ExperimentalLayoutApi::class)

package com.vjaykrsna.nanoai.feature.library.ui

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.model.DownloadStatus
import com.vjaykrsna.nanoai.feature.library.model.InstallState
import com.vjaykrsna.nanoai.feature.library.model.ProviderType
import com.vjaykrsna.nanoai.feature.library.presentation.LibraryDownloadItem
import com.vjaykrsna.nanoai.feature.library.presentation.LibraryFilterState
import com.vjaykrsna.nanoai.feature.library.presentation.ModelLibrarySections
import com.vjaykrsna.nanoai.feature.library.presentation.ModelSort
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryUiConstants.DOWNLOAD_QUEUE_HEADER_TAG
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryUiConstants.DOWNLOAD_QUEUE_TAG
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryUiConstants.FILTER_PANEL_TAG
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryUiConstants.FILTER_TOGGLE_TAG
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryUiConstants.LIST_TAG
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryUiConstants.MAX_CAPABILITY_CHIPS
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryUiConstants.PERCENTAGE_MULTIPLIER
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryUiConstants.SEARCH_FIELD_TAG
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryUiConstants.SECTION_ATTENTION_TAG
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryUiConstants.SECTION_AVAILABLE_TAG
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryUiConstants.SECTION_INSTALLED_TAG
import java.util.Locale
import java.util.UUID

@Composable
internal fun ModelCard(
  model: ModelPackage,
  isInstalled: Boolean,
  onDownload: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (isInstalled) {
    ModelManagementCard(
      model = model,
      primaryActionLabel = "Delete",
      onPrimaryAction = onDelete,
      secondaryActionLabel = "Download",
      onSecondaryAction = onDownload,
      emphasizeSecondary = false,
      primaryActionIcon = Icons.Filled.Delete,
      secondaryActionIcon = Icons.Filled.Download,
      modifier = modifier,
    )
  } else {
    ModelManagementCard(
      model = model,
      primaryActionLabel = "Download",
      onPrimaryAction = onDownload,
      modifier = modifier,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ModelLibraryToolbar(
  filters: LibraryFilterState,
  providers: List<ProviderType>,
  capabilities: List<String>,
  hasActiveFilters: Boolean,
  onSearchChange: (String) -> Unit,
  onProviderSelect: (ProviderType?) -> Unit,
  onCapabilityToggle: (String) -> Unit,
  onSortSelect: (ModelSort) -> Unit,
  onClearFilters: () -> Unit,
) {
  var filtersExpanded by rememberSaveable { mutableStateOf(false) }
  val activeFilterCount = filters.activeFilterCount
  val badgeLabel =
    when {
      activeFilterCount <= 0 -> null
      activeFilterCount > 9 -> "9+"
      else -> activeFilterCount.toString()
    }

  Surface(shape = MaterialTheme.shapes.large, tonalElevation = 4.dp) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedTextField(
          value = filters.searchQuery,
          onValueChange = onSearchChange,
          modifier = Modifier.weight(1f).testTag(SEARCH_FIELD_TAG),
          singleLine = true,
          shape = MaterialTheme.shapes.large,
          placeholder = { Text("Search models") },
          leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
          trailingIcon =
            if (filters.searchQuery.isNotBlank()) {
              {
                IconButton(onClick = { onSearchChange("") }) {
                  Icon(Icons.Filled.Close, contentDescription = "Clear search query")
                }
              }
            } else {
              null
            },
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedContainerColor = MaterialTheme.colorScheme.surface,
              unfocusedContainerColor = MaterialTheme.colorScheme.surface,
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline,
              cursorColor = MaterialTheme.colorScheme.primary,
              focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
              unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
              focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
              unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
              focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
              unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )

        IconButton(
          modifier = Modifier.testTag(FILTER_TOGGLE_TAG),
          onClick = { filtersExpanded = !filtersExpanded },
        ) {
          BadgedBox(badge = { badgeLabel?.let { label -> Badge { Text(label) } } }) {
            Icon(
              imageVector = Icons.Outlined.Tune,
              contentDescription = if (filtersExpanded) "Collapse filters" else "Expand filters",
            )
          }
        }
      }

      AnimatedVisibility(visible = filtersExpanded || hasActiveFilters) {
        Surface(
          modifier = Modifier.fillMaxWidth().testTag(FILTER_PANEL_TAG),
          shape = MaterialTheme.shapes.large,
          tonalElevation = 1.dp,
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            FilterSection(title = "Provider") {
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                FilterChip(
                  selected = filters.provider == null,
                  onClick = { onProviderSelect(null) },
                  label = { Text("All providers") },
                )
                providers.forEach { provider ->
                  val isSelected = filters.provider == provider
                  FilterChip(
                    selected = isSelected,
                    onClick = { onProviderSelect(if (isSelected) null else provider) },
                    label = { Text(provider.displayName()) },
                  )
                }
              }
            }

            if (capabilities.isNotEmpty()) {
              HorizontalDivider()
              FilterSection(title = "Capabilities") {
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  capabilities.forEach { capability ->
                    val normalized = capability.lowercase(Locale.US)
                    val selected = normalized in filters.capabilities
                    AssistChip(
                      onClick = { onCapabilityToggle(capability) },
                      label = {
                        Text(
                          capability.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
                          }
                        )
                      },
                      leadingIcon =
                        if (selected) {
                          {
                            Icon(
                              Icons.Filled.CheckCircle,
                              contentDescription = null,
                              tint = MaterialTheme.colorScheme.primary
                            )
                          }
                        } else {
                          null
                        },
                    )
                  }
                }
              }
            }

            HorizontalDivider()
            FilterSection(title = "Sort order") {
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                ModelSort.entries.forEach { option ->
                  val selected = filters.sort == option
                  FilterChip(
                    selected = selected,
                    onClick = { if (!selected) onSortSelect(option) },
                    label = { Text(option.label()) },
                  )
                }
              }
            }

            if (hasActiveFilters) {
              HorizontalDivider()
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
              ) {
                TextButton(onClick = onClearFilters) { Text("Clear filters") }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
internal fun ModelLibraryContent(
  sections: ModelLibrarySections,
  onDownload: (ModelPackage) -> Unit,
  onDelete: (ModelPackage) -> Unit,
  onPause: (UUID) -> Unit,
  onResume: (UUID) -> Unit,
  onCancel: (UUID) -> Unit,
  onRetry: (UUID) -> Unit,
  modifier: Modifier = Modifier,
) {
  val listHasContent =
    sections.downloads.isNotEmpty() ||
      sections.attention.isNotEmpty() ||
      sections.installed.isNotEmpty() ||
      sections.available.isNotEmpty()

  LazyColumn(
    modifier = modifier.fillMaxWidth().testTag(LIST_TAG),
    contentPadding = PaddingValues(bottom = 32.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
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
          secondaryActionLabel = "Download",
          onSecondaryAction = { onDownload(model) },
          emphasizeSecondary = false,
          primaryActionIcon = Icons.Filled.Delete,
          secondaryActionIcon = Icons.Filled.Download,
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
        contentDescription = "Download progress ${percent}%"
      },
    trackColor = MaterialTheme.colorScheme.surface,
  )
}

@Composable
private fun ModelManagementCard(
  model: ModelPackage,
  primaryActionLabel: String,
  onPrimaryAction: () -> Unit,
  secondaryActionLabel: String? = null,
  onSecondaryAction: (() -> Unit)? = null,
  emphasizeSecondary: Boolean = true,
  primaryActionIcon: ImageVector = Icons.Filled.Download,
  secondaryActionIcon: ImageVector = Icons.Filled.Delete,
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
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = model.displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = model.providerType.displayName(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          CapabilityRow(capabilities = model.capabilities)
        }
        StatusBadge(state = model.installState)
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = "Version ${model.version}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = formatSize(model.sizeBytes),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = "Updated ${formatUpdated(model.updatedAt)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilledTonalButton(
          onClick = onPrimaryAction,
          modifier =
            Modifier.semantics {
              contentDescription = "${primaryActionLabel} ${model.displayName}".trim()
            },
        ) {
          Icon(primaryActionIcon, contentDescription = null)
          Spacer(modifier = Modifier.size(8.dp))
          Text(primaryActionLabel)
        }

        if (secondaryActionLabel != null && onSecondaryAction != null) {
          if (emphasizeSecondary) {
            OutlinedButton(
              onClick = onSecondaryAction,
              modifier =
                Modifier.semantics {
                  contentDescription = "${secondaryActionLabel} ${model.displayName}".trim()
                },
            ) {
              Icon(secondaryActionIcon, contentDescription = null)
              Spacer(modifier = Modifier.size(8.dp))
              Text(secondaryActionLabel)
            }
          } else {
            TextButton(
              onClick = onSecondaryAction,
              modifier =
                Modifier.semantics {
                  contentDescription = "${secondaryActionLabel} ${model.displayName}".trim()
                },
            ) {
              Icon(secondaryActionIcon, contentDescription = null)
              Spacer(modifier = Modifier.size(8.dp))
              Text(secondaryActionLabel)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FilterSection(
  title: String,
  content: @Composable () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    content()
  }
}

@Composable
private fun CapabilityRow(capabilities: Set<String>) {
  if (capabilities.isEmpty()) return
  FlowRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    capabilities.take(MAX_CAPABILITY_CHIPS).forEach { capability ->
      AssistChip(
        onClick = {},
        enabled = false,
        label = {
          Text(
            capability.replaceFirstChar {
              if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
            }
          )
        },
      )
    }
    val remaining = capabilities.size - MAX_CAPABILITY_CHIPS
    if (remaining > 0) {
      AssistChip(onClick = {}, enabled = false, label = { Text("+${remaining}") })
    }
  }
}

@Composable
private fun StatusBadge(state: InstallState) {
  val (label, color, icon) =
    when (state) {
      InstallState.INSTALLED ->
        Triple("Installed", MaterialTheme.colorScheme.primary, Icons.Filled.CheckCircle)
      InstallState.DOWNLOADING ->
        Triple("Downloading", MaterialTheme.colorScheme.tertiary, Icons.Filled.Download)
      InstallState.PAUSED ->
        Triple("Paused", MaterialTheme.colorScheme.secondary, Icons.Filled.Pause)
      InstallState.ERROR ->
        Triple("Error", MaterialTheme.colorScheme.error, Icons.Filled.Close)
      InstallState.NOT_INSTALLED ->
        Triple("Available", MaterialTheme.colorScheme.outline, Icons.Filled.Download)
    }
  val containerColor =
    if (state == InstallState.NOT_INSTALLED) {
      MaterialTheme.colorScheme.surfaceVariant
    } else {
      color.copy(alpha = 0.12f)
    }
  Surface(
    shape = MaterialTheme.shapes.extraLarge,
    color = containerColor,
    border = BorderStroke(width = 1.dp, color = color.copy(alpha = 0.4f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(icon, contentDescription = null, tint = color)
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = color,
      )
    }
  }
}

@Composable
private fun EmptyState() {
  Column(
    modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(text = "No models to show", style = MaterialTheme.typography.titleMedium)
    Text(
      text = "Adjust your filters or connect to the catalog to discover new runtimes.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}
