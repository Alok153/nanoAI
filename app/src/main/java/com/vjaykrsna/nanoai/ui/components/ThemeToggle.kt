package com.vjaykrsna.nanoai.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import kotlin.text.titlecase

@Composable
fun ThemeToggle(
  currentTheme: ThemePreference,
  onThemeChange: (ThemePreference) -> Unit,
  modifier: Modifier = Modifier,
  animationsEnabled: Boolean = true,
) {
  val haptics = LocalHapticFeedback.current
  val usingSystem = currentTheme == ThemePreference.SYSTEM

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .semantics { contentDescription = "Theme toggle row" }
        .padding(vertical = 12.dp, horizontal = 16.dp),
  ) {
    ThemeToggleHeader()
    Spacer(modifier = Modifier.height(8.dp))
    ThemeModeChips(
      currentTheme = currentTheme,
      usingSystem = usingSystem,
      haptics = haptics,
      onThemeChange = onThemeChange,
    )
    Spacer(modifier = Modifier.height(12.dp))
    ThemePreferenceSwitch(
      currentTheme = currentTheme,
      usingSystem = usingSystem,
      haptics = haptics,
      onThemeChange = onThemeChange,
    )
    Spacer(modifier = Modifier.height(8.dp))
    ThemeStatusRow(
      currentTheme = currentTheme,
      animationsEnabled = animationsEnabled,
    )
    Spacer(
      modifier = Modifier.fillMaxWidth().height(1.dp).testTag("theme_layout_stability_check"),
    )
  }
}

@Composable
private fun ThemeToggleHeader() {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = "Appearance",
      style = MaterialTheme.typography.titleMedium,
      modifier = Modifier.testTag("theme_toggle_title").semantics { heading() },
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = "Choose Light, Dark, or System default",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier =
        Modifier.fillMaxWidth().semantics { contentDescription = "Theme selection instructions" },
    )
  }
}

@Composable
private fun ThemeModeChips(
  currentTheme: ThemePreference,
  usingSystem: Boolean,
  haptics: HapticFeedback,
  onThemeChange: (ThemePreference) -> Unit,
) {
  val options = remember {
    listOf(
      ThemeToggleOption(
        preference = ThemePreference.SYSTEM,
        label = "System",
        icon = Icons.Filled.AutoMode,
        description = "System theme",
      ),
      ThemeToggleOption(
        preference = ThemePreference.LIGHT,
        label = "Light",
        icon = Icons.Filled.LightMode,
        description = "Light theme",
      ),
      ThemeToggleOption(
        preference = ThemePreference.DARK,
        label = "Dark",
        icon = Icons.Filled.DarkMode,
        description = "Dark theme",
      ),
    )
  }

  Row(
    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Theme options" },
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    options.forEach { option ->
      val isSelected = currentTheme == option.preference
      ThemeModeChip(
        option = option,
        selected = isSelected,
        onSelect = {
          if (!isSelected) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onThemeChange(option.preference)
          }
        },
        usingSystem = usingSystem,
      )
    }
  }
}

@Composable
private fun ThemeModeChip(
  option: ThemeToggleOption,
  selected: Boolean,
  onSelect: () -> Unit,
  usingSystem: Boolean,
) {
  val stateDescriptionValue =
    when {
      option.preference == ThemePreference.SYSTEM -> if (usingSystem) "Selected" else "Not selected"
      selected -> "Selected"
      else -> "Not selected"
    }

  FilterChip(
    selected = selected,
    onClick = onSelect,
    label = { Text(option.label) },
    leadingIcon = { Icon(imageVector = option.icon, contentDescription = null) },
    modifier =
      Modifier.testTag("theme_toggle_option_${option.label.lowercase()}").semantics {
        contentDescription = option.description
        stateDescription = stateDescriptionValue
      },
    colors = FilterChipDefaults.filterChipColors(),
  )
}

@Composable
private fun ThemePreferenceSwitch(
  currentTheme: ThemePreference,
  usingSystem: Boolean,
  haptics: HapticFeedback,
  onThemeChange: (ThemePreference) -> Unit,
) {
  val darkSelected = currentTheme == ThemePreference.DARK

  Row(
    modifier =
      Modifier.fillMaxWidth().semantics {
        role = Role.Switch
        contentDescription = "Toggle dark theme"
        stateDescription = if (darkSelected) "Dark mode enabled" else "Light mode enabled"
      },
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text("Dark mode", style = MaterialTheme.typography.bodyMedium)
    Switch(
      checked = darkSelected,
      onCheckedChange = { checked ->
        val target = if (checked) ThemePreference.DARK else ThemePreference.LIGHT
        if (target != currentTheme) {
          haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onThemeChange(target)
        }
      },
      enabled = !usingSystem,
      modifier = Modifier.testTag("theme_toggle_switch"),
    )
  }
}

@Composable
private fun ThemeStatusRow(
  currentTheme: ThemePreference,
  animationsEnabled: Boolean,
) {
  val statusText = "Current: ${currentTheme.displayName()}"

  if (animationsEnabled) {
    AnimatedContent(
      targetState = statusText,
      label = "theme_status_animation",
    ) { animatedText ->
      Text(
        text = animatedText,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.testTag("theme_toggle_persistence_status"),
      )
    }
  } else {
    Text(
      text = statusText,
      style = MaterialTheme.typography.bodySmall,
      modifier = Modifier.testTag("theme_toggle_persistence_status"),
    )
  }
}

private data class ThemeToggleOption(
  val preference: ThemePreference,
  val label: String,
  val icon: ImageVector,
  val description: String,
)

private fun ThemePreference.displayName(): String =
  name.lowercase().replaceFirstChar { it.titlecase() }
