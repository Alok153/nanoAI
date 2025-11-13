package com.vjaykrsna.nanoai.feature.library.ui

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState

@Composable
internal fun ModelManagementHeader(model: ModelPackage) {
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
}

@Composable
internal fun ModelManagementAuthor(model: ModelPackage) {
  val author = model.author?.takeIf { it.isNotBlank() }
  val fallback = model.providerType.displayName()
  Text(
    text = author ?: fallback,
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

@Composable
internal fun ModelManagementMetadata(model: ModelPackage) {
  val metadata = remember(model) { buildModelMetadataTags(model) }
  if (metadata.isEmpty()) return

  Text(
    text = metadata.joinToString(" • "),
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis,
  )
}

@VisibleForTesting
internal fun buildModelMetadataTags(model: ModelPackage): List<String> {
  return buildList {
    model.license?.takeIf { it.isNotBlank() }?.let { add("License: $it") }
    if (model.architectures.isNotEmpty()) add("Arch: ${model.architectures.joinToString(", ")}")
    model.modelType?.takeIf { it.isNotBlank() }?.let { add("Type: $it") }
    if (model.languages.isNotEmpty()) add("Lang: ${model.languages.joinToString(", ")}")
  }
}

@Composable
internal fun ModelSummaryText(summary: String?) {
  summary
    ?.takeIf { it.isNotBlank() }
    ?.let { content ->
      Text(
        text = content,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
      )
    }
}

@Composable
internal fun StatusBadge(state: InstallState) {
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
