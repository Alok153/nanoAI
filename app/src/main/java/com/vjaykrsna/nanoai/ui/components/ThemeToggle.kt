package com.vjaykrsna.nanoai.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
  modifier: Modifier = Modifier
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
      modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      FilterChip(
        selected = usingSystem,
        onClick = {
          haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onThemeChange(ThemePreference.SYSTEM)
        },
        label = { Text("System") },
        leadingIcon = {
          Icon(
            imageVector = Icons.Filled.AutoMode,
            contentDescription = null,
          )
        },
        modifier =
          Modifier.testTag("theme_toggle_option_system").semantics {
            contentDescription = "System theme"
            stateDescription = if (usingSystem) "Selected" else "Not selected"
          },
        colors = FilterChipDefaults.filterChipColors(),
      )
      FilterChip(
        selected = !usingSystem && currentTheme == ThemePreference.LIGHT,
        onClick = {
          haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onThemeChange(ThemePreference.LIGHT)
        },
        label = { Text("Light") },
        leadingIcon = { Icon(imageVector = Icons.Filled.LightMode, contentDescription = null) },
        modifier =
          Modifier.testTag("theme_toggle_option_light").semantics {
            contentDescription = "Light theme"
            stateDescription =
              if (!usingSystem && currentTheme == ThemePreference.LIGHT) "Selected"
              else "Not selected"
          },
      )
      FilterChip(
        selected = !usingSystem && currentTheme == ThemePreference.DARK,
        onClick = {
          haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onThemeChange(ThemePreference.DARK)
        },
        label = { Text("Dark") },
        leadingIcon = { Icon(imageVector = Icons.Filled.DarkMode, contentDescription = null) },
        modifier =
          Modifier.testTag("theme_toggle_option_dark").semantics {
            contentDescription = "Dark theme"
            stateDescription =
              if (!usingSystem && currentTheme == ThemePreference.DARK) "Selected"
              else "Not selected"
          },
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    val darkSelected = currentTheme == ThemePreference.DARK
    Row(
      modifier =
        Modifier.fillMaxWidth().semantics {
          role = Role.Switch
          stateDescription = if (darkSelected) "Dark mode enabled" else "Light mode enabled"
        },
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text("Dark mode", style = MaterialTheme.typography.bodyMedium)
      Switch(
        checked = darkSelected,
        onCheckedChange = { checked ->
          haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onThemeChange(if (checked) ThemePreference.DARK else ThemePreference.LIGHT)
        },
        enabled = !usingSystem,
        modifier =
          Modifier.testTag("theme_toggle_switch").semantics {
            contentDescription = "Toggle dark theme"
            stateDescription = if (darkSelected) "Dark" else "Light"
          },
      )
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text =
        "Current: ${
                currentTheme.name.lowercase().replaceFirstChar { it.titlecase() }
            }",
      style = MaterialTheme.typography.bodySmall,
      modifier = Modifier.testTag("theme_toggle_persistence_status"),
    )

    Spacer(
      modifier = Modifier.fillMaxWidth().height(1.dp).testTag("theme_layout_stability_check"),
    )
  }
}
