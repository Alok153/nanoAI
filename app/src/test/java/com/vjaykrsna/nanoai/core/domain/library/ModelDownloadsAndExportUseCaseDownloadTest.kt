package com.vjaykrsna.nanoai.core.domain.library

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.library.DownloadStatus
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import com.vjaykrsna.nanoai.testing.DomainTestBuilders
import com.vjaykrsna.nanoai.testing.assertRecoverableError
import com.vjaykrsna.nanoai.testing.assertSuccess
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import java.util.UUID
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class ModelDownloadsAndExportUseCaseDownloadTest :
  ModelDownloadsAndExportUseCaseTestBase() {

  @Test
  fun `downloadModel enqueues when limit reached`() = runTest {
    val activeDownloads =
      listOf(
        DomainTestBuilders.buildDownloadTask(status = DownloadStatus.DOWNLOADING),
        DomainTestBuilders.buildDownloadTask(status = DownloadStatus.DOWNLOADING),
      )
    coEvery { downloadManager.getActiveDownloads() } returns flowOf(activeDownloads)
    coEvery { downloadManager.getMaxConcurrentDownloads() } returns 2

    val result = useCase.downloadModel("gemini-2.0-flash-lite")

    result.assertSuccess()
    coVerify { downloadManager.queueDownload("gemini-2.0-flash-lite") }
    coVerify(exactly = 0) { downloadManager.startDownload("gemini-2.0-flash-lite") }
    coVerify {
      modelCatalogRepository.updateInstallState("gemini-2.0-flash-lite", InstallState.DOWNLOADING)
    }
  }

  @Test
  fun `downloadModel starts immediately under limit`() = runTest {
    val activeDownloads =
      listOf(DomainTestBuilders.buildDownloadTask(status = DownloadStatus.DOWNLOADING))
    coEvery { downloadManager.getActiveDownloads() } returns flowOf(activeDownloads)
    coEvery { downloadManager.getMaxConcurrentDownloads() } returns 3

    val result = useCase.downloadModel("phi-3-mini-4k")

    result.assertSuccess()
    coVerify { downloadManager.startDownload("phi-3-mini-4k") }
  }

  @Test
  fun `downloadModel returns recoverable when manager fails`() = runTest {
    coEvery { downloadManager.getActiveDownloads() } throws IllegalStateException("down")

    val result = useCase.downloadModel("faulty-model")

    result.assertRecoverableError()
  }

  @Test
  fun `downloadModel rethrows cancellation`() = runTest {
    coEvery { downloadManager.getActiveDownloads() } throws CancellationException("cancel")

    assertFailsWith<CancellationException> { useCase.downloadModel("cancel-model") }
  }

  @Test
  fun `pauseDownload updates status`() = runTest {
    val taskId = UUID.randomUUID()

    useCase.pauseDownload(taskId)

    coVerify { downloadManager.pauseDownload(taskId) }
    coVerify { downloadManager.updateTaskStatus(taskId, DownloadStatus.PAUSED) }
  }

  @Test
  fun `resumeDownload returns recoverable when manager throws`() = runTest {
    val taskId = UUID.randomUUID()
    coEvery { downloadManager.getTaskById(taskId) } throws IllegalStateException("db")

    val result = useCase.resumeDownload(taskId)

    result.assertRecoverableError()
  }

  @Test
  fun `resumeDownload restarts paused task`() = runTest {
    val taskId = UUID.randomUUID()
    coEvery { downloadManager.getTaskById(taskId) } returns
      flowOf(DomainTestBuilders.buildDownloadTask(taskId = taskId, status = DownloadStatus.PAUSED))

    useCase.resumeDownload(taskId)

    coVerify { downloadManager.resumeDownload(taskId) }
    coVerify { downloadManager.updateTaskStatus(taskId, DownloadStatus.DOWNLOADING) }
  }

  @Test
  fun `resumeDownload returns recoverable when task not paused`() = runTest {
    val taskId = UUID.randomUUID()
    coEvery { downloadManager.getTaskById(taskId) } returns
      flowOf(
        DomainTestBuilders.buildDownloadTask(taskId = taskId, status = DownloadStatus.DOWNLOADING)
      )

    val result = useCase.resumeDownload(taskId)

    result.assertRecoverableError()
    coVerify(exactly = 0) { downloadManager.resumeDownload(taskId) }
  }

  @Test
  fun `cancelDownload cleans up artifacts`() = runTest {
    val taskId = UUID.randomUUID()
    coEvery { downloadManager.getModelIdForTask(taskId) } returns "test-model"

    useCase.cancelDownload(taskId)

    coVerify { downloadManager.cancelDownload(taskId) }
    coVerify { downloadManager.deletePartialFiles("test-model") }
    coVerify { modelCatalogRepository.updateInstallState("test-model", InstallState.NOT_INSTALLED) }
  }

  @Test
  fun `getDownloadProgress proxies flow`() = runTest {
    val taskId = UUID.randomUUID()
    val progressFlow = flowOf(0.0f, 0.25f, 1.0f)
    every { downloadManager.observeProgress(taskId) } returns progressFlow

    useCase.getDownloadProgress(taskId).test {
      assertThat(awaitItem()).isEqualTo(0.0f)
      assertThat(awaitItem()).isEqualTo(0.25f)
      assertThat(awaitItem()).isEqualTo(1.0f)
      awaitComplete()
    }
  }

  @Test
  fun `observeDownloadTasks emits managed list`() = runTest {
    val queue = listOf(DomainTestBuilders.buildDownloadTask(status = DownloadStatus.QUEUED))
    every { downloadManager.observeManagedDownloads() } returns flowOf(queue)

    useCase.observeDownloadTasks().test {
      val emission = awaitItem()
      assertThat(emission).hasSize(1)
      assertThat(emission.first().status).isEqualTo(DownloadStatus.QUEUED)
      awaitComplete()
    }
  }

  @Test
  fun `retryFailedDownload resets and restarts`() = runTest {
    val taskId = UUID.randomUUID()
    val modelId = "failed-model"
    coEvery { downloadManager.getTaskById(taskId) } returns
      flowOf(
        DomainTestBuilders.buildDownloadTask(
          taskId = taskId,
          modelId = modelId,
          status = DownloadStatus.FAILED,
        )
      )
    coEvery { downloadManager.getModelIdForTask(taskId) } returns modelId

    useCase.retryFailedDownload(taskId)

    coVerify { downloadManager.resetTask(taskId) }
    coVerify { downloadManager.startDownload(modelId) }
    coVerify { modelCatalogRepository.updateInstallState(modelId, InstallState.DOWNLOADING) }
  }

  @Test
  fun `retryFailedDownload returns recoverable when manager throws`() = runTest {
    val taskId = UUID.randomUUID()
    coEvery { downloadManager.getTaskById(taskId) } throws IllegalStateException("db")

    val result = useCase.retryFailedDownload(taskId)

    result.assertRecoverableError()
  }

  @Test
  fun `retryFailedDownload returns recoverable when task not failed`() = runTest {
    val taskId = UUID.randomUUID()
    coEvery { downloadManager.getTaskById(taskId) } returns
      flowOf(
        DomainTestBuilders.buildDownloadTask(taskId = taskId, status = DownloadStatus.DOWNLOADING)
      )

    val result = useCase.retryFailedDownload(taskId)

    result.assertRecoverableError()
    coVerify(exactly = 0) { downloadManager.resetTask(taskId) }
  }

  @Test
  fun `retryFailedDownload returns recoverable when model id missing`() = runTest {
    val taskId = UUID.randomUUID()
    coEvery { downloadManager.getTaskById(taskId) } returns
      flowOf(DomainTestBuilders.buildDownloadTask(taskId = taskId, status = DownloadStatus.FAILED))
    coEvery { downloadManager.getModelIdForTask(taskId) } returns null

    val result = useCase.retryFailedDownload(taskId)

    result.assertRecoverableError()
  }
}
