package com.vjaykrsna.nanoai.feature.uiux.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.uiux.presentation.HomeUiState
import com.vjaykrsna.nanoai.ui.components.OfflineBanner
import com.vjaykrsna.nanoai.ui.components.OnboardingTooltip
import com.vjaykrsna.nanoai.ui.components.PrimaryActionCard
import kotlin.text.titlecase

@Composable
fun HomeScreen(
  state: HomeUiState,
  onToggleTools: () -> Unit,
  onActionClick: (String) -> Unit,
  onTooltipDismiss: () -> Unit,
  onTooltipHelp: () -> Unit,
  onTooltipDontShow: () -> Unit,
  onRetryOffline: () -> Unit,
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
            onRetryOffline = onRetryOffline,
          )
        }
      }

      item("recent_actions_header") {
        HomeRecentActionsHeader(
          toolsExpanded = state.toolsExpanded,
          onToggleTools = onToggleTools,
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
            onActionClick = onActionClick,
          )
        }
      }

      if (state.latencyIndicatorVisible) {
        item("latency_indicator") { HomeLatencyIndicator() }
      }

      if (state.tooltipEntryVisible) {
        item("tooltip_entry") {
          HomeTooltipEntry(
            onTooltipDismiss = onTooltipDismiss,
            onTooltipDontShow = onTooltipDontShow,
            onTooltipHelp = onTooltipHelp,
          )
        }
      }
    }
  }
}

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
  onTooltipDismiss: () -> Unit,
  onTooltipDontShow: () -> Unit,
  onTooltipHelp: () -> Unit,
) {
  Column(
    modifier =
      Modifier.fillMaxWidth().testTag("onboarding_tooltip_entry").semantics {
        contentDescription = "Onboarding tips"
      },
  ) {
    OnboardingTooltip(
      message = "Tip: Pin your favorite tools for quick access.",
      onDismiss = onTooltipDismiss,
      onDontShowAgain = onTooltipDontShow,
      onHelp = onTooltipHelp,
    )
  }
}

@Composable
private fun HomeSkeleton(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    repeat(3) {
      Card(
        modifier = Modifier.fillMaxWidth().alpha(0.5f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      ) {
        Spacer(modifier = Modifier.height(56.dp))
      }
    }
  }
}
