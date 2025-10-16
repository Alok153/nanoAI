package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.feature.settings.presentation.SettingsUiUxState
import com.vjaykrsna.nanoai.feature.uiux.ui.components.ThemePreferenceChips
import com.vjaykrsna.nanoai.feature.uiux.ui.components.VisualDensityChips

@Composable
internal fun AppearanceThemeSection(
  uiUxState: SettingsUiUxState,
  onThemeChange: (ThemePreference) -> Unit,
  modifier: Modifier = Modifier,
) {
  SettingsSection(title = "Theme", modifier = modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(
        text = "Switch between light, dark, AMOLED (pitch black), or follow the system theme.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      ThemePreferenceChips(
        selected = uiUxState.themePreference,
        onSelect = onThemeChange,
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
internal fun AppearanceDensitySection(
  uiUxState: SettingsUiUxState,
  onDensityChange: (VisualDensity) -> Unit,
  modifier: Modifier = Modifier,
) {
  val selectedDensity =
    if (uiUxState.compactModeEnabled) VisualDensity.COMPACT else VisualDensity.DEFAULT
  SettingsSection(title = "Layout density", modifier = modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(
        text = "Pick how compact lists and rails should feel across the app.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      VisualDensityChips(
        selected = selectedDensity,
        onSelect = onDensityChange,
        modifier = Modifier.fillMaxWidth(),
        onUnsupportedSelect = null,
      )
      HorizontalDivider()
      Text(
        text = "Spacious mode is coming soon and will enable a more relaxed layout for tablets.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
internal fun AppearanceTypographyPlaceholder(modifier: Modifier = Modifier) {
  SettingsSection(title = "Typography & spacing", modifier = modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(
        text = "Adjusting font scale and chat bubble density will land in Phase 2.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(8.dp))
      SettingsPlaceholderCard(
        description = "Custom font scale presets and per-surface density controls.",
        supportingText =
          "Specs for typography tokens live in specs/003-UI-UX/plan.md " +
            "and will be wired once shared theming surfaces are ready.",
      )
    }
  }
}
