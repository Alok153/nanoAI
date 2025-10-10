package com.vjaykrsna.nanoai.feature.uiux.ui.sidebar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.feature.uiux.state.UiPreferenceSnapshot
import com.vjaykrsna.nanoai.feature.uiux.ui.components.ThemePreferenceChips
import com.vjaykrsna.nanoai.feature.uiux.ui.components.VisualDensityChips

@Composable
internal fun SettingsShortcutsPanel(
  preferences: UiPreferenceSnapshot,
  onThemeSelect: (ThemePreference) -> Unit,
  onDensitySelect: (VisualDensity) -> Unit,
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

    ShortcutThemeSelector(preferences.theme, onThemeSelect)
    ShortcutDensitySelector(preferences.density, onDensitySelect)

    OutlinedButton(
      onClick = onOpenSettings,
      modifier =
        Modifier.fillMaxWidth()
          .semantics { contentDescription = "Open settings screen" }
          .testTag("open_settings_button"),
    ) {
      androidx.compose.material3.Icon(Icons.Outlined.Settings, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text("Open settings")
    }
  }
}

@Composable
private fun ShortcutThemeSelector(
  selected: ThemePreference,
  onSelect: (ThemePreference) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("Theme", style = MaterialTheme.typography.titleSmall)
    ThemePreferenceChips(
      selected = selected,
      onSelect = onSelect,
      chipModifier = { theme -> Modifier.testTag("theme_option_${theme.name.lowercase()}") },
    )
  }
}

@Composable
private fun ShortcutDensitySelector(
  selected: VisualDensity,
  onSelect: (VisualDensity) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("Density", style = MaterialTheme.typography.titleSmall)
    VisualDensityChips(
      selected = selected,
      onSelect = onSelect,
      modifier = Modifier.fillMaxWidth(),
      onUnsupportedSelect = null,
    )
  }
}
