package com.vjaykrsna.nanoai.feature.library.domain

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.feature.library.data.DownloadManager
import com.vjaykrsna.nanoai.feature.library.data.ModelCatalogRepository
import com.vjaykrsna.nanoai.feature.library.model.DownloadStatus
import com.vjaykrsna.nanoai.feature.library.model.InstallState
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Use case for managing model download operations. */
@Singleton
class DownloadModelUseCase
@Inject
constructor(
  private val modelCatalogRepository: ModelCatalogRepository,
  private val downloadManager: DownloadManager,
) {
  /** Queue or start a download based on concurrency limits. */
  suspend fun downloadModel(modelId: String): NanoAIResult<UUID> {
    return try {
      val activeDownloads = downloadManager.getActiveDownloads().first()
      val activeCount = activeDownloads.count { it.status == DownloadStatus.DOWNLOADING }
      val maxConcurrent = downloadManager.getMaxConcurrentDownloads()

      val taskId =
        if (activeCount >= maxConcurrent) {
          downloadManager.queueDownload(modelId)
        } else {
          downloadManager.startDownload(modelId)
        }

      modelCatalogRepository.updateInstallState(modelId, InstallState.DOWNLOADING)
      modelCatalogRepository.updateDownloadTaskId(modelId, taskId)
      NanoAIResult.success(taskId)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to start download for model $modelId",
        cause = e,
        context = mapOf("modelId" to modelId),
      )
    }
  }

  /** Pause a download task and persist status. */
  suspend fun pauseDownload(taskId: UUID) {
    downloadManager.pauseDownload(taskId)
    downloadManager.updateTaskStatus(taskId, DownloadStatus.PAUSED)
  }

  /** Resume a paused download. */
  suspend fun resumeDownload(taskId: UUID) {
    val task = downloadManager.getTaskById(taskId).first() ?: return
    if (task.status != DownloadStatus.PAUSED) return

    downloadManager.resumeDownload(taskId)
    downloadManager.updateTaskStatus(taskId, DownloadStatus.DOWNLOADING)
  }

  /** Cancel a download and cleanup associated files. */
  suspend fun cancelDownload(taskId: UUID) {
    val modelId = downloadManager.getModelIdForTask(taskId)
    downloadManager.cancelDownload(taskId)
    modelId?.let {
      downloadManager.deletePartialFiles(it)
      modelCatalogRepository.updateInstallState(it, InstallState.NOT_INSTALLED)
      modelCatalogRepository.updateDownloadTaskId(it, null)
    }
  }

  /** Retry a failed download task. */
  suspend fun retryFailedDownload(taskId: UUID) {
    val task = downloadManager.getTaskById(taskId).first()
    val modelId = downloadManager.getModelIdForTask(taskId)

    if (task == null || task.status != DownloadStatus.FAILED || modelId == null) return

    downloadManager.resetTask(taskId)
    downloadManager.startDownload(modelId)
    modelCatalogRepository.updateInstallState(modelId, InstallState.DOWNLOADING)
    modelCatalogRepository.updateDownloadTaskId(modelId, taskId)
  }

  /** Observe download progress as a Flow. */
  fun getDownloadProgress(taskId: UUID): Flow<Float> = downloadManager.observeProgress(taskId)

  /** Observe a specific download task for status or error updates. */
  suspend fun observeDownloadTask(taskId: UUID): Flow<DownloadTask?> =
    downloadManager.getTaskById(taskId)

  /** Observe queued download tasks. */
  fun observeDownloadTasks(): Flow<List<DownloadTask>> = downloadManager.observeManagedDownloads()
}
