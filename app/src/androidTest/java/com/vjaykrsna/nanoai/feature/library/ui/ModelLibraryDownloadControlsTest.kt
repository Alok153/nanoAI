package com.vjaykrsna.nanoai.feature.library.ui

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.library.DownloadStatus
import io.mockk.verify
import java.util.UUID
import kotlinx.datetime.Instant
import org.junit.Test

class ModelLibraryDownloadControlsTest : BaseModelLibraryScreenTest() {

  @Test
  fun showsProgressAndHandlesPauseResume() {
    val taskId = UUID.randomUUID()
    val downloadingTask =
      DownloadTask(
        taskId = taskId,
        modelId = "model-download",
        progress = 0.5f,
        status = DownloadStatus.DOWNLOADING,
        bytesDownloaded = 512,
        totalBytes = 1024,
        startedAt = Instant.DISTANT_PAST,
        finishedAt = null,
        errorMessage = null,
      )

    updateDownloadTasks(listOf(downloadingTask))
    renderModelLibraryScreen()

    composeTestRule
      .onNodeWithContentDescription("Downloading 50%", useUnmergedTree = true)
      .assertExists()

    composeTestRule
      .onNodeWithContentDescription("Pause download", useUnmergedTree = true)
      .performClick()
    composeTestRule.waitForIdle()
    verify { downloadCoordinator.pauseDownload(taskId) }

    val pausedTask = downloadingTask.copy(status = DownloadStatus.PAUSED)
    updateDownloadTasks(listOf(pausedTask))

    composeTestRule
      .onNodeWithContentDescription("Resume download", useUnmergedTree = true)
      .performClick()
    composeTestRule.waitForIdle()
    verify { downloadCoordinator.resumeDownload(taskId) }
  }
}
