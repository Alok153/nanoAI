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
  suspend fun pauseDownload(taskId: UUID): NanoAIResult<Unit> {
    return try {
      downloadManager.pauseDownload(taskId)
      downloadManager.updateTaskStatus(taskId, DownloadStatus.PAUSED)
      NanoAIResult.success(Unit)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to pause download $taskId",
        cause = e,
        context = mapOf("taskId" to taskId.toString()),
      )
    }
  }

  /** Resume a paused download. */
  suspend fun resumeDownload(taskId: UUID): NanoAIResult<Unit> {
    return try {
      val task = downloadManager.getTaskById(taskId).first()
      val result =
        if (task == null) {
          NanoAIResult.recoverable(
            message = "Download task $taskId not found",
            context = mapOf("taskId" to taskId.toString()),
          )
        } else if (task.status != DownloadStatus.PAUSED) {
          NanoAIResult.recoverable(
            message = "Download task $taskId is not paused",
            context = mapOf("taskId" to taskId.toString(), "status" to task.status.toString()),
          )
        } else {
          downloadManager.resumeDownload(taskId)
          downloadManager.updateTaskStatus(taskId, DownloadStatus.DOWNLOADING)
          NanoAIResult.success(Unit)
        }
      result
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to resume download $taskId",
        cause = e,
        context = mapOf("taskId" to taskId.toString()),
      )
    }
  }

  /** Cancel a download and cleanup associated files. */
  suspend fun cancelDownload(taskId: UUID): NanoAIResult<Unit> {
    return try {
      val modelId = downloadManager.getModelIdForTask(taskId)
      downloadManager.cancelDownload(taskId)
      modelId?.let {
        downloadManager.deletePartialFiles(it)
        modelCatalogRepository.updateInstallState(it, InstallState.NOT_INSTALLED)
        modelCatalogRepository.updateDownloadTaskId(it, null)
      }
      NanoAIResult.success(Unit)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to cancel download $taskId",
        cause = e,
        context = mapOf("taskId" to taskId.toString()),
      )
    }
  }

  /** Retry a failed download task. */
  suspend fun retryFailedDownload(taskId: UUID): NanoAIResult<Unit> {
    return try {
      val task = downloadManager.getTaskById(taskId).first()
      val modelId = downloadManager.getModelIdForTask(taskId)

      val result =
        if (task == null) {
          NanoAIResult.recoverable(
            message = "Download task $taskId not found",
            context = mapOf("taskId" to taskId.toString()),
          )
        } else if (task.status != DownloadStatus.FAILED) {
          NanoAIResult.recoverable(
            message = "Download task $taskId is not failed",
            context = mapOf("taskId" to taskId.toString(), "status" to task.status.toString()),
          )
        } else if (modelId == null) {
          NanoAIResult.recoverable(
            message = "Model ID not found for task $taskId",
            context = mapOf("taskId" to taskId.toString()),
          )
        } else {
          downloadManager.resetTask(taskId)
          downloadManager.startDownload(modelId)
          modelCatalogRepository.updateInstallState(modelId, InstallState.DOWNLOADING)
          modelCatalogRepository.updateDownloadTaskId(modelId, taskId)
          NanoAIResult.success(Unit)
        }
      result
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to retry download $taskId",
        cause = e,
        context = mapOf("taskId" to taskId.toString()),
      )
    }
  }

  /** Observe download progress as a Flow. */
  fun getDownloadProgress(taskId: UUID): Flow<Float> = downloadManager.observeProgress(taskId)

  /** Observe a specific download task for status or error updates. */
  suspend fun observeDownloadTask(taskId: UUID): Flow<DownloadTask?> =
    downloadManager.getTaskById(taskId)

  /** Observe queued download tasks. */
  fun observeDownloadTasks(): Flow<List<DownloadTask>> = downloadManager.observeManagedDownloads()
}
