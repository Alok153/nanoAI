package com.vjaykrsna.nanoai.feature.library.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage

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
  modifier: Modifier = Modifier,
  secondaryActionLabel: String? = null,
  onSecondaryAction: (() -> Unit)? = null,
  emphasizeSecondary: Boolean = true,
  primaryActionIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.Download,
  secondaryActionIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.Delete,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ModelManagementHeader(model)
      ModelManagementAuthor(model)
      CapabilityRow(capabilities = model.capabilities)
      ModelManagementMetadata(model)
      ModelSummaryText(model.summary)
      ModelManagementFooter(
        model = model,
        primaryActionLabel = primaryActionLabel,
        primaryActionIcon = primaryActionIcon,
        onPrimaryAction = onPrimaryAction,
        secondaryActionLabel = secondaryActionLabel,
        onSecondaryAction = onSecondaryAction,
        emphasizeSecondary = emphasizeSecondary,
        secondaryActionIcon = secondaryActionIcon,
      )
    }
  }
}

@Composable
private fun ModelManagementFooter(
  model: ModelPackage,
  primaryActionLabel: String,
  primaryActionIcon: androidx.compose.ui.graphics.vector.ImageVector,
  onPrimaryAction: () -> Unit,
  secondaryActionLabel: String?,
  onSecondaryAction: (() -> Unit)?,
  emphasizeSecondary: Boolean,
  secondaryActionIcon: androidx.compose.ui.graphics.vector.ImageVector,
) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    ModelDetailList(
      sizeText = formatSize(model.sizeBytes),
      updatedText = formatUpdated(model.updatedAt),
      modifier = Modifier.weight(1f),
    )
    ModelActionButtons(
      modelName = model.displayName,
      primaryActionLabel = primaryActionLabel,
      primaryActionIcon = primaryActionIcon,
      onPrimaryAction = onPrimaryAction,
      secondaryActionLabel = secondaryActionLabel,
      onSecondaryAction = onSecondaryAction,
      emphasizeSecondary = emphasizeSecondary,
      secondaryActionIcon = secondaryActionIcon,
    )
  }
}

@Composable
private fun ModelDetailList(sizeText: String, updatedText: String, modifier: Modifier = Modifier) {
  Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    ModelDetailText(sizeText)
    ModelDetailText(updatedText)
  }
}

@Composable
private fun ModelDetailText(value: String) {
  Text(
    text = value,
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

@Composable
private fun ModelActionButtons(
  modelName: String,
  primaryActionLabel: String,
  primaryActionIcon: androidx.compose.ui.graphics.vector.ImageVector,
  onPrimaryAction: () -> Unit,
  secondaryActionLabel: String?,
  onSecondaryAction: (() -> Unit)?,
  emphasizeSecondary: Boolean,
  secondaryActionIcon: androidx.compose.ui.graphics.vector.ImageVector,
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    FilledTonalButton(
      onClick = onPrimaryAction,
      modifier = Modifier.semantics { contentDescription = "$primaryActionLabel $modelName".trim() },
    ) {
      Icon(primaryActionIcon, contentDescription = null)
      Spacer(modifier = Modifier.size(8.dp))
      Text(primaryActionLabel)
    }

    if (secondaryActionLabel != null && onSecondaryAction != null) {
      val secondaryModifier =
        Modifier.semantics { contentDescription = "$secondaryActionLabel $modelName".trim() }
      if (emphasizeSecondary) {
        OutlinedButton(onClick = onSecondaryAction, modifier = secondaryModifier) {
          Icon(secondaryActionIcon, contentDescription = null)
          Spacer(modifier = Modifier.size(8.dp))
          Text(secondaryActionLabel)
        }
      } else {
        TextButton(onClick = onSecondaryAction, modifier = secondaryModifier) {
          Icon(secondaryActionIcon, contentDescription = null)
          Spacer(modifier = Modifier.size(8.dp))
          Text(secondaryActionLabel)
        }
      }
    }
  }
}
