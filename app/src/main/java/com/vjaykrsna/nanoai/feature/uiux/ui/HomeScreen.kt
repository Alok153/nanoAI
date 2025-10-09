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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.uiux.state.CommandAction
import com.vjaykrsna.nanoai.feature.uiux.state.ModeCard
import com.vjaykrsna.nanoai.feature.uiux.state.ModeId
import com.vjaykrsna.nanoai.feature.uiux.state.RecentActivityItem
import com.vjaykrsna.nanoai.feature.uiux.state.ShellLayoutState
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoSpacing
import com.vjaykrsna.nanoai.feature.uiux.ui.components.layout.NanoScreen
import com.vjaykrsna.nanoai.feature.uiux.ui.components.layout.NanoSection
import com.vjaykrsna.nanoai.feature.uiux.ui.components.primitives.NanoCard
import com.vjaykrsna.nanoai.feature.uiux.ui.components.primitives.NanoInputField

private const val MAX_INLINE_QUICK_ACTIONS = 3
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
  onModeSelect: (ModeId) -> Unit,
  onQuickActionSelect: (CommandAction) -> Unit,
  onRecentActivitySelect: (RecentActivityItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  val columnCount = remember(layout.windowSizeClass.widthSizeClass) { columnsForLayout(layout) }
  val modeById = remember(modeCards) { modeCards.associateBy { it.id } }

  NanoScreen(
    modifier = modifier.testTag("home_hub"),
    header = { HomeHeaderSection(modeCardsCount = modeCards.size) },
  ) {
    HomeSearchPrompt()

    if (quickActions.isNotEmpty()) {
      QuickActionsSection(
        actions = quickActions,
        onQuickActionSelect = onQuickActionSelect,
      )
    }

    NanoSection(title = "Modes") {
      ModeGrid(
        columns = columnCount,
        modeCards = modeCards,
        onModeSelect = onModeSelect,
      )
    }

    NanoSection(
      title = "Recent activity",
      action = {
        if (recentActivity.isNotEmpty()) {
          TextButton(onClick = { /* placeholder until history navigation is wired */}) {
            Text("View history")
          }
        }
      },
    ) {
      RecentActivityContent(
        recentActivity = recentActivity,
        modeById = modeById,
        onRecentActivitySelect = onRecentActivitySelect,
      )
    }
  }
}

@Composable
private fun HomeHeaderSection(modeCardsCount: Int) {
  Column(verticalArrangement = Arrangement.spacedBy(NanoSpacing.sm)) {
    Text(
      text = "Home hub",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.semantics { heading() },
    )
    Text(
      text = "Choose from $modeCardsCount modes or jump back into recent work.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun HomeSearchPrompt() {
  NanoInputField(
    value = "",
    onValueChange = {},
    label = "Search or jump to action",
    placeholder = "Search or jump to action",
    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
    readOnly = true,
    modifier =
      Modifier.fillMaxWidth().semantics {
        role = Role.Button
        contentDescription = "Open command palette"
        stateDescription = "Opens global command search"
      },
  )
}

@Composable
private fun QuickActionsSection(
  actions: List<CommandAction>,
  onQuickActionSelect: (CommandAction) -> Unit,
  modifier: Modifier = Modifier,
) {
  NanoSection(
    title = "Quick actions",
    subtitle = if (actions.size > MAX_INLINE_QUICK_ACTIONS) "${actions.size} available" else null,
    modifier = modifier,
  ) {
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(NanoSpacing.sm),
      modifier = Modifier.testTag("quick_actions_row"),
    ) {
      items(actions, key = { it.id }) { action ->
        AssistChip(
          onClick = { if (action.enabled) onQuickActionSelect(action) },
          enabled = action.enabled,
          label = { Text(action.title) },
          modifier =
            Modifier.semantics {
              contentDescription = action.title
              stateDescription = if (action.enabled) "Enabled" else "Disabled"
            },
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModeGrid(
  columns: Int,
  modeCards: List<ModeCard>,
  onModeSelect: (ModeId) -> Unit,
) {
  FlowRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(NanoSpacing.md),
    verticalArrangement = Arrangement.spacedBy(NanoSpacing.md),
    maxItemsInEachRow = columns,
  ) {
    modeCards.forEach { card ->
      ModeCardItem(
        card = card,
        onClick = { onModeSelect(card.id) },
      )
    }
  }
}

@Composable
private fun ModeCardItem(
  card: ModeCard,
  onClick: () -> Unit,
) {
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
    Surface(shape = MaterialTheme.shapes.large, tonalElevation = 1.dp) {
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
