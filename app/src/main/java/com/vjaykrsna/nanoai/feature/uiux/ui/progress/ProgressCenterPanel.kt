package com.vjaykrsna.nanoai.feature.uiux.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.R
import com.vjaykrsna.nanoai.feature.uiux.state.JobStatus
import com.vjaykrsna.nanoai.feature.uiux.state.JobType
import com.vjaykrsna.nanoai.feature.uiux.state.ProgressJob
import com.vjaykrsna.nanoai.feature.uiux.state.label

private val PROGRESS_PANEL_HORIZONTAL_PADDING = 20.dp
private val PROGRESS_PANEL_VERTICAL_PADDING = 16.dp
private val PROGRESS_SECTION_SPACING = 16.dp
private val PROGRESS_EMPTY_STATE_TOP_PADDING = 48.dp
private val PROGRESS_LIST_MAX_HEIGHT = 420.dp
private val PROGRESS_LIST_ITEM_SPACING = 12.dp
private val PROGRESS_ITEM_HORIZONTAL_PADDING = 16.dp
private val PROGRESS_ITEM_VERTICAL_PADDING = 12.dp
private val PROGRESS_ITEM_SPACING = 8.dp
private val PROGRESS_SUBTITLE_SPACING = 2.dp
private val PROGRESS_ACTION_SPACING = 8.dp
private val PROGRESS_PERCENT_LABEL_START_PADDING = 12.dp
private val PROGRESS_PANEL_CORNER_RADIUS = 24.dp
private val PROGRESS_ITEM_CORNER_RADIUS = 16.dp
private const val PROGRESS_PERCENT_SCALE = 100
private const val PROGRESS_PERCENT_MIN = 0
private const val PROGRESS_PERCENT_MAX = 100

/** Displays the current queue of background jobs inside the progress center. */
@Composable
fun ProgressCenterPanel(
  jobs: List<ProgressJob>,
  onRetry: (ProgressJob) -> Unit,
  onDismissJob: (ProgressJob) -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.testTag("progress_center_panel"),
    tonalElevation = 3.dp,
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
      Text(
        text = stringResource(R.string.progress_center_panel_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
      )

      if (jobs.isEmpty()) {
        Text(
          text = stringResource(R.string.progress_center_panel_no_tasks),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier =
            Modifier.padding(top = PROGRESS_EMPTY_STATE_TOP_PADDING)
              .align(Alignment.CenterHorizontally),
        )
      } else {
        LazyColumn(
          modifier =
            Modifier.fillMaxWidth()
              .heightIn(max = PROGRESS_LIST_MAX_HEIGHT)
              .testTag("progress_list"),
          verticalArrangement = Arrangement.spacedBy(PROGRESS_LIST_ITEM_SPACING),
        ) {
          itemsIndexed(jobs, key = { _, job -> job.jobId }) { index, job ->
            ProgressJobItem(
              job = job,
              index = index,
              onRetry = onRetry,
              onDismiss = onDismissJob,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ProgressJobItem(
  job: ProgressJob,
  index: Int,
  onRetry: (ProgressJob) -> Unit,
  onDismiss: (ProgressJob) -> Unit,
) {
  Surface(
    modifier =
      Modifier.fillMaxWidth().testTag("progress_list_item_$index").semantics {
        val percent =
          (job.normalizedProgress * PROGRESS_PERCENT_SCALE)
            .toInt()
            .coerceIn(PROGRESS_PERCENT_MIN, PROGRESS_PERCENT_MAX)
        contentDescription = buildString {
          append(job.accessibilityLabel)
          append(", ")
          append(percent)
          append(" percent complete")
        }
      },
    tonalElevation = 1.dp,
    shape = RoundedCornerShape(PROGRESS_ITEM_CORNER_RADIUS),
  ) {
    Column(
      modifier =
        Modifier.padding(
          horizontal = PROGRESS_ITEM_HORIZONTAL_PADDING,
          vertical = PROGRESS_ITEM_VERTICAL_PADDING,
        ),
      verticalArrangement = Arrangement.spacedBy(PROGRESS_ITEM_SPACING)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
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
          Text(
            text = job.statusLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        val percent =
          (job.normalizedProgress * PROGRESS_PERCENT_SCALE)
            .toInt()
            .coerceIn(PROGRESS_PERCENT_MIN, PROGRESS_PERCENT_MAX)
        Text(
          text = "$percent%",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(start = PROGRESS_PERCENT_LABEL_START_PADDING),
        )
      }

      LinearProgressIndicator(
        progress = { job.normalizedProgress },
        modifier =
          Modifier.fillMaxWidth().semantics {
            val percent =
              (job.normalizedProgress * PROGRESS_PERCENT_SCALE)
                .toInt()
                .coerceIn(PROGRESS_PERCENT_MIN, PROGRESS_PERCENT_MAX)
            contentDescription = "${job.type.label} progress $percent percent"
            progressBarRangeInfo = ProgressBarRangeInfo(job.normalizedProgress, 0f..1f)
            stateDescription = "$percent percent"
          },
      )

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

        val showRetry = job.status == JobStatus.FAILED
        val showClear = job.status == JobStatus.COMPLETED || job.status == JobStatus.FAILED

        // Resolve strings outside semantics blocks
        val retryContentDescription =
          stringResource(
            R.string.progress_center_panel_retry_content_description,
            job.type.label.lowercase()
          )
        val retryStateDescription =
          if (job.canRetryNow) stringResource(R.string.progress_center_panel_retry_available)
          else stringResource(R.string.progress_center_panel_retry_unavailable)
        val clearContentDescription =
          stringResource(
            R.string.progress_center_panel_clear_content_description,
            job.type.label.lowercase()
          )
        val clearStateDescription =
          if (job.status == JobStatus.COMPLETED) {
            stringResource(R.string.progress_center_panel_clear_completed)
          } else {
            stringResource(R.string.progress_center_panel_clear_failed)
          }

        Row(
          horizontalArrangement = Arrangement.spacedBy(PROGRESS_ACTION_SPACING),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (showRetry) {
            Button(
              onClick = { onRetry(job) },
              enabled = job.canRetryNow,
              modifier =
                Modifier.testTag("progress_retry_button_${job.jobId}").semantics {
                  contentDescription = retryContentDescription
                  stateDescription = retryStateDescription
                },
            ) {
              Text(stringResource(R.string.progress_center_panel_retry))
            }
          }

          if (showClear) {
            TextButton(
              onClick = { onDismiss(job) },
              modifier =
                Modifier.testTag("progress_clear_button").semantics {
                  contentDescription = clearContentDescription
                  stateDescription = clearStateDescription
                },
            ) {
              Text(stringResource(R.string.progress_center_panel_clear))
            }
          }
        }
      }

      HorizontalDivider()
      Text(
        text = stringResource(R.string.progress_center_panel_queued_at, job.queuedAt),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

private val JobType.label: String
  get() =
    when (this) {
      JobType.IMAGE_GENERATION -> "Image generation"
      JobType.AUDIO_RECORDING -> "Audio recording"
      JobType.MODEL_DOWNLOAD -> "Model download"
      JobType.TEXT_GENERATION -> "Text generation"
      JobType.TRANSLATION -> "Translation"
      JobType.OTHER -> "Background task"
    }
