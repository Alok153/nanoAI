@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.vjaykrsna.nanoai.feature.library.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.domain.HuggingFaceModelSummary
import com.vjaykrsna.nanoai.feature.library.domain.InstallState
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryUiConstants.COUNT_DECIMAL_THRESHOLD
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryUiConstants.COUNT_FORMAT_THRESHOLD_LONG
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryUiConstants.COUNT_INTEGER_CHECK
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryUiConstants.MAX_CAPABILITY_CHIPS
import java.util.Locale

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

@Composable
internal fun ModelManagementCard(
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
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // Top row: title and status
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Text(
          text = model.displayName,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        StatusBadge(state = model.installState)
      }

      // Author
      model.author
        ?.takeIf { it.isNotBlank() }
        ?.let { author ->
          Text(
            text = author,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        ?: Text(
          text = model.providerType.displayName(),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

      // Capabilities
      CapabilityRow(capabilities = model.capabilities)

      // Metadata
      val allTags = buildList {
        model.license?.takeIf { it.isNotBlank() }?.let { add("License: $it") }
        if (model.architectures.isNotEmpty()) {
          add("Arch: ${model.architectures.joinToString(", ")}")
        }
        model.modelType?.takeIf { it.isNotBlank() }?.let { add("Type: $it") }
        if (model.languages.isNotEmpty()) {
          add("Lang: ${model.languages.joinToString(", ")}")
        }
      }
      if (allTags.isNotEmpty()) {
        Text(
          text = allTags.joinToString(" • "),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      // Summary
      model.summary
        ?.takeIf { it.isNotBlank() }
        ?.let { summary ->
          Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
          )
        }

      // Bottom row: size, updated, buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = formatSize(model.sizeBytes),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = formatUpdated(model.updatedAt),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
}

@Composable
internal fun HuggingFaceModelCard(
  model: HuggingFaceModelSummary,
  isDownloadable: Boolean = false,
  onDownload: (() -> Unit)? = null,
) {
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
        val allTags = buildList {
          pipelineTag?.let { add(it) }
          libraryName?.let { add(it) }
          addAll(model.tags)
        }
        CapabilityRow(capabilities = allTags)
      }

      Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            Icons.Filled.Download,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "${formatCount(model.downloads)} downloads",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            Icons.Filled.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
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
          text = formatUpdated(lastModified),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (model.createdAt != null && model.lastModified == null) {
        Text(
          text = formatUpdated(model.createdAt),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // Download button - enabled for compatible models, disabled for incompatible ones
      OutlinedButton(
        onClick =
          if (isDownloadable && onDownload != null) onDownload
          else {
            {}
          },
        enabled = isDownloadable && onDownload != null,
        modifier =
          Modifier.semantics {
            contentDescription =
              if (isDownloadable) "Download ${model.displayName}"
              else "Model ${model.displayName} is not supported on this device"
          },
      ) {
        Icon(Icons.Filled.Download, contentDescription = null)
        Spacer(modifier = Modifier.size(8.dp))
        Text(if (isDownloadable) "Download" else "Unsupported")
      }
    }
  }
}

@Composable
private fun CapabilityRow(capabilities: Collection<String>) {
  val displayTags = remember(capabilities) { sanitizeCapabilitiesForDisplay(capabilities) }
  if (displayTags.isEmpty()) return

  val scrollState = rememberScrollState()
  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
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

private fun formatCount(value: Long): String {
  if (value < COUNT_FORMAT_THRESHOLD_LONG) return value.toString()

  val units = arrayOf("k", "M", "B", "T")
  var remainder = value.toDouble()
  var unitIndex = 0
  while (remainder >= COUNT_FORMAT_THRESHOLD_LONG && unitIndex < units.lastIndex) {
    remainder /= COUNT_FORMAT_THRESHOLD_LONG
    unitIndex += 1
  }

  val pattern =
    if (remainder >= COUNT_DECIMAL_THRESHOLD || remainder % COUNT_INTEGER_CHECK == 0.0) "%.0f%s"
    else "%.1f%s"
  return pattern.format(Locale.US, remainder, units[unitIndex])
}

private fun sanitizeCapabilitiesForDisplay(raw: Collection<String>): List<String> {
  if (raw.isEmpty()) return emptyList()
  val normalized = raw.map { it.trim() }.filter { it.isNotEmpty() }
  val deduplicated = normalized.distinctBy { it.lowercase() }
  val hasMultimodal = deduplicated.any { it.equals("multimodal", ignoreCase = true) }
  return deduplicated.filterNot { hasMultimodal && it.equals("text-generation", ignoreCase = true) }
}
