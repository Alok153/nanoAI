package com.vjaykrsna.nanoai.feature.uiux.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.R
import com.vjaykrsna.nanoai.core.domain.model.uiux.JobStatus
import com.vjaykrsna.nanoai.core.domain.model.uiux.ProgressJob
import com.vjaykrsna.nanoai.core.domain.model.uiux.label
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoElevation

private val PROGRESS_ITEM_CORNER_RADIUS = 16.dp
private val PROGRESS_ITEM_HORIZONTAL_PADDING = 16.dp
private val PROGRESS_ITEM_VERTICAL_PADDING = 12.dp
private val PROGRESS_ITEM_SPACING = 8.dp
private val PROGRESS_SUBTITLE_SPACING = 2.dp
private val PROGRESS_PERCENT_LABEL_START_PADDING = 12.dp
private val PROGRESS_ACTION_SPACING = 8.dp
private const val PROGRESS_PERCENT_SCALE = 100
private const val PROGRESS_PERCENT_MIN = 0
private const val PROGRESS_PERCENT_MAX = 100

@Composable
internal fun ProgressJobItem(
  job: ProgressJob,
  index: Int,
  onRetry: (ProgressJob) -> Unit,
  onDismiss: (ProgressJob) -> Unit,
) {
  val percent = jobProgressPercent(job.normalizedProgress)
  Box(
    modifier =
      Modifier.fillMaxWidth().testTag("progress_list_item_$index").semantics(
        mergeDescendants = true
      ) {}
  ) {
    Surface(
      modifier =
        Modifier.fillMaxWidth().semantics {
          this[SemanticsProperties.ContentDescription] =
            listOf(job.type.label, job.statusLabel, "$percent percent complete")
        },
      tonalElevation = NanoElevation.level1,
      shape = RoundedCornerShape(PROGRESS_ITEM_CORNER_RADIUS),
    ) {
      Column(
        modifier =
          Modifier.padding(
            horizontal = PROGRESS_ITEM_HORIZONTAL_PADDING,
            vertical = PROGRESS_ITEM_VERTICAL_PADDING,
          ),
        verticalArrangement = Arrangement.spacedBy(PROGRESS_ITEM_SPACING),
      ) {
        ProgressJobHeader(job = job, percent = percent)
        ProgressJobProgress(job = job, percent = percent)
        ProgressJobActions(job = job, onRetry = onRetry, onDismiss = onDismiss)
        HorizontalDivider()
        ProgressJobFooter(job = job)
      }
    }
  }
}

@Composable
private fun ProgressJobHeader(job: ProgressJob, percent: Int) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(PROGRESS_SUBTITLE_SPACING),
    ) {
      Text(
        text = job.type.label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(text = job.statusLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Text(
      text = "$percent%",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(start = PROGRESS_PERCENT_LABEL_START_PADDING),
    )
  }
}

@Composable
private fun ProgressJobProgress(job: ProgressJob, percent: Int) {
  LinearProgressIndicator(
    progress = { job.normalizedProgress },
    modifier =
      Modifier.fillMaxWidth().semantics {
        contentDescription = "${job.type.label} progress $percent percent"
        progressBarRangeInfo = ProgressBarRangeInfo(job.normalizedProgress, 0f..1f)
        stateDescription = "$percent percent"
      },
  )
}

@Composable
private fun ProgressJobActions(
  job: ProgressJob,
  onRetry: (ProgressJob) -> Unit,
  onDismiss: (ProgressJob) -> Unit,
) {
  val labels = jobActionLabels(job)
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = job.type.label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    ProgressJobActionButtons(job = job, labels = labels, onRetry = onRetry, onDismiss = onDismiss)
  }
}

@Composable
private fun jobActionLabels(job: ProgressJob): JobActionLabels {
  val retryContentDescription =
    stringResource(
      R.string.progress_center_panel_retry_content_description,
      job.type.label.lowercase(),
    )
  val retryStateDescription =
    if (job.canRetryNow) {
      stringResource(R.string.progress_center_panel_retry_available)
    } else {
      stringResource(R.string.progress_center_panel_retry_unavailable)
    }
  val clearContentDescription =
    stringResource(
      R.string.progress_center_panel_clear_content_description,
      job.type.label.lowercase(),
    )
  val clearStateDescription =
    if (job.status == JobStatus.COMPLETED) {
      stringResource(R.string.progress_center_panel_clear_completed)
    } else {
      stringResource(R.string.progress_center_panel_clear_failed)
    }

  return JobActionLabels(
    retryContentDescription = retryContentDescription,
    retryStateDescription = retryStateDescription,
    clearContentDescription = clearContentDescription,
    clearStateDescription = clearStateDescription,
  )
}

@Composable
private fun ProgressJobActionButtons(
  job: ProgressJob,
  labels: JobActionLabels,
  onRetry: (ProgressJob) -> Unit,
  onDismiss: (ProgressJob) -> Unit,
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(PROGRESS_ACTION_SPACING),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (job.status == JobStatus.FAILED) {
      ProgressRetryButton(job = job, labels = labels, onRetry = onRetry)
    }

    if (job.status == JobStatus.COMPLETED || job.status == JobStatus.FAILED) {
      ProgressClearButton(job = job, labels = labels, onDismiss = onDismiss)
    }
  }
}

@Composable
private fun ProgressRetryButton(
  job: ProgressJob,
  labels: JobActionLabels,
  onRetry: (ProgressJob) -> Unit,
) {
  val handleRetryAction: () -> Boolean = {
    if (!job.canRetryNow) {
      false
    } else {
      onRetry(job)
      true
    }
  }
  Button(
    onClick = { handleRetryAction() },
    enabled = job.canRetryNow,
    modifier =
      Modifier.testTag("progress_retry_button_${job.jobId}").semantics {
        contentDescription = labels.retryContentDescription
        stateDescription = labels.retryStateDescription
        onClick(label = labels.retryContentDescription, action = handleRetryAction)
      },
  ) {
    Text(stringResource(R.string.progress_center_panel_retry))
  }
}

@Composable
private fun ProgressClearButton(
  job: ProgressJob,
  labels: JobActionLabels,
  onDismiss: (ProgressJob) -> Unit,
) {
  TextButton(
    onClick = { onDismiss(job) },
    modifier =
      Modifier.testTag("progress_clear_button").semantics {
        contentDescription = labels.clearContentDescription
        stateDescription = labels.clearStateDescription
      },
  ) {
    Text(stringResource(R.string.progress_center_panel_clear))
  }
}

@Composable
private fun ProgressJobFooter(job: ProgressJob) {
  Text(
    text = stringResource(R.string.progress_center_panel_queued_at, job.queuedAt),
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

private data class JobActionLabels(
  val retryContentDescription: String,
  val retryStateDescription: String,
  val clearContentDescription: String,
  val clearStateDescription: String,
)

private fun jobProgressPercent(progress: Float): Int =
  (progress * PROGRESS_PERCENT_SCALE).toInt().coerceIn(PROGRESS_PERCENT_MIN, PROGRESS_PERCENT_MAX)
