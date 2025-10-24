package com.vjaykrsna.nanoai.feature.uiux.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.uiux.presentation.CommandAction
import com.vjaykrsna.nanoai.feature.uiux.presentation.ModeCard
import com.vjaykrsna.nanoai.feature.uiux.presentation.ModeId
import com.vjaykrsna.nanoai.feature.uiux.presentation.ProgressJob
import com.vjaykrsna.nanoai.feature.uiux.presentation.RecentActivityItem
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellLayoutState
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoElevation
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoSpacing
import com.vjaykrsna.nanoai.feature.uiux.ui.components.layout.NanoScreen
import com.vjaykrsna.nanoai.feature.uiux.ui.components.layout.NanoSection
import com.vjaykrsna.nanoai.feature.uiux.ui.components.primitives.NanoCard
import com.vjaykrsna.nanoai.feature.uiux.ui.progress.ProgressCenterPanel

private const val MODE_COLUMNS_COMPACT = 2
private const val MODE_COLUMNS_MEDIUM = 3
private const val MODE_COLUMNS_EXPANDED = 4

/** Home hub surface rendered inside the unified shell when [ModeId.HOME] is active. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
  layout: ShellLayoutState,
  modeCards: List<ModeCard>,
  quickActions: List<CommandAction>,
  recentActivity: List<RecentActivityItem>,
  progressJobs: List<ProgressJob>,
  onModeSelect: (ModeId) -> Unit,
  onQuickActionSelect: (CommandAction) -> Unit,
  onRecentActivitySelect: (RecentActivityItem) -> Unit,
  onProgressRetry: (ProgressJob) -> Unit,
  onProgressDismiss: (ProgressJob) -> Unit,
  modifier: Modifier = Modifier,
) {
  val columnCount = remember(layout.windowSizeClass.widthSizeClass) { columnsForLayout(layout) }
  val modeById = remember(modeCards) { modeCards.associateBy { it.id } }

  NanoScreen(modifier = modifier.testTag("home_hub")) {
    var toolsExpanded by rememberSaveable { mutableStateOf(false) }
    var recentConfirmation by rememberSaveable { mutableStateOf<String?>(null) }

    QuickActionsPanel(
      actions = quickActions,
      expanded = toolsExpanded,
      onToggle = {
        if (quickActions.isNotEmpty()) {
          toolsExpanded = !toolsExpanded
        }
      },
      onQuickActionSelect = { action ->
        onQuickActionSelect(action)
        recentConfirmation = null
      },
    )

    NanoSection(title = "Modes") {
      ModeGrid(columns = columnCount, modeCards = modeCards, onModeSelect = onModeSelect)
    }

    if (progressJobs.isNotEmpty()) {
      NanoSection(title = "Queued jobs") {
        ProgressCenterPanel(
          jobs = progressJobs,
          onRetry = onProgressRetry,
          onDismissJob = onProgressDismiss,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }

    NanoSection(
      title = "Recent activity",
      action = {
        if (recentActivity.isNotEmpty()) {
          TextButton(onClick = { /* placeholder until history navigation is wired */ }) {
            Text("View history")
          }
        }
      },
    ) {
      RecentActivityContent(
        recentActivity = recentActivity,
        modeById = modeById,
        onRecentActivitySelect = { item ->
          recentConfirmation = item.title
          onRecentActivitySelect(item)
        },
      )
    }

    recentConfirmation?.let { title ->
      Surface(
        modifier = Modifier.fillMaxWidth().testTag("home_recent_action_confirmation"),
        tonalElevation = NanoElevation.level1,
        shape = MaterialTheme.shapes.large,
      ) {
        Text(
          text = "$title ready to resume",
          modifier = Modifier.padding(NanoSpacing.md),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun QuickActionsPanel(
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

      if (expanded && actions.isNotEmpty()) {
        QuickActionsRow(
          actions = actions,
          onQuickActionSelect = onQuickActionSelect,
          modifier = Modifier.testTag("home_tools_panel_expanded"),
        )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModeGrid(columns: Int, modeCards: List<ModeCard>, onModeSelect: (ModeId) -> Unit) {
  Box(
    modifier =
      Modifier.fillMaxWidth().testTag("home_mode_grid").semantics {
        val rowCount = ((modeCards.size + columns - 1) / columns).coerceAtLeast(1)
        this[SemanticsProperties.CollectionInfo] =
          CollectionInfo(rowCount = rowCount, columnCount = columns)
      }
  ) {
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(NanoSpacing.md),
      verticalArrangement = Arrangement.spacedBy(NanoSpacing.md),
      maxItemsInEachRow = columns,
    ) {
      modeCards.forEach { card -> ModeCardItem(card = card, onClick = { onModeSelect(card.id) }) }
    }
  }
}

@Composable
private fun ModeCardItem(card: ModeCard, onClick: () -> Unit) {
  NanoCard(
    title = card.title,
    subtitle = card.subtitle,
    icon = card.icon,
    badge = card.badge?.let { badgeText(it) },
    enabled = card.enabled,
    onClick = onClick,
    modifier =
      Modifier.widthIn(min = 220.dp).testTag("mode_card").semantics {
        stateDescription = if (card.enabled) "Available" else "Unavailable"
      },
    trailingContent = {
      if (!card.enabled) {
        Text(
          text = "Offline",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.error,
        )
      }
    },
    semanticsDescription = card.contentDescription,
  )
}

@Composable
private fun RecentActivityContent(
  recentActivity: List<RecentActivityItem>,
  modeById: Map<ModeId, ModeCard>,
  onRecentActivitySelect: (RecentActivityItem) -> Unit,
) {
  if (recentActivity.isEmpty()) {
    Surface(
      shape = MaterialTheme.shapes.large,
      tonalElevation = NanoElevation.level1,
      modifier = Modifier.testTag("recent_activity_list"),
    ) {
      Box(
        modifier = Modifier.fillMaxWidth().padding(NanoSpacing.lg),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "No recent activity yet.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  } else {
    Column(
      modifier = Modifier.testTag("recent_activity_list"),
      verticalArrangement = Arrangement.spacedBy(NanoSpacing.sm),
    ) {
      recentActivity.forEach { item ->
        RecentActivityItemCard(
          item = item,
          modeCard = modeById[item.modeId],
          onClick = { onRecentActivitySelect(item) },
        )
      }
    }
  }
}

@Composable
private fun RecentActivityItemCard(
  item: RecentActivityItem,
  modeCard: ModeCard?,
  onClick: () -> Unit,
) {
  NanoCard(
    title = item.title,
    subtitle = modeCard?.title ?: item.modeId.displayName(),
    supportingContent = {
      HorizontalDivider()
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = item.statusLabel,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
          horizontalAlignment = Alignment.End,
          verticalArrangement = Arrangement.spacedBy(NanoSpacing.xs),
        ) {
          Text(
            text = formatRelativeTime(item.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = formatAbsoluteTime(item.timestamp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    },
    modifier = Modifier.fillMaxWidth().testTag("recent_activity_item"),
    onClick = onClick,
    semanticsDescription =
      buildString {
        append(item.title)
        append(", status ")
        append(item.statusLabel)
        append(", updated ")
        append(formatRelativeTime(item.timestamp))
      },
  )
}

private fun columnsForLayout(layout: ShellLayoutState): Int =
  when (layout.windowSizeClass.widthSizeClass) {
    WindowWidthSizeClass.Compact -> MODE_COLUMNS_COMPACT
    WindowWidthSizeClass.Medium -> MODE_COLUMNS_MEDIUM
    WindowWidthSizeClass.Expanded -> MODE_COLUMNS_EXPANDED
    else -> MODE_COLUMNS_COMPACT
  }
