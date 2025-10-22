package com.vjaykrsna.nanoai.feature.library.domain

import com.vjaykrsna.nanoai.feature.library.data.DownloadManager
import com.vjaykrsna.nanoai.feature.library.data.ModelCatalogRepository
import com.vjaykrsna.nanoai.feature.library.model.DownloadStatus
import com.vjaykrsna.nanoai.feature.library.model.InstallState
import com.vjaykrsna.nanoai.testing.assertRecoverableError
import com.vjaykrsna.nanoai.testing.assertSuccess
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class DownloadModelUseCaseTest {
  private lateinit var useCase: DownloadModelUseCase
  private lateinit var modelCatalogRepository: ModelCatalogRepository
  private lateinit var downloadManager: DownloadManager

  private val modelId = "test-model"
  private val taskId = UUID.randomUUID()

  @Before
  fun setup() {
    modelCatalogRepository = mockk(relaxed = true)
    downloadManager = mockk(relaxed = true)

    useCase =
      DownloadModelUseCase(
        modelCatalogRepository = modelCatalogRepository,
        downloadManager = downloadManager,
      )
  }

  @Test
  fun `downloadModel starts download when under concurrency limit`() = runTest {
    val activeDownloads = listOf(createDownloadTask(DownloadStatus.DOWNLOADING))

    coEvery { downloadManager.getActiveDownloads() } returns flowOf(activeDownloads)
    coEvery { downloadManager.getMaxConcurrentDownloads() } returns 3
    coEvery { downloadManager.startDownload(modelId) } returns taskId

    val result = useCase.downloadModel(modelId)

    val returnedTaskId = result.assertSuccess()
    assert(returnedTaskId == taskId)
    coVerify { downloadManager.startDownload(modelId) }
    coVerify { modelCatalogRepository.updateInstallState(modelId, InstallState.DOWNLOADING) }
    coVerify { modelCatalogRepository.updateDownloadTaskId(modelId, taskId) }
  }

  @Test
  fun `downloadModel queues download when at concurrency limit`() = runTest {
    val activeDownloads =
      listOf(
        createDownloadTask(DownloadStatus.DOWNLOADING),
        createDownloadTask(DownloadStatus.DOWNLOADING),
      )

    coEvery { downloadManager.getActiveDownloads() } returns flowOf(activeDownloads)
    coEvery { downloadManager.getMaxConcurrentDownloads() } returns 2
    coEvery { downloadManager.queueDownload(modelId) } returns taskId

    val result = useCase.downloadModel(modelId)

    val returnedTaskId = result.assertSuccess()
    assert(returnedTaskId == taskId)
    coVerify { downloadManager.queueDownload(modelId) }
    coVerify(exactly = 0) { downloadManager.startDownload(modelId) }
  }

  @Test
  fun `downloadModel returns recoverable error when download fails`() = runTest {
    val exception = RuntimeException("Download failed")
    coEvery { downloadManager.getActiveDownloads() } throws exception

    val result = useCase.downloadModel(modelId)

    result.assertRecoverableError()
  }

  @Test
  fun `pauseDownload succeeds and updates status`() = runTest {
    val result = useCase.pauseDownload(taskId)

    result.assertSuccess()
    coVerify { downloadManager.pauseDownload(taskId) }
    coVerify { downloadManager.updateTaskStatus(taskId, DownloadStatus.PAUSED) }
  }

  @Test
  fun `resumeDownload succeeds when task is paused`() = runTest {
    val pausedTask = createDownloadTask(DownloadStatus.PAUSED, taskId)
    coEvery { downloadManager.getTaskById(taskId) } returns flowOf(pausedTask)

    val result = useCase.resumeDownload(taskId)

    result.assertSuccess()
    coVerify { downloadManager.resumeDownload(taskId) }
    coVerify { downloadManager.updateTaskStatus(taskId, DownloadStatus.DOWNLOADING) }
  }

  @Test
  fun `resumeDownload fails when task not found`() = runTest {
    coEvery { downloadManager.getTaskById(taskId) } returns flowOf(null)

    val result = useCase.resumeDownload(taskId)

    result.assertRecoverableError()
  }

  @Test
  fun `cancelDownload cleans up files and resets state`() = runTest {
    coEvery { downloadManager.getModelIdForTask(taskId) } returns modelId

    val result = useCase.cancelDownload(taskId)

    result.assertSuccess()
    coVerify { downloadManager.cancelDownload(taskId) }
    coVerify { downloadManager.deletePartialFiles(modelId) }
    coVerify { modelCatalogRepository.updateInstallState(modelId, InstallState.NOT_INSTALLED) }
    coVerify { modelCatalogRepository.updateDownloadTaskId(modelId, null) }
  }

  private fun createDownloadTask(status: DownloadStatus, id: UUID = UUID.randomUUID()) =
    com.vjaykrsna.nanoai.core.domain.model.DownloadTask(
      taskId = id,
      modelId = "some-model",
      progress = 0.5f,
      status = status,
      bytesDownloaded = 1000,
      startedAt = null,
      finishedAt = null,
      errorMessage = null,
    )
}
