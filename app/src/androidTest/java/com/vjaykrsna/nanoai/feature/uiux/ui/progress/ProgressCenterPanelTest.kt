package com.vjaykrsna.nanoai.feature.uiux.ui.progress

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.vjaykrsna.nanoai.core.domain.model.uiux.JobStatus
import com.vjaykrsna.nanoai.core.domain.model.uiux.JobType
import com.vjaykrsna.nanoai.core.domain.model.uiux.ProgressJob
import com.vjaykrsna.nanoai.shared.testing.TestingTheme
import java.time.Instant
import java.util.UUID
import org.junit.Rule
import org.junit.Test

class ProgressCenterPanelTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun showsProgressPercentForRunningJob() {
    val job =
      ProgressJob(
        jobId = UUID.randomUUID(),
        type = JobType.IMAGE_GENERATION,
        status = JobStatus.RUNNING,
        progress = 0.5f,
        queuedAt = Instant.parse("2024-01-01T00:00:00Z"),
      )

    composeTestRule.setContent {
      TestingTheme { ProgressCenterPanel(jobs = listOf(job), onRetry = {}, onDismissJob = {}) }
    }

    composeTestRule.onNodeWithText("50%", substring = false, useUnmergedTree = true).assertExists()
    composeTestRule
      .onNodeWithTag("progress_retry_button_${job.jobId}", useUnmergedTree = true)
      .assertDoesNotExist()
  }

  @Test
  fun showsRetryButtonWhenJobFailed() {
    val job =
      ProgressJob(
        jobId = UUID.randomUUID(),
        type = JobType.MODEL_DOWNLOAD,
        status = JobStatus.FAILED,
        progress = 0.2f,
        canRetry = true,
        queuedAt = Instant.parse("2024-01-02T00:00:00Z"),
      )

    composeTestRule.setContent {
      TestingTheme { ProgressCenterPanel(jobs = listOf(job), onRetry = {}, onDismissJob = {}) }
    }

    composeTestRule
      .onNodeWithTag("progress_retry_button_${job.jobId}", useUnmergedTree = true)
      .assertExists()
      .assertIsEnabled()
    composeTestRule
      .onNodeWithText("Retry", substring = false, useUnmergedTree = true)
      .assertExists()
  }
}
