package com.vjaykrsna.nanoai.feature.library.ui

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceModelSummary
import java.util.Locale

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
      HuggingFaceModelHeader(model)
      HuggingFaceModelStats(model)
      HuggingFaceModelMetadata(model)
      ModelSummaryText(model.summary)
      HuggingFaceDownloadButton(
        model = model,
        isDownloadable = isDownloadable,
        onDownload = onDownload,
      )
    }
  }
}

@Composable
internal fun HuggingFaceModelHeader(model: HuggingFaceModelSummary) {
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
    val capabilityTags = buildCapabilityTags(model)
    CapabilityRow(capabilities = capabilityTags)
  }
}

@Composable
internal fun HuggingFaceModelStats(model: HuggingFaceModelSummary) {
  Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
    ModelStatRow(
      icon = Icons.Filled.Download,
      tint = MaterialTheme.colorScheme.primary,
      text = "${formatCount(model.downloads)} downloads",
    )
    ModelStatRow(
      icon = Icons.Filled.Star,
      tint = MaterialTheme.colorScheme.secondary,
      text = "${formatCount(model.likes)} likes",
    )
  }
}

@Composable
private fun ModelStatRow(icon: ImageVector, tint: Color, text: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Icon(icon, contentDescription = null, tint = tint)
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
internal fun HuggingFaceModelMetadata(model: HuggingFaceModelSummary) {
  val metadata = remember(model) { buildMetadataLines(model) }
  metadata.forEach { line ->
    Text(
      text = line,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
internal fun HuggingFaceDownloadButton(
  model: HuggingFaceModelSummary,
  isDownloadable: Boolean,
  onDownload: (() -> Unit)?,
) {
  val action = onDownload ?: {}
  OutlinedButton(
    onClick = action,
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

@VisibleForTesting
internal fun buildCapabilityTags(model: HuggingFaceModelSummary): List<String> {
  val pipelineTag = model.pipelineTag?.takeIf { it.isNotBlank() }
  val libraryName = model.libraryName?.takeIf { it.isNotBlank() }
  return buildList {
    pipelineTag?.let { add(it) }
    libraryName?.let { add(it) }
    addAll(model.tags)
  }
}

@VisibleForTesting
internal fun buildMetadataLines(model: HuggingFaceModelSummary): List<String> {
  val updatedText = model.lastModified?.let { formatUpdated(it) }
  val createdText =
    model.createdAt
      ?.let { formatUpdated(it) }
      ?.takeUnless { value -> updatedText != null && value == updatedText }
  return buildList {
    model.license?.takeIf { it.isNotBlank() }?.let { add("License: ${it}") }
    if (model.languages.isNotEmpty()) add("Languages: ${model.languages.joinToString(", ")}")
    model.baseModel?.takeIf { it.isNotBlank() }?.let { add("Base model: ${it}") }
    if (model.architectures.isNotEmpty())
      add("Architectures: ${model.architectures.joinToString(", ")}")
    model.modelType?.takeIf { it.isNotBlank() }?.let { add("Type: ${it}") }
    model.totalSizeBytes?.let { add("Size: ${formatSize(it)}") }
    updatedText?.let { add(it) }
    createdText?.let { add(it) }
  }
}

private fun formatCount(value: Long): String {
  if (value < ModelLibraryUiConstants.COUNT_FORMAT_THRESHOLD_LONG) return value.toString()

  val units = arrayOf("k", "M", "B", "T")
  var remainder = value.toDouble()
  var unitIndex = 0
  while (
    remainder >= ModelLibraryUiConstants.COUNT_FORMAT_THRESHOLD_LONG && unitIndex < units.lastIndex
  ) {
    remainder /= ModelLibraryUiConstants.COUNT_FORMAT_THRESHOLD_LONG
    unitIndex += 1
  }

  val pattern =
    if (
      remainder >= ModelLibraryUiConstants.COUNT_DECIMAL_THRESHOLD ||
        remainder % ModelLibraryUiConstants.COUNT_INTEGER_CHECK == 0.0
    )
      "%.0f%s"
    else "%.1f%s"
  return pattern.format(Locale.US, remainder, units[unitIndex])
}
