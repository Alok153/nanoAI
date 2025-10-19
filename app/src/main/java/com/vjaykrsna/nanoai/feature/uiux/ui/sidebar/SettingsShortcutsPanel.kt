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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.R
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
      text = stringResource(R.string.settings_shortcuts_panel_description),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    ShortcutThemeSelector(preferences.theme, onThemeSelect)
    ShortcutDensitySelector(preferences.density, onDensitySelect)

    val openSettingsContentDescription =
      stringResource(R.string.settings_shortcuts_panel_open_settings_content_description)

    OutlinedButton(
      onClick = onOpenSettings,
      modifier =
        Modifier.fillMaxWidth()
          .semantics { contentDescription = openSettingsContentDescription }
          .testTag("open_settings_button"),
    ) {
      androidx.compose.material3.Icon(Icons.Outlined.Settings, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text(stringResource(R.string.settings_shortcuts_panel_open_settings))
    }
  }
}

@Composable
private fun ShortcutThemeSelector(
  selected: ThemePreference,
  onSelect: (ThemePreference) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      stringResource(R.string.settings_shortcuts_panel_theme),
      style = MaterialTheme.typography.titleSmall
    )
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
    Text(
      stringResource(R.string.settings_shortcuts_panel_density),
      style = MaterialTheme.typography.titleSmall
    )
    VisualDensityChips(
      selected = selected,
      onSelect = onSelect,
      modifier = Modifier.fillMaxWidth(),
      onUnsupportedSelect = null,
    )
  }
}
