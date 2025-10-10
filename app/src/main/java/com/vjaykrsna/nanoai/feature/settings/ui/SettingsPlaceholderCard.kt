package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vjaykrsna.nanoai.feature.uiux.ui.components.primitives.NanoCard

@Composable
internal fun SettingsPlaceholderCard(
  description: String,
  supportingText: String,
  modifier: Modifier = Modifier,
) {
  NanoCard(
    title = "Foundation in progress",
    supportingText = description,
    supportingContent = {
      Text(
        text = supportingText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    },
    modifier = modifier.fillMaxWidth(),
    enabled = false,
    semanticsDescription = "Placeholder section: $description",
  )
}
