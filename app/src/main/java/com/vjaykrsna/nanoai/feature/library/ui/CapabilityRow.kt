@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.vjaykrsna.nanoai.feature.library.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
internal fun CapabilityRow(capabilities: Collection<String>) {
  val displayTags = remember(capabilities) { sanitizeCapabilitiesForDisplay(capabilities) }
  if (displayTags.isEmpty()) return

  val scrollState = rememberScrollState()
  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    displayTags.take(ModelLibraryUiConstants.MAX_CAPABILITY_CHIPS).forEach { capability ->
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
    val remaining = displayTags.size - ModelLibraryUiConstants.MAX_CAPABILITY_CHIPS
    if (remaining > 0) {
      AssistChip(onClick = {}, enabled = false, label = { Text("+${remaining}") })
    }
  }
}

private fun sanitizeCapabilitiesForDisplay(raw: Collection<String>): List<String> {
  if (raw.isEmpty()) return emptyList()
  val normalized = raw.map { it.trim() }.filter { it.isNotEmpty() }
  val deduplicated = normalized.distinctBy { it.lowercase() }
  val hasMultimodal = deduplicated.any { it.equals("multimodal", ignoreCase = true) }
  return deduplicated.filterNot { hasMultimodal && it.equals("text-generation", ignoreCase = true) }
}
