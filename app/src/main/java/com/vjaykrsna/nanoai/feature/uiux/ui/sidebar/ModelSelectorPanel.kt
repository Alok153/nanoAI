package com.vjaykrsna.nanoai.feature.uiux.ui.sidebar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.R
import com.vjaykrsna.nanoai.feature.uiux.presentation.ConnectivityStatus
import com.vjaykrsna.nanoai.feature.uiux.presentation.ModeCard
import com.vjaykrsna.nanoai.feature.uiux.presentation.ModeId
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoElevation

@Composable
internal fun ModelSelectorPanel(
  activeMode: ModeId,
  modeCards: List<ModeCard>,
  connectivity: ConnectivityStatus,
  onModeSelect: (ModeId) -> Unit,
  onOpenPalette: () -> Unit,
  onOpenLibrary: () -> Unit,
) {
  val activeCard = remember(activeMode, modeCards) { modeCards.firstOrNull { it.id == activeMode } }
  val supportedCards =
    remember(modeCards) { modeCards.filter { card -> card.id in MODEL_SELECTOR_SUPPORTED_MODES } }

  Column(
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier.testTag("model_selector_panel"),
  ) {
    val fallbackModeName =
      remember(activeMode) { activeMode.name.lowercase().replaceFirstChar { it.uppercase() } }
    val headerLabel =
      stringResource(
        R.string.model_selector_panel_adjust_models,
        activeCard?.title ?: fallbackModeName,
      )
    Text(
      text = headerLabel,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )

    if (supportedCards.isEmpty()) {
      InfoCard(stringResource(R.string.model_selector_panel_controls_unavailable))
    } else {
      ModeStrip(modeCards = supportedCards, activeMode = activeMode, onModeSelect = onModeSelect)
    }

    if (connectivity != ConnectivityStatus.ONLINE) {
      InfoCard(stringResource(R.string.model_selector_panel_offline_message))
    }

    val options = modelOptionsForMode(activeMode)
    if (options.isEmpty()) {
      InfoCard(stringResource(R.string.model_selector_panel_switch_mode_message))
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEach { option ->
          ModelOptionCard(
            option = option,
            connectivity = connectivity,
            onActivate = { onOpenPalette() },
          )
        }
      }
    }

    val openLibraryContentDescription =
      stringResource(R.string.model_selector_panel_open_library_content_description)

    OutlinedButton(
      onClick = onOpenLibrary,
      modifier =
        Modifier.fillMaxWidth().semantics { contentDescription = openLibraryContentDescription },
    ) {
      Text(stringResource(R.string.model_selector_panel_manage_models))
    }
  }
}

@Composable
private fun InfoCard(message: String) {
  Surface(tonalElevation = NanoElevation.level1, shape = MaterialTheme.shapes.medium) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(12.dp),
    )
  }
}

@Composable
private fun ModeStrip(
  modeCards: List<ModeCard>,
  activeMode: ModeId,
  onModeSelect: (ModeId) -> Unit,
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier.fillMaxWidth().testTag("mode_selector_strip"),
  ) {
    modeCards.forEach { card ->
      FilterChip(
        selected = card.id == activeMode,
        onClick = { onModeSelect(card.id) },
        label = { Text(card.title) },
      )
    }
  }
}

@Composable
private fun ModelOptionCard(
  option: ModelOption,
  connectivity: ConnectivityStatus,
  onActivate: (ModelOption) -> Unit,
) {
  val isAvailable = option.availableOffline || connectivity == ConnectivityStatus.ONLINE
  Surface(
    tonalElevation = NanoElevation.level1,
    shape = MaterialTheme.shapes.medium,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(option.name, style = MaterialTheme.typography.titleSmall)
          Text(
            text = option.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        val (icon, description) = optionAvailabilityIcon(option)
        Icon(imageVector = icon, contentDescription = description)
      }

      Button(
        onClick = { onActivate(option) },
        enabled = isAvailable,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          if (isAvailable) stringResource(R.string.model_selector_panel_activate)
          else stringResource(R.string.model_selector_panel_unavailable_offline)
        )
      }
    }
  }
}

@Composable
private fun optionAvailabilityIcon(
  option: ModelOption
): Pair<androidx.compose.ui.graphics.vector.ImageVector, String> =
  if (option.availableOffline) {
    Icons.Rounded.Download to stringResource(R.string.model_selector_panel_on_device_capable)
  } else {
    Icons.Outlined.Cloud to stringResource(R.string.model_selector_panel_requires_connectivity)
  }

private fun modelOptionsForMode(modeId: ModeId): List<ModelOption> =
  when (modeId) {
    ModeId.CHAT ->
      listOf(
        ModelOption("Gemini Nano", "On-device assistant tuned for chat.", availableOffline = true),
        ModelOption(
          "Gemini Pro",
          "Cloud-enhanced responses and longer context.",
          availableOffline = false,
        ),
      )
    ModeId.IMAGE ->
      listOf(
        ModelOption("Imagen Lite", "Fast text-to-image rendering.", availableOffline = false),
        ModelOption("Imagen Micro", "Low bandwidth preset for sketches.", availableOffline = true),
      )
    ModeId.AUDIO ->
      listOf(
        ModelOption(
          "Audio Scribe",
          "On-device transcription for meetings.",
          availableOffline = true,
        ),
        ModelOption("Audio Studio", "Cloud mastering and cleanup.", availableOffline = false),
      )
    ModeId.CODE ->
      listOf(
        ModelOption(
          "Code Mentor",
          "Inline suggestions for Kotlin & Java.",
          availableOffline = true,
        ),
        ModelOption("Code Expert", "Cloud analysis for large projects.", availableOffline = false),
      )
    ModeId.TRANSLATE ->
      listOf(
        ModelOption(
          "Polyglot Mini",
          "On-device translation for core languages.",
          availableOffline = true,
        ),
        ModelOption("Polyglot Cloud", "Extended language coverage.", availableOffline = false),
      )
    else -> emptyList()
  }

private data class ModelOption(
  val name: String,
  val description: String,
  val availableOffline: Boolean,
)

private val MODEL_SELECTOR_SUPPORTED_MODES: Set<ModeId> =
  setOf(ModeId.CHAT, ModeId.IMAGE, ModeId.AUDIO, ModeId.CODE, ModeId.TRANSLATE)
