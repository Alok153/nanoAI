package com.vjaykrsna.nanoai.feature.uiux.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.R
import com.vjaykrsna.nanoai.core.domain.model.uiux.ProgressJob
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoElevation

private val PROGRESS_PANEL_HORIZONTAL_PADDING = 20.dp
private val PROGRESS_PANEL_VERTICAL_PADDING = 16.dp
private val PROGRESS_SECTION_SPACING = 16.dp
private val PROGRESS_EMPTY_STATE_TOP_PADDING = 48.dp
private val PROGRESS_LIST_MAX_HEIGHT = 420.dp
private val PROGRESS_LIST_ITEM_SPACING = 12.dp
private val PROGRESS_PANEL_CORNER_RADIUS = 24.dp
private const val PROGRESS_INLINE_ITEM_THRESHOLD = 4

/** Displays the current queue of background jobs inside the progress center. */
@Composable
fun ProgressCenterPanel(
  jobs: List<ProgressJob>,
  onRetry: (ProgressJob) -> Unit,
  onDismissJob: (ProgressJob) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.testTag("progress_center_panel")) {
    Surface(
      modifier = Modifier.fillMaxWidth(),
      tonalElevation = NanoElevation.level2,
      shape =
        RoundedCornerShape(
          topStart = PROGRESS_PANEL_CORNER_RADIUS,
          topEnd = PROGRESS_PANEL_CORNER_RADIUS,
        ),
    ) {
      Column(
        modifier =
          Modifier.padding(
            horizontal = PROGRESS_PANEL_HORIZONTAL_PADDING,
            vertical = PROGRESS_PANEL_VERTICAL_PADDING,
          ),
        verticalArrangement = Arrangement.spacedBy(PROGRESS_SECTION_SPACING),
      ) {
        ProgressPanelTitle()
        ProgressPanelBody(jobs = jobs, onRetry = onRetry, onDismissJob = onDismissJob)
      }
    }
  }
}

@Composable
private fun ProgressPanelTitle() {
  Text(
    text = stringResource(R.string.progress_center_panel_title),
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.SemiBold,
  )
}

@Composable
private fun ColumnScope.ProgressPanelBody(
  jobs: List<ProgressJob>,
  onRetry: (ProgressJob) -> Unit,
  onDismissJob: (ProgressJob) -> Unit,
) {
  if (jobs.isEmpty()) {
    ProgressPanelEmptyState(modifier = Modifier.align(Alignment.CenterHorizontally))
  } else {
    ProgressJobList(jobs = jobs, onRetry = onRetry, onDismissJob = onDismissJob)
  }
}

@Composable
private fun ProgressPanelEmptyState(modifier: Modifier = Modifier) {
  Text(
    text = stringResource(R.string.progress_center_panel_no_tasks),
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = modifier.padding(top = PROGRESS_EMPTY_STATE_TOP_PADDING),
  )
}

@Composable
private fun ProgressJobList(
  jobs: List<ProgressJob>,
  onRetry: (ProgressJob) -> Unit,
  onDismissJob: (ProgressJob) -> Unit,
) {
  if (jobs.size <= PROGRESS_INLINE_ITEM_THRESHOLD) {
    ProgressInlineJobList(jobs = jobs, onRetry = onRetry, onDismissJob = onDismissJob)
  } else {
    ProgressLazyJobList(jobs = jobs, onRetry = onRetry, onDismissJob = onDismissJob)
  }
}

@Composable
private fun ProgressInlineJobList(
  jobs: List<ProgressJob>,
  onRetry: (ProgressJob) -> Unit,
  onDismissJob: (ProgressJob) -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth().testTag("progress_list"),
    verticalArrangement = Arrangement.spacedBy(PROGRESS_LIST_ITEM_SPACING),
  ) {
    jobs.forEachIndexed { index, job ->
      ProgressJobItem(job = job, index = index, onRetry = onRetry, onDismiss = onDismissJob)
    }
  }
}

@Composable
private fun ProgressLazyJobList(
  jobs: List<ProgressJob>,
  onRetry: (ProgressJob) -> Unit,
  onDismissJob: (ProgressJob) -> Unit,
) {
  LazyColumn(
    modifier =
      Modifier.fillMaxWidth().heightIn(max = PROGRESS_LIST_MAX_HEIGHT).testTag("progress_list"),
    verticalArrangement = Arrangement.spacedBy(PROGRESS_LIST_ITEM_SPACING),
  ) {
    itemsIndexed(jobs, key = { _, job -> job.jobId }) { index, job ->
      ProgressJobItem(job = job, index = index, onRetry = onRetry, onDismiss = onDismissJob)
    }
  }
}
