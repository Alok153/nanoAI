package com.vjaykrsna.nanoai.feature.uiux.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

@Composable
fun SidebarDrawer(
  pinnedTools: List<String>,
  activeRoute: String?,
  onNavigateSettings: () -> Unit,
  onNavigateHome: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(16.dp)
        .semantics { contentDescription = "Sidebar navigation" }
        .testTag("sidebar_drawer"),
  ) {
    Text(
      text = "Navigation",
      style = MaterialTheme.typography.titleMedium,
      modifier = Modifier.semantics { heading() },
    )
    NavigationDrawerItem(
      icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
      label = { Text("Home") },
      selected = activeRoute?.startsWith("home") == true,
      onClick = onNavigateHome,
      modifier =
        Modifier.testTag("sidebar_nav_home").semantics {
          contentDescription = "Navigate to Home"
          if (activeRoute?.startsWith("home") == true) stateDescription = "Currently selected"
        },
      colors =
        NavigationDrawerItemDefaults.colors(
          selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
          unselectedContainerColor = MaterialTheme.colorScheme.surface,
        ),
    )
    NavigationDrawerItem(
      icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
      label = { Text("Settings") },
      selected = activeRoute?.startsWith("settings") == true,
      onClick = onNavigateSettings,
      modifier =
        Modifier.testTag("sidebar_item_settings").semantics {
          contentDescription = "Navigate to Settings"
          if (activeRoute?.startsWith("settings") == true) stateDescription = "Currently selected"
        },
      colors =
        NavigationDrawerItemDefaults.colors(
          selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
          unselectedContainerColor = MaterialTheme.colorScheme.surface,
        ),
    )
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    Text(
      text = "Pinned tools",
      style = MaterialTheme.typography.titleSmall,
      modifier = Modifier.semantics { heading() },
    )
    pinnedTools.forEach { tool ->
      Text(
        text = tool,
        style = MaterialTheme.typography.bodySmall,
        modifier =
          Modifier.fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("sidebar_pinned_$tool")
            .semantics { contentDescription = "$tool pinned tool" },
      )
    }
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    Text(
      text = "Deep links",
      style = MaterialTheme.typography.titleSmall,
      modifier = Modifier.testTag("sidebar_deeplink_slot").semantics { heading() },
    )
  }
}
