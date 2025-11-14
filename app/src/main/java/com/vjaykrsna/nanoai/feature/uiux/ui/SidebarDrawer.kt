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
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(16.dp)
        .semantics { contentDescription = "Sidebar navigation" }
        .testTag("sidebar_drawer")
  ) {
    SidebarSectionTitle(text = "Navigation", style = MaterialTheme.typography.titleMedium)
    SidebarNavItem(
      label = "Home",
      icon = Icons.Outlined.Home,
      isSelected = activeRoute?.startsWith("home") == true,
      testTag = "sidebar_nav_home",
      onClick = onNavigateHome,
    )
    SidebarNavItem(
      label = "Settings",
      icon = Icons.Outlined.Settings,
      isSelected = activeRoute?.startsWith("settings") == true,
      testTag = "sidebar_item_settings",
      onClick = onNavigateSettings,
    )
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    SidebarSectionTitle(text = "Pinned tools", style = MaterialTheme.typography.titleSmall)
    PinnedToolsList(pinnedTools)
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    Text(
      text = "Deep links",
      style = MaterialTheme.typography.titleSmall,
      modifier = Modifier.testTag("sidebar_deeplink_slot").semantics { heading() },
    )
  }
}

@Composable
private fun SidebarSectionTitle(text: String, style: androidx.compose.ui.text.TextStyle) {
  Text(text = text, style = style, modifier = Modifier.semantics { heading() })
}

@Composable
private fun SidebarNavItem(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  isSelected: Boolean,
  testTag: String,
  onClick: () -> Unit,
) {
  NavigationDrawerItem(
    icon = { Icon(icon, contentDescription = null) },
    label = { Text(label) },
    selected = isSelected,
    onClick = onClick,
    modifier =
      Modifier.testTag(testTag).semantics {
        contentDescription = "Navigate to $label"
        if (isSelected) stateDescription = "Currently selected"
      },
    colors =
      NavigationDrawerItemDefaults.colors(
        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        unselectedContainerColor = MaterialTheme.colorScheme.surface,
      ),
  )
}

@Composable
private fun PinnedToolsList(pinnedTools: List<String>) {
  pinnedTools.forEach { tool ->
    Text(
      text = tool,
      style = MaterialTheme.typography.bodySmall,
      modifier =
        Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("sidebar_pinned_$tool").semantics {
          contentDescription = "$tool pinned tool"
        },
    )
  }
}
