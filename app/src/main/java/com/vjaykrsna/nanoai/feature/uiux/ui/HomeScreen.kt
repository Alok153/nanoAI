package com.vjaykrsna.nanoai.feature.uiux.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.uiux.state.CommandAction
import com.vjaykrsna.nanoai.feature.uiux.state.ModeCard
import com.vjaykrsna.nanoai.feature.uiux.state.ModeId
import com.vjaykrsna.nanoai.feature.uiux.state.RecentActivityItem
import com.vjaykrsna.nanoai.feature.uiux.state.ShellLayoutState

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
  val scrollState = rememberScrollState()
  val columnCount = remember(layout.windowSizeClass.widthSizeClass) { columnsForLayout(layout) }
  val modeById = remember(modeCards) { modeCards.associateBy { it.id } }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(horizontal = 24.dp, vertical = 24.dp)
        .testTag("home_hub"),
    verticalArrangement = Arrangement.spacedBy(24.dp),
  ) {
    HomeHeaderSection(modeCardsCount = modeCards.size)
    HomeSearchPrompt()

    if (quickActions.isNotEmpty()) {
      QuickActionsRow(
        actions = quickActions,
        onQuickActionSelect = onQuickActionSelect,
      )
    }

    ModeGrid(
      columns = columnCount,
      modeCards = modeCards,
      onModeSelect = onModeSelect,
    )

    RecentActivitySection(
      recentActivity = recentActivity,
      modeById = modeById,
      onRecentActivitySelect = onRecentActivitySelect,
    )
  }
}

@Composable
private fun HomeHeaderSection(modeCardsCount: Int) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = "Home hub",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.SemiBold,
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
  OutlinedTextField(
    value = "",
    onValueChange = {},
    readOnly = true,
    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
    label = { Text("Search or jump to action") },
    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Open command palette" },
  )
}

@Composable
private fun QuickActionsRow(
  actions: List<CommandAction>,
  onQuickActionSelect: (CommandAction) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "Quick actions",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
      )
      if (actions.size > MAX_INLINE_QUICK_ACTIONS) {
        Text(
          text = "${actions.size} available",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.testTag("quick_actions_row"),
    ) {
      items(actions, key = { it.id }) { action ->
        AssistChip(
          onClick = { if (action.enabled) onQuickActionSelect(action) },
          enabled = action.enabled,
          label = { Text(action.title) },
          modifier = Modifier.semantics { contentDescription = action.title },
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
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(
      text = "Modes",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
    )
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
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
}

@Composable
private fun ModeCardItem(
  card: ModeCard,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    enabled = card.enabled,
    tonalElevation = 4.dp,
    modifier =
      Modifier.widthIn(min = 220.dp).testTag("mode_card").semantics {
        contentDescription = card.contentDescription
      },
    shape = MaterialTheme.shapes.extraLarge,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        BadgedBox(
          badge = { card.badge?.let { badge -> Badge { Text(badgeText(badge)) } } },
        ) {
          Icon(card.icon, contentDescription = null)
        }
        if (!card.enabled) {
          Text(
            text = "Offline",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = card.title, style = MaterialTheme.typography.titleMedium)
        card.subtitle?.let { subtitle ->
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Text(
        text = card.primaryActionLabel,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
      )
    }
  }
}

@Composable
private fun RecentActivitySection(
  recentActivity: List<RecentActivityItem>,
  modeById: Map<ModeId, ModeCard>,
  onRecentActivitySelect: (RecentActivityItem) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "Recent activity",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
      )
      if (recentActivity.isNotEmpty()) {
        TextButton(onClick = { /* placeholder until history navigation is wired */}) {
          Text("View history")
        }
      }
    }

    if (recentActivity.isEmpty()) {
      Surface(tonalElevation = 1.dp) {
        Box(
          modifier = Modifier.fillMaxWidth().padding(24.dp),
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
}

@Composable
private fun RecentActivityItemCard(
  item: RecentActivityItem,
  modeCard: ModeCard?,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    tonalElevation = 1.dp,
    modifier = Modifier.fillMaxWidth().testTag("recent_activity_item"),
    shape = MaterialTheme.shapes.large,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = item.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = modeCard?.title ?: item.modeId.displayName(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(
          text = formatRelativeTime(item.timestamp),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

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
        Text(
          text = formatAbsoluteTime(item.timestamp),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

private fun columnsForLayout(layout: ShellLayoutState): Int =
  when (layout.windowSizeClass.widthSizeClass) {
    WindowWidthSizeClass.Compact -> MODE_COLUMNS_COMPACT
    WindowWidthSizeClass.Medium -> MODE_COLUMNS_MEDIUM
    WindowWidthSizeClass.Expanded -> MODE_COLUMNS_EXPANDED
    else -> MODE_COLUMNS_COMPACT
  }
