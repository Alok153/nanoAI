package com.vjaykrsna.nanoai.feature.uiux.ui.sidebar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.model.PersonaSwitchAction
import com.vjaykrsna.nanoai.feature.uiux.presentation.ModeId
import com.vjaykrsna.nanoai.feature.uiux.presentation.PaletteSource
import com.vjaykrsna.nanoai.feature.uiux.presentation.RightPanel
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellUiState
import com.vjaykrsna.nanoai.shared.ui.shell.ShellUiEvent

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
  val activePanel = layout.activeRightPanel ?: RightPanel.MODEL_SELECTOR

  Surface(
    modifier =
      modifier.testTag("right_sidebar_panels").semantics {
        stateDescription =
          when (activePanel) {
            RightPanel.MODEL_SELECTOR -> "Model controls"
            RightPanel.SETTINGS_SHORTCUT -> "Settings shortcuts"
          }
      },
    tonalElevation = 2.dp,
  ) {
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
        onPanelSelect = { panel ->
          if (panel != activePanel) {
            onEvent(ShellUiEvent.ToggleRightDrawer(panel))
          }
        },
      )

      when (activePanel) {
        RightPanel.MODEL_SELECTOR ->
          if (layout.activeMode == ModeId.CHAT && state.chatState != null) {
            ChatModelSelectorPanel(
              availablePersonas = state.chatState.availablePersonas,
              currentPersonaId = state.chatState.currentPersonaId,
              onPersonaSelect = { persona ->
                onEvent(
                  ShellUiEvent.ChatPersonaSelected(
                    persona.personaId,
                    com.vjaykrsna.nanoai.core.model.PersonaSwitchAction.CONTINUE_THREAD,
                  )
                )
              },
            )
          } else {
            ModelSelectorPanel(
              activeMode = layout.activeMode,
              modeCards = state.modeCards,
              connectivity = layout.connectivity,
              onModeSelect = { modeId -> onEvent(ShellUiEvent.ModeSelected(modeId)) },
              onOpenPalette = {
                onEvent(ShellUiEvent.ShowCommandPalette(PaletteSource.QUICK_ACTION))
              },
              onOpenLibrary = { onEvent(ShellUiEvent.ModeSelected(ModeId.LIBRARY)) },
            )
          }
        RightPanel.SETTINGS_SHORTCUT ->
          SettingsShortcutsPanel(
            preferences = state.preferences,
            onThemeSelect = { theme -> onEvent(ShellUiEvent.UpdateTheme(theme)) },
            onDensitySelect = { density -> onEvent(ShellUiEvent.UpdateDensity(density)) },
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
          RightPanel.MODEL_SELECTOR -> "Model controls"
          RightPanel.SETTINGS_SHORTCUT -> "Settings shortcuts"
        },
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.semantics { heading() },
    )
    if (isDrawerOpen) {
      IconButton(
        onClick = onClose,
        modifier = Modifier.semantics { contentDescription = "Close contextual drawer" },
      ) {
        Icon(Icons.Outlined.Close, contentDescription = null)
      }
    }
  }
}

@Composable
private fun RightPanelSwitcher(activePanel: RightPanel, onPanelSelect: (RightPanel) -> Unit) {
  val panels = remember { RightPanel.entries }
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    panels.forEach { panel ->
      val (label, icon) = panelLabel(panel)
      FilterChip(
        selected = panel == activePanel,
        onClick = { onPanelSelect(panel) },
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = "$label panel") },
        modifier =
          Modifier.weight(1f, fill = false).semantics {
            role = Role.Tab
            stateDescription = if (panel == activePanel) "Selected" else "Not selected"
          },
      )
    }
  }
}

private fun panelLabel(
  panel: RightPanel
): Pair<String, androidx.compose.ui.graphics.vector.ImageVector> =
  when (panel) {
    RightPanel.MODEL_SELECTOR -> "Models" to Icons.Rounded.Tune
    RightPanel.SETTINGS_SHORTCUT -> "Settings" to Icons.Outlined.Settings
  }
