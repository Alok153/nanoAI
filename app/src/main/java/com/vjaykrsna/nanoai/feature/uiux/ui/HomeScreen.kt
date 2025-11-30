package com.vjaykrsna.nanoai.feature.uiux.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandAction
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandDestination
import com.vjaykrsna.nanoai.core.domain.model.uiux.ModeId
import com.vjaykrsna.nanoai.core.domain.model.uiux.ProgressJob
import com.vjaykrsna.nanoai.core.domain.model.uiux.RecentActivityItem
import com.vjaykrsna.nanoai.core.domain.model.uiux.ShellWindowWidthClass
import com.vjaykrsna.nanoai.core.domain.uiux.navigation.toModeIdOrNull
import com.vjaykrsna.nanoai.feature.uiux.presentation.ModeCard
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
    HomeScreenSections(
      quickActions = quickActions,
      modeCards = modeCards,
      columnCount = columnCount,
      progressJobs = progressJobs,
      recentActivity = recentActivity,
      modeById = modeById,
      onModeSelect = onModeSelect,
      onQuickActionSelect = onQuickActionSelect,
      onProgressRetry = onProgressRetry,
      onProgressDismiss = onProgressDismiss,
      onRecentActivitySelect = onRecentActivitySelect,
    )
  }
}

@Composable
private fun HomeScreenSections(
  quickActions: List<CommandAction>,
  modeCards: List<ModeCard>,
  columnCount: Int,
  progressJobs: List<ProgressJob>,
  recentActivity: List<RecentActivityItem>,
  modeById: Map<ModeId, ModeCard>,
  onModeSelect: (ModeId) -> Unit,
  onQuickActionSelect: (CommandAction) -> Unit,
  onProgressRetry: (ProgressJob) -> Unit,
  onProgressDismiss: (ProgressJob) -> Unit,
  onRecentActivitySelect: (RecentActivityItem) -> Unit,
) {
  var toolsExpanded by rememberSaveable { mutableStateOf(false) }
  var recentConfirmation by rememberSaveable { mutableStateOf<String?>(null) }
  Column(verticalArrangement = Arrangement.spacedBy(NanoSpacing.lg)) {
    QuickActionsPanel(
      actions = quickActions,
      expanded = toolsExpanded,
      onToggle = { if (quickActions.isNotEmpty()) toolsExpanded = !toolsExpanded },
      onQuickActionSelect = { action ->
        handleQuickAction(
          action = action,
          onQuickActionSelect = onQuickActionSelect,
          onModeSelect = onModeSelect,
          onConfirmationReset = { recentConfirmation = null },
        )
      },
    )

    ModeSection(columns = columnCount, modeCards = modeCards, onModeSelect = onModeSelect)
    QueuedJobsSection(progressJobs, onProgressRetry, onProgressDismiss)
    RecentActivitySection(
      recentActivity = recentActivity,
      modeById = modeById,
      onRecentActivitySelect = {
        recentConfirmation = it.title
        onRecentActivitySelect(it)
      },
    )
    RecentConfirmationBanner(recentConfirmation)
  }
}

private fun handleQuickAction(
  action: CommandAction,
  onQuickActionSelect: (CommandAction) -> Unit,
  onModeSelect: (ModeId) -> Unit,
  onConfirmationReset: () -> Unit,
) {
  onQuickActionSelect(action)
  val modeToActivate = (action.destination as? CommandDestination.Navigate)?.route?.toModeIdOrNull()
  if (modeToActivate != null) {
    onModeSelect(modeToActivate)
  }
  onConfirmationReset()
}

@Composable
private fun ModeSection(columns: Int, modeCards: List<ModeCard>, onModeSelect: (ModeId) -> Unit) {
  NanoSection(title = "Modes") {
    ModeGrid(columns = columns, modeCards = modeCards, onModeSelect = onModeSelect)
  }
}

@Composable
private fun QueuedJobsSection(
  progressJobs: List<ProgressJob>,
  onProgressRetry: (ProgressJob) -> Unit,
  onProgressDismiss: (ProgressJob) -> Unit,
) {
  if (progressJobs.isEmpty()) return

  NanoSection(title = "Queued jobs") {
    ProgressCenterPanel(
      jobs = progressJobs,
      onRetry = onProgressRetry,
      onDismissJob = onProgressDismiss,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
private fun RecentConfirmationBanner(recentConfirmation: String?) {
  recentConfirmation ?: return

  Surface(
    modifier = Modifier.fillMaxWidth().testTag("home_recent_action_confirmation"),
    tonalElevation = NanoElevation.level1,
    shape = MaterialTheme.shapes.large,
  ) {
    Text(
      text = "$recentConfirmation ready to resume",
      modifier = Modifier.padding(NanoSpacing.md),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
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

private fun columnsForLayout(layout: ShellLayoutState): Int =
  when (layout.windowSizeClass.widthSizeClass) {
    ShellWindowWidthClass.COMPACT -> MODE_COLUMNS_COMPACT
    ShellWindowWidthClass.MEDIUM -> MODE_COLUMNS_MEDIUM
    ShellWindowWidthClass.EXPANDED -> MODE_COLUMNS_EXPANDED
  }
