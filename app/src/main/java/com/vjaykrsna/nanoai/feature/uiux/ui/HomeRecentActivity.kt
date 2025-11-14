package com.vjaykrsna.nanoai.feature.uiux.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.vjaykrsna.nanoai.core.domain.model.uiux.ModeId
import com.vjaykrsna.nanoai.core.domain.model.uiux.RecentActivityItem
import com.vjaykrsna.nanoai.feature.uiux.presentation.ModeCard
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoElevation
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoSpacing
import com.vjaykrsna.nanoai.feature.uiux.ui.components.layout.NanoSection
import com.vjaykrsna.nanoai.feature.uiux.ui.components.primitives.NanoCard

@Composable
internal fun RecentActivitySection(
  recentActivity: List<RecentActivityItem>,
  modeById: Map<ModeId, ModeCard>,
  onRecentActivitySelect: (RecentActivityItem) -> Unit,
) {
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
      onRecentActivitySelect = onRecentActivitySelect,
    )
  }
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
