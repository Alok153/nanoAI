package com.vjaykrsna.nanoai.feature.uiux.ui.sidebar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FilterChip
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

    ThemeSelector(preferences.theme, onThemeSelect)
    DensitySelector(preferences.density, onDensitySelect)

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
private fun ThemeSelector(
  selected: ThemePreference,
  onSelect: (ThemePreference) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("Theme", style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      ThemePreference.entries.forEach { theme ->
        FilterChip(
          selected = selected == theme,
          onClick = { if (selected != theme) onSelect(theme) },
          label = { Text(themeLabel(theme)) },
          modifier = Modifier.testTag("theme_option_${theme.name.lowercase()}")
        )
      }
    }
  }
}

@Composable
private fun DensitySelector(
  selected: VisualDensity,
  onSelect: (VisualDensity) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("Density", style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      VisualDensity.entries.forEach { density ->
        FilterChip(
          selected = selected == density,
          onClick = { if (selected != density) onSelect(density) },
          label = { Text(densityLabel(density)) },
          modifier = Modifier.testTag("density_option_${density.name.lowercase()}")
        )
      }
    }
  }
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
