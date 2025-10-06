package com.vjaykrsna.nanoai.feature.uiux.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.uiux.presentation.HomeUiState
import com.vjaykrsna.nanoai.feature.uiux.state.BadgeInfo
import com.vjaykrsna.nanoai.feature.uiux.state.BadgeType
import com.vjaykrsna.nanoai.feature.uiux.state.CommandAction
import com.vjaykrsna.nanoai.feature.uiux.state.ModeCard
import com.vjaykrsna.nanoai.feature.uiux.state.ModeId
import com.vjaykrsna.nanoai.feature.uiux.state.RecentActivityItem
import com.vjaykrsna.nanoai.feature.uiux.state.ShellLayoutState
import com.vjaykrsna.nanoai.ui.components.OfflineBanner
import com.vjaykrsna.nanoai.ui.components.OnboardingTooltip
import com.vjaykrsna.nanoai.ui.components.PrimaryActionCard
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.text.titlecase

private const val SKELETON_CARD_COUNT = 3
private const val SKELETON_ALPHA = 0.5f

/** Home hub surface rendered inside the unified shell when [ModeId.HOME] is active. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
  layout: ShellLayoutState,
  modeCards: List<ModeCard>,
  quickActions: List<CommandAction>,
  recentActivity: List<RecentActivityItem>,
  onModeSelected: (ModeId) -> Unit,
  onQuickActionSelected: (CommandAction) -> Unit,
  onRecentActivitySelected: (RecentActivityItem) -> Unit,
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
        onQuickActionSelected = onQuickActionSelected,
      )
    }

    ModeGrid(
      columns = columnCount,
      modeCards = modeCards,
      onModeSelected = onModeSelected,
    )

    RecentActivitySection(
      recentActivity = recentActivity,
      modeById = modeById,
      onRecentActivitySelected = onRecentActivitySelected,
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
  onQuickActionSelected: (CommandAction) -> Unit,
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
      if (actions.size > 3) {
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
          onClick = { if (action.enabled) onQuickActionSelected(action) },
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
  onModeSelected: (ModeId) -> Unit,
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
          onClick = { onModeSelected(card.id) },
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
  onRecentActivitySelected: (RecentActivityItem) -> Unit,
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
        TextButton(onClick = { /* TODO: hook into history navigation */}) { Text("View history") }
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
            onClick = { onRecentActivitySelected(item) },
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
    androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Compact -> 2
    androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Medium -> 3
    androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Expanded -> 4
    else -> 2
  }

private fun badgeText(info: BadgeInfo): String =
  when (info.type) {
    BadgeType.NEW -> if (info.count > 0) "${info.count} new" else "New"
    BadgeType.PRO -> "Pro"
    BadgeType.SYNCING -> if (info.count > 0) "${info.count}" else "Sync"
  }

private fun formatRelativeTime(timestamp: Instant, reference: Instant = Instant.now()): String {
  val duration = Duration.between(timestamp, reference)
  val totalSeconds = max(0L, duration.seconds)
  val minutes = totalSeconds / 60
  val hours = minutes / 60
  val days = hours / 24
  return when {
    minutes < 1 -> "Just now"
    minutes < 60 -> "${minutes}m ago"
    hours < 24 -> "${hours}h ago"
    days < 7 -> "${days}d ago"
    else ->
      DateTimeFormatter.ofPattern("MMM d")
        .withLocale(Locale.getDefault())
        .format(
          timestamp.atZone(ZoneId.systemDefault()),
        )
  }
}

private fun formatAbsoluteTime(timestamp: Instant): String {
  val localDateTime = timestamp.atZone(ZoneId.systemDefault()).toLocalDateTime()
  val formatter = DateTimeFormatter.ofPattern("MMM d · HH:mm")
  return formatter.format(localDateTime)
}

private fun ModeId.displayName(): String =
  name.lowercase(Locale.getDefault()).replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
  }

// Legacy composable kept temporarily for navigation scaffold until shell integration lands
// everywhere.
@Deprecated("Use HomeScreen with shell layout state")
@Composable
fun HomeScreen(
  state: HomeUiState,
  callbacks: HomeScreenCallbacks,
  modifier: Modifier = Modifier,
) {
  Surface(modifier = modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().testTag("home_single_column_feed"),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      if (state.offlineBannerVisible) {
        item("offline_banner") {
          HomeOfflineBanner(
            isOffline = state.offlineBannerVisible,
            queuedActionsCount = state.queuedActions,
            onRetryOffline = callbacks.onRetryOffline,
          )
        }
      }

      item("recent_actions_header") {
        HomeRecentActionsHeader(
          toolsExpanded = state.toolsExpanded,
          onToggleTools = callbacks.onToggleTools,
        )
      }

      item("tools_panel_state") { HomeToolsPanelState(toolsExpanded = state.toolsExpanded) }

      if (state.isHydrating) {
        item("home_skeleton") { HomeSkeleton(modifier = Modifier.testTag("home_skeleton_loader")) }
      } else {
        itemsIndexed(state.recentActions) { index, action ->
          HomeRecentActionCard(
            index = index,
            action = action,
            onActionClick = callbacks.onActionClick,
          )
        }
      }

      if (state.latencyIndicatorVisible) {
        item("latency_indicator") { HomeLatencyIndicator() }
      }

      if (state.tooltipEntryVisible) {
        item("tooltip_entry") { HomeTooltipEntry(callbacks.tooltip) }
      }
    }
  }
}

data class HomeTooltipCallbacks(
  val onDismiss: () -> Unit,
  val onHelp: () -> Unit,
  val onDontShowAgain: () -> Unit,
)

data class HomeScreenCallbacks(
  val onToggleTools: () -> Unit,
  val onActionClick: (String) -> Unit,
  val onRetryOffline: () -> Unit,
  val tooltip: HomeTooltipCallbacks,
)

@Composable
private fun HomeOfflineBanner(
  isOffline: Boolean,
  queuedActionsCount: Int,
  onRetryOffline: () -> Unit,
) {
  OfflineBanner(
    isOffline = isOffline,
    queuedActions = queuedActionsCount,
    onRetry = onRetryOffline,
    modifier = Modifier.fillMaxWidth().testTag("offline_banner_container_wrapper"),
  )
}

@Composable
private fun HomeRecentActionsHeader(
  toolsExpanded: Boolean,
  onToggleTools: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = "Recent actions",
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.testTag("home_recent_actions_header").semantics { heading() },
    )
    IconButton(
      onClick = onToggleTools,
      modifier =
        Modifier.testTag("home_tools_toggle").semantics {
          contentDescription = if (toolsExpanded) "Collapse tools panel" else "Expand tools panel"
        },
    ) {
      Icon(
        imageVector =
          if (toolsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
        contentDescription = null,
      )
    }
  }
}

@Composable
private fun HomeToolsPanelState(toolsExpanded: Boolean) {
  if (toolsExpanded) {
    Text(
      text = "Tools",
      style = MaterialTheme.typography.titleMedium,
      modifier = Modifier.testTag("home_tools_panel_expanded").semantics { heading() },
    )
  } else {
    Text(
      text = "Advanced tools hidden",
      style = MaterialTheme.typography.bodySmall,
      modifier =
        Modifier.testTag("home_tools_panel_collapsed").semantics {
          contentDescription =
            "Advanced tools are currently hidden. Activate the toggle to reveal tools."
        },
    )
  }
}

@Composable
private fun HomeRecentActionCard(
  index: Int,
  action: String,
  onActionClick: (String) -> Unit,
) {
  PrimaryActionCard(
    title = action.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
    description = "Quick action",
    tag = "home_recent_action_$index",
    onClick = { onActionClick(action) },
  )
}

@Composable
private fun HomeLatencyIndicator() {
  Text(
    text = "Response in under 100ms",
    style = MaterialTheme.typography.labelMedium,
    modifier = Modifier.testTag("home_latency_meter"),
  )
}

@Composable
private fun HomeTooltipEntry(
  callbacks: HomeTooltipCallbacks,
) {
  Column(
    modifier =
      Modifier.fillMaxWidth().testTag("onboarding_tooltip_entry").semantics {
        contentDescription = "Onboarding tips"
      },
  ) {
    OnboardingTooltip(
      message = "Tip: Pin your favorite tools for quick access.",
      onDismiss = callbacks.onDismiss,
      onDontShowAgain = callbacks.onDontShowAgain,
      onHelp = callbacks.onHelp,
    )
  }
}

@Composable
private fun HomeSkeleton(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    repeat(SKELETON_CARD_COUNT) {
      Card(
        modifier = Modifier.fillMaxWidth().alpha(SKELETON_ALPHA),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      ) {
        Spacer(modifier = Modifier.height(56.dp))
      }
    }
  }
}
