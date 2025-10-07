package com.vjaykrsna.nanoai.feature.uiux.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.uiux.state.JobStatus
import com.vjaykrsna.nanoai.feature.uiux.state.JobType
import com.vjaykrsna.nanoai.feature.uiux.state.ProgressJob

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
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        text = "Progress center",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
      )

      if (jobs.isEmpty()) {
        Text(
          text = "No queued tasks",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 48.dp).align(Alignment.CenterHorizontally),
        )
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).testTag("progress_list"),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(jobs, key = { it.jobId }) { job ->
            ProgressJobItem(
              job = job,
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
  onRetry: (ProgressJob) -> Unit,
  onDismiss: (ProgressJob) -> Unit,
) {
  Surface(
    modifier =
      Modifier.fillMaxWidth().testTag("progress_list_item").semantics {
        val percent = (job.normalizedProgress * 100).toInt().coerceIn(0, 100)
        contentDescription = buildString {
          append(job.type.label)
          append(", ")
          append(job.statusLabel)
          append(", ")
          append(percent)
          append(" percent complete")
        }
      },
    tonalElevation = 1.dp,
    shape = RoundedCornerShape(16.dp),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
        val percent = (job.normalizedProgress * 100).toInt().coerceIn(0, 100)
        Text(
          text = "$percent%",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(start = 12.dp),
        )
      }

      LinearProgressIndicator(
        progress = { job.normalizedProgress },
        modifier =
          Modifier.fillMaxWidth().semantics {
            val percent = (job.normalizedProgress * 100).toInt().coerceIn(0, 100)
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
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Button(
            onClick = { onRetry(job) },
            enabled = job.canRetryNow,
            modifier =
              Modifier.testTag("progress_retry_button").semantics {
                stateDescription = if (job.canRetryNow) "Retry available" else "Retry unavailable"
              },
          ) {
            Text("Retry")
          }
          if (job.status == JobStatus.COMPLETED) {
            TextButton(onClick = { onDismiss(job) }) { Text("Clear") }
          }
        }
      }

      HorizontalDivider()
      Text(
        text = "Queued at ${job.queuedAt}",
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
