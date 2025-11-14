package com.vjaykrsna.nanoai.feature.uiux.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandAction
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoElevation
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoSpacing

@Composable
internal fun QuickActionsPanel(
  actions: List<CommandAction>,
  expanded: Boolean,
  onToggle: () -> Unit,
  onQuickActionSelect: (CommandAction) -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    tonalElevation = NanoElevation.level1,
  ) {
    Column(
      modifier = Modifier.padding(NanoSpacing.md),
      verticalArrangement = Arrangement.spacedBy(NanoSpacing.sm),
    ) {
      QuickActionsHeader(actions = actions, expanded = expanded, onToggle = onToggle)
      QuickActionsBody(
        actions = actions,
        expanded = expanded,
        onQuickActionSelect = onQuickActionSelect,
      )
    }
  }
}

@Composable
private fun QuickActionsHeader(
  actions: List<CommandAction>,
  expanded: Boolean,
  onToggle: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(NanoSpacing.xs)) {
      Text(text = "Quick actions", style = MaterialTheme.typography.titleMedium)
      Text(
        text = if (actions.isEmpty()) "No shortcuts available" else "${actions.size} shortcuts",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    TextButton(
      onClick = onToggle,
      modifier =
        Modifier.testTag("home_tools_toggle").semantics {
          contentDescription = "Toggle tools panel"
          stateDescription = if (expanded) "Expanded" else "Collapsed"
        },
      enabled = actions.isNotEmpty(),
    ) {
      Text(if (expanded) "Hide" else "Show")
    }
  }
}

@Composable
private fun QuickActionsBody(
  actions: List<CommandAction>,
  expanded: Boolean,
  onQuickActionSelect: (CommandAction) -> Unit,
) {
  if (expanded && actions.isNotEmpty()) {
    Box(modifier = Modifier.fillMaxWidth().testTag("home_tools_panel_expanded")) {
      QuickActionsRow(
        actions = actions,
        onQuickActionSelect = onQuickActionSelect,
        modifier = Modifier.fillMaxWidth(),
      )
    }
  } else {
    Surface(
      modifier = Modifier.fillMaxWidth().testTag("home_tools_panel_collapsed"),
      tonalElevation = NanoElevation.level0,
      color = MaterialTheme.colorScheme.surfaceVariant,
      shape = MaterialTheme.shapes.medium,
    ) {
      Text(
        text = if (actions.isEmpty()) "No tools to show" else "Tools panel collapsed",
        modifier = Modifier.padding(vertical = NanoSpacing.sm, horizontal = NanoSpacing.md),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun QuickActionsRow(
  actions: List<CommandAction>,
  onQuickActionSelect: (CommandAction) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyRow(
    horizontalArrangement = Arrangement.spacedBy(NanoSpacing.sm),
    modifier = modifier.testTag("quick_actions_row"),
  ) {
    items(actions, key = { it.id }) { action ->
      AssistChip(
        onClick = { if (action.enabled) onQuickActionSelect(action) },
        enabled = action.enabled,
        label = { Text(action.title) },
        modifier =
          Modifier.testTag("home_quick_action_${action.id}").semantics {
            contentDescription = action.title
            stateDescription = if (action.enabled) "Enabled" else "Disabled"
          },
      )
    }
  }
}
