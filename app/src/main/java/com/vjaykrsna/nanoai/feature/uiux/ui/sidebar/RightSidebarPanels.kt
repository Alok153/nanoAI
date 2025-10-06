package com.vjaykrsna.nanoai.feature.uiux.ui.sidebar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellUiState
import com.vjaykrsna.nanoai.feature.uiux.state.ConnectivityStatus
import com.vjaykrsna.nanoai.feature.uiux.state.ModeCard
import com.vjaykrsna.nanoai.feature.uiux.state.ModeId
import com.vjaykrsna.nanoai.feature.uiux.state.PaletteSource
import com.vjaykrsna.nanoai.feature.uiux.state.ProgressJob
import com.vjaykrsna.nanoai.feature.uiux.state.RightPanel
import com.vjaykrsna.nanoai.feature.uiux.state.UiPreferenceSnapshot
import com.vjaykrsna.nanoai.feature.uiux.ui.progress.ProgressCenterPanel
import com.vjaykrsna.nanoai.feature.uiux.ui.shell.ShellUiEvent

/**
 * Right sidebar container rendering contextual panels such as model selector, progress center, and
 * quick settings shortcuts.
 */
@Composable
fun RightSidebarPanels(
  state: ShellUiState,
  onEvent: (ShellUiEvent) -> Unit,
  modifier: Modifier = Modifier,
) {
  val layout = state.layout
  val activePanel = layout.activeRightPanel ?: RightPanel.PROGRESS_CENTER

  Surface(modifier = modifier.testTag("right_sidebar_panels"), tonalElevation = 2.dp) {
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      RightSidebarHeader(
        activePanel = activePanel,
        isDrawerOpen = layout.isRightDrawerOpen,
        onClose = {
          if (layout.isRightDrawerOpen && layout.activeRightPanel != null) {
            onEvent(ShellUiEvent.ToggleRightDrawer(layout.activeRightPanel))
          }
        },
      )

      RightPanelSwitcher(
        activePanel = activePanel,
        onPanelSelected = { panel ->
          if (panel != activePanel) {
            onEvent(ShellUiEvent.ToggleRightDrawer(panel))
          }
        },
      )

      when (activePanel) {
        RightPanel.PROGRESS_CENTER ->
          ProgressPanel(
            jobs = layout.progressJobs,
            onRetry = { job -> onEvent(ShellUiEvent.QueueJob(job)) },
            onDismiss = { job -> onEvent(ShellUiEvent.CompleteJob(job.jobId)) },
          )
        RightPanel.MODEL_SELECTOR ->
          ModelSelectorPanel(
            activeMode = layout.activeMode,
            modeCards = state.modeCards,
            connectivity = layout.connectivity,
            onModeSelected = { modeId -> onEvent(ShellUiEvent.ModeSelected(modeId)) },
            onOpenPalette = {
              onEvent(ShellUiEvent.ShowCommandPalette(PaletteSource.QUICK_ACTION))
            },
            onOpenLibrary = { onEvent(ShellUiEvent.ModeSelected(ModeId.LIBRARY)) },
          )
        RightPanel.SETTINGS_SHORTCUT ->
          SettingsShortcutsPanel(
            preferences = state.preferences,
            onThemeSelected = { theme -> onEvent(ShellUiEvent.UpdateTheme(theme)) },
            onDensitySelected = { density -> onEvent(ShellUiEvent.UpdateDensity(density)) },
            onOpenSettings = { onEvent(ShellUiEvent.ModeSelected(ModeId.SETTINGS)) },
          )
      }
    }
  }
}

@Composable
private fun RightSidebarHeader(
  activePanel: RightPanel,
  isDrawerOpen: Boolean,
  onClose: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().testTag("right_panel_switcher"),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text =
        when (activePanel) {
          RightPanel.PROGRESS_CENTER -> "Progress center"
          RightPanel.MODEL_SELECTOR -> "Model controls"
          RightPanel.SETTINGS_SHORTCUT -> "Settings shortcuts"
        },
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
    )
    if (isDrawerOpen) {
      IconButton(
        onClick = onClose,
        modifier = Modifier.semantics { contentDescription = "Close contextual drawer" }
      ) {
        Icon(Icons.Outlined.Close, contentDescription = null)
      }
    }
  }
}

@Composable
private fun RightPanelSwitcher(
  activePanel: RightPanel,
  onPanelSelected: (RightPanel) -> Unit,
) {
  val panels = remember { RightPanel.entries }
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    panels.forEach { panel ->
      val (label, icon) = panelLabel(panel)
      FilterChip(
        selected = panel == activePanel,
        onClick = { onPanelSelected(panel) },
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        modifier = Modifier.weight(1f, fill = false)
      )
    }
  }
}

@Composable
private fun ProgressPanel(
  jobs: List<ProgressJob>,
  onRetry: (ProgressJob) -> Unit,
  onDismiss: (ProgressJob) -> Unit,
) {
  ProgressCenterPanel(
    jobs = jobs,
    onRetry = onRetry,
    onDismissJob = onDismiss,
    modifier = Modifier.fillMaxWidth().testTag("progress_center_panel"),
  )
}

@Composable
private fun ModelSelectorPanel(
  activeMode: ModeId,
  modeCards: List<ModeCard>,
  connectivity: ConnectivityStatus,
  onModeSelected: (ModeId) -> Unit,
  onOpenPalette: () -> Unit,
  onOpenLibrary: () -> Unit,
) {
  val activeCard = remember(activeMode, modeCards) { modeCards.firstOrNull { it.id == activeMode } }
  val supportedCards =
    remember(modeCards) { modeCards.filter { card -> card.id in MODEL_SELECTOR_SUPPORTED_MODES } }

  Column(
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier.testTag("model_selector_panel")
  ) {
    Text(
      text =
        "Adjust models for ${activeCard?.title ?: activeMode.name.lowercase().replaceFirstChar { it.uppercase() }}",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    if (supportedCards.isEmpty()) {
      Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
        Text(
          text = "Model controls are unavailable for this mode.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(12.dp),
        )
      }
    } else {
      ModeStrip(
        modeCards = supportedCards,
        activeMode = activeMode,
        onModeSelected = onModeSelected
      )
    }

    if (connectivity != ConnectivityStatus.ONLINE) {
      Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
        Text(
          text = "Offline: cloud-only models will queue until connection resumes.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(12.dp),
        )
      }
    }

    val options = modelOptionsForMode(activeMode)
    if (options.isEmpty()) {
      Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
        Text(
          text = "Switch to a supported mode to tune models.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(12.dp),
        )
      }
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

    OutlinedButton(
      onClick = onOpenLibrary,
      modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Open model library" }
    ) {
      Text("Manage installed models")
    }
  }
}

@Composable
private fun ModeStrip(
  modeCards: List<ModeCard>,
  activeMode: ModeId,
  onModeSelected: (ModeId) -> Unit,
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier.fillMaxWidth().testTag("mode_selector_strip"),
  ) {
    modeCards.forEach { card ->
      FilterChip(
        selected = card.id == activeMode,
        onClick = { onModeSelected(card.id) },
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
    tonalElevation = 1.dp,
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
        Icon(
          imageVector =
            if (option.availableOffline) Icons.Rounded.Download else Icons.Outlined.Cloud,
          contentDescription =
            if (option.availableOffline) "On-device capable" else "Requires connectivity",
          tint =
            if (option.availableOffline) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Button(
        onClick = { onActivate(option) },
        enabled = isAvailable,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(if (isAvailable) "Activate" else "Unavailable offline")
      }
    }
  }
}

@Composable
private fun SettingsShortcutsPanel(
  preferences: UiPreferenceSnapshot,
  onThemeSelected: (ThemePreference) -> Unit,
  onDensitySelected: (VisualDensity) -> Unit,
  onOpenSettings: () -> Unit,
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier.testTag("settings_shortcuts_panel")
  ) {
    Text(
      text = "Fine-tune appearance and layout instantly.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("Theme", style = MaterialTheme.typography.titleSmall)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemePreference.entries.forEach { theme ->
          FilterChip(
            selected = preferences.theme == theme,
            onClick = { if (preferences.theme != theme) onThemeSelected(theme) },
            label = { Text(themeLabel(theme)) },
            modifier = Modifier.testTag("theme_option_${theme.name.lowercase()}")
          )
        }
      }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("Density", style = MaterialTheme.typography.titleSmall)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        VisualDensity.entries.forEach { density ->
          FilterChip(
            selected = preferences.density == density,
            onClick = { if (preferences.density != density) onDensitySelected(density) },
            label = { Text(densityLabel(density)) },
            modifier = Modifier.testTag("density_option_${density.name.lowercase()}")
          )
        }
      }
    }

    OutlinedButton(
      onClick = onOpenSettings,
      modifier =
        Modifier.fillMaxWidth()
          .semantics { contentDescription = "Open settings screen" }
          .testTag("open_settings_button"),
    ) {
      Icon(Icons.Outlined.Settings, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text("Open settings")
    }
  }
}

private fun panelLabel(
  panel: RightPanel
): Pair<String, androidx.compose.ui.graphics.vector.ImageVector> =
  when (panel) {
    RightPanel.PROGRESS_CENTER -> "Progress" to Icons.Rounded.Download
    RightPanel.MODEL_SELECTOR -> "Models" to Icons.Rounded.Tune
    RightPanel.SETTINGS_SHORTCUT -> "Settings" to Icons.Outlined.Settings
  }

private fun modelOptionsForMode(modeId: ModeId): List<ModelOption> =
  when (modeId) {
    ModeId.CHAT ->
      listOf(
        ModelOption("Gemini Nano", "On-device assistant tuned for chat.", availableOffline = true),
        ModelOption(
          "Gemini Pro",
          "Cloud-enhanced responses and longer context.",
          availableOffline = false
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
          availableOffline = true
        ),
        ModelOption("Audio Studio", "Cloud mastering and cleanup.", availableOffline = false),
      )
    ModeId.CODE ->
      listOf(
        ModelOption(
          "Code Mentor",
          "Inline suggestions for Kotlin & Java.",
          availableOffline = true
        ),
        ModelOption("Code Expert", "Cloud analysis for large projects.", availableOffline = false),
      )
    ModeId.TRANSLATE ->
      listOf(
        ModelOption(
          "Polyglot Mini",
          "On-device translation for core languages.",
          availableOffline = true
        ),
        ModelOption("Polyglot Cloud", "Extended language coverage.", availableOffline = false),
      )
    else -> emptyList()
  }

private fun themeLabel(theme: ThemePreference): String =
  when (theme) {
    ThemePreference.LIGHT -> "Light"
    ThemePreference.DARK -> "Dark"
    ThemePreference.SYSTEM -> "System"
  }

private fun densityLabel(density: VisualDensity): String =
  when (density) {
    VisualDensity.DEFAULT -> "Comfortable"
    VisualDensity.COMPACT -> "Compact"
    VisualDensity.EXPANDED -> "Spacious"
  }

private data class ModelOption(
  val name: String,
  val description: String,
  val availableOffline: Boolean,
)

private val MODEL_SELECTOR_SUPPORTED_MODES: Set<ModeId> =
  setOf(ModeId.CHAT, ModeId.IMAGE, ModeId.AUDIO, ModeId.CODE, ModeId.TRANSLATE)
