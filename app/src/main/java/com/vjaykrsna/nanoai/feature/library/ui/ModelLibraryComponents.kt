@file:OptIn(ExperimentalLayoutApi::class)

package com.vjaykrsna.nanoai.feature.library.ui

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.vjaykrsna.nanoai.feature.library.domain.model.HuggingFaceModelSummary
import com.vjaykrsna.nanoai.feature.library.model.DownloadStatus
import com.vjaykrsna.nanoai.feature.library.model.InstallState
import com.vjaykrsna.nanoai.feature.library.model.ProviderType
import com.vjaykrsna.nanoai.feature.library.presentation.HuggingFaceFilterState
import com.vjaykrsna.nanoai.feature.library.presentation.HuggingFaceSortOption
import com.vjaykrsna.nanoai.feature.library.presentation.LibraryDownloadItem
import com.vjaykrsna.nanoai.feature.library.presentation.LibraryFilterState
import com.vjaykrsna.nanoai.feature.library.presentation.ModelLibrarySections
import com.vjaykrsna.nanoai.feature.library.presentation.ModelLibraryTab
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
      primaryActionIcon = Icons.Filled.Delete,
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
  showModelFilters: Boolean,
  onSearchChange: (String) -> Unit,
  onProviderSelect: (ProviderType?) -> Unit,
  onCapabilityToggle: (String) -> Unit,
  onSortSelect: (ModelSort) -> Unit,
  onClearFilters: () -> Unit,
) {
  var filtersExpanded by rememberSaveable { mutableStateOf(false) }
  if (!showModelFilters && filtersExpanded) filtersExpanded = false
  val activeFilterCount = if (showModelFilters) filters.activeFilterCount else 0
  val badgeLabel =
    when {
      !showModelFilters || activeFilterCount <= 0 -> null
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

        if (showModelFilters) {
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
      }

      AnimatedVisibility(visible = showModelFilters && (filtersExpanded || hasActiveFilters)) {
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
              val providerScroll = rememberScrollState()
              Row(
                modifier = Modifier.horizontalScroll(providerScroll),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                val capabilityScroll = rememberScrollState()
                Row(
                  modifier = Modifier.horizontalScroll(capabilityScroll),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
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
              val sortScroll = rememberScrollState()
              Row(
                modifier = Modifier.horizontalScroll(sortScroll),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
internal fun HuggingFaceFilterBar(
  filters: HuggingFaceFilterState,
  pipelineOptions: List<String>,
  libraryOptions: List<String>,
  onSortSelect: (HuggingFaceSortOption) -> Unit,
  onPipelineSelect: (String?) -> Unit,
  onLibrarySelect: (String?) -> Unit,
  onClearFilters: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(shape = MaterialTheme.shapes.large, tonalElevation = 2.dp, modifier = modifier) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      FilterSection(title = "Sort order") {
        val scroll = rememberScrollState()
        Row(
          modifier = Modifier.horizontalScroll(scroll),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          HuggingFaceSortOption.entries.forEach { option ->
            val selected = filters.sort == option
            FilterChip(
              selected = selected,
              label = { Text(option.label()) },
              onClick = { if (!selected) onSortSelect(option) },
            )
          }
        }
      }

      if (pipelineOptions.isNotEmpty()) {
        HorizontalDivider()
        FilterSection(title = "Pipeline") {
          val scroll = rememberScrollState()
          Row(
            modifier = Modifier.horizontalScroll(scroll),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FilterChip(
              selected = filters.pipelineTag == null,
              label = { Text("All pipelines") },
              onClick = { onPipelineSelect(null) },
            )
            pipelineOptions.forEach { pipeline ->
              val selected = filters.pipelineTag == pipeline
              FilterChip(
                selected = selected,
                label = { Text(pipeline) },
                onClick = { onPipelineSelect(if (selected) null else pipeline) },
              )
            }
          }
        }
      }

      if (libraryOptions.isNotEmpty()) {
        HorizontalDivider()
        FilterSection(title = "Library") {
          val scroll = rememberScrollState()
          Row(
            modifier = Modifier.horizontalScroll(scroll),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FilterChip(
              selected = filters.library == null,
              label = { Text("All libraries") },
              onClick = { onLibrarySelect(null) },
            )
            libraryOptions.forEach { library ->
              val selected = filters.library == library
              FilterChip(
                selected = selected,
                label = { Text(library) },
                onClick = { onLibrarySelect(if (selected) null else library) },
              )
            }
          }
        }
      }

      if (filters.hasActiveFilters) {
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

@Composable
internal fun ModelLibraryTabs(
  selectedTab: ModelLibraryTab,
  onTabSelected: (ModelLibraryTab) -> Unit,
  modifier: Modifier = Modifier,
) {
  val tabs = ModelLibraryTab.entries
  val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
  TabRow(selectedTabIndex = selectedIndex, modifier = modifier.fillMaxWidth()) {
    tabs.forEach { tab ->
      Tab(
        selected = tab == selectedTab,
        onClick = { onTabSelected(tab) },
        text = { Text(tab.label) },
      )
    }
  }
}

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
  if (isLoading && models.isEmpty()) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator()
    }
    return
  }

  if (models.isEmpty()) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
      EmptyState(
        title = "No Hugging Face models found",
        message = "Try a different search term or explore the curated tab.",
      )
    }
    return
  }

  LazyColumn(
    modifier = modifier.fillMaxWidth().testTag(LIST_TAG),
    contentPadding = PaddingValues(bottom = 32.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    items(models, key = { it.modelId }) { model -> HuggingFaceModelCard(model) }
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

@Composable
private fun HuggingFaceModelCard(model: HuggingFaceModelSummary) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = model.displayName,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        model.author
          ?.takeIf { it.isNotBlank() }
          ?.let { author ->
            Text(
              text = "By ${author}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        val pipelineTag = model.pipelineTag?.takeIf { it.isNotBlank() }
        val libraryName = model.libraryName?.takeIf { it.isNotBlank() }
        if (pipelineTag != null || libraryName != null) {
          val descriptorScroll = rememberScrollState()
          Row(
            modifier = Modifier.horizontalScroll(descriptorScroll),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            pipelineTag?.let { pipeline ->
              AssistChip(onClick = {}, enabled = false, label = { Text(pipeline) })
            }
            libraryName?.let { library ->
              AssistChip(onClick = {}, enabled = false, label = { Text(library) })
            }
          }
        }
        CapabilityRow(capabilities = model.tags)
      }

      Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(
            Icons.Filled.Download,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "${formatCount(model.downloads)} downloads",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(
            Icons.Filled.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary
          )
          Text(
            text = "${formatCount(model.likes)} likes",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      model.lastModified?.let { lastModified ->
        Text(
          text = "Updated ${formatUpdated(lastModified)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (model.createdAt != null && model.lastModified == null) {
        Text(
          text = "Published ${formatUpdated(model.createdAt)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
private fun CapabilityRow(capabilities: Collection<String>) {
  val displayTags = remember(capabilities) { sanitizeCapabilitiesForDisplay(capabilities) }
  if (displayTags.isEmpty()) return

  val scrollState = rememberScrollState()
  Row(
    modifier = Modifier.horizontalScroll(scrollState),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    displayTags.take(MAX_CAPABILITY_CHIPS).forEach { capability ->
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
    val remaining = displayTags.size - MAX_CAPABILITY_CHIPS
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
      InstallState.ERROR -> Triple("Error", MaterialTheme.colorScheme.error, Icons.Filled.Close)
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

private fun formatCount(value: Long): String {
  if (value < 1_000) return value.toString()

  val units = arrayOf("k", "M", "B", "T")
  var remainder = value.toDouble()
  var unitIndex = 0
  while (remainder >= 1_000 && unitIndex < units.lastIndex) {
    remainder /= 1_000
    unitIndex += 1
  }

  val pattern = if (remainder >= 10 || remainder % 1.0 == 0.0) "%.0f%s" else "%.1f%s"
  return pattern.format(Locale.US, remainder, units[unitIndex])
}

private fun sanitizeCapabilitiesForDisplay(raw: Collection<String>): List<String> {
  if (raw.isEmpty()) return emptyList()
  val normalized = raw.map { it.trim() }.filter { it.isNotEmpty() }
  val hasMultimodal = normalized.any { it.equals("multimodal", ignoreCase = true) }
  return normalized.filterNot { hasMultimodal && it.equals("text-generation", ignoreCase = true) }
}
