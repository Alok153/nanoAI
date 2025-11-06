package com.vjaykrsna.nanoai.core.domain.library

import android.database.sqlite.SQLiteException
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.library.DownloadStatus
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
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
  suspend fun downloadModel(modelId: String): NanoAIResult<UUID> =
    guardDownloadOperation(
      message = "Failed to start download for model $modelId",
      context = mapOf("modelId" to modelId),
    ) {
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
    }

  /** Pause a download task and persist status. */
  suspend fun pauseDownload(taskId: UUID): NanoAIResult<Unit> =
    guardDownloadOperation(
      message = "Failed to pause download $taskId",
      context = mapOf("taskId" to taskId.toString()),
    ) {
      downloadManager.pauseDownload(taskId)
      downloadManager.updateTaskStatus(taskId, DownloadStatus.PAUSED)
      NanoAIResult.success(Unit)
    }

  /** Resume a paused download. */
  suspend fun resumeDownload(taskId: UUID): NanoAIResult<Unit> =
    guardDownloadOperation(
      message = "Failed to resume download $taskId",
      context = mapOf("taskId" to taskId.toString()),
    ) {
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
    }

  /** Cancel a download and cleanup associated files. */
  suspend fun cancelDownload(taskId: UUID): NanoAIResult<Unit> =
    guardDownloadOperation(
      message = "Failed to cancel download $taskId",
      context = mapOf("taskId" to taskId.toString()),
    ) {
      val modelId = downloadManager.getModelIdForTask(taskId)
      downloadManager.cancelDownload(taskId)
      modelId?.let {
        downloadManager.deletePartialFiles(it)
        modelCatalogRepository.updateInstallState(it, InstallState.NOT_INSTALLED)
        modelCatalogRepository.updateDownloadTaskId(it, null)
      }
      NanoAIResult.success(Unit)
    }

  /** Retry a failed download task. */
  suspend fun retryFailedDownload(taskId: UUID): NanoAIResult<Unit> =
    guardDownloadOperation(
      message = "Failed to retry download $taskId",
      context = mapOf("taskId" to taskId.toString()),
    ) {
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
    }

  /** Observe download progress as a Flow. */
  fun getDownloadProgress(taskId: UUID): Flow<Float> = downloadManager.observeProgress(taskId)

  /** Observe a specific download task for status or error updates. */
  suspend fun observeDownloadTask(taskId: UUID): Flow<DownloadTask?> =
    downloadManager.getTaskById(taskId)

  /** Observe queued download tasks. */
  fun observeDownloadTasks(): Flow<List<DownloadTask>> = downloadManager.observeManagedDownloads()

  private inline fun <T> guardDownloadOperation(
    message: String,
    context: Map<String, String> = emptyMap(),
    block: () -> NanoAIResult<T>,
  ): NanoAIResult<T> {
    return try {
      block()
    } catch (cancellation: CancellationException) {
      throw cancellation
    } catch (sqliteException: SQLiteException) {
      NanoAIResult.recoverable(message = message, cause = sqliteException, context = context)
    } catch (ioException: IOException) {
      NanoAIResult.recoverable(message = message, cause = ioException, context = context)
    } catch (illegalStateException: IllegalStateException) {
      NanoAIResult.recoverable(
        message = message,
        cause = illegalStateException,
        context = context,
      )
    } catch (illegalArgumentException: IllegalArgumentException) {
      NanoAIResult.recoverable(
        message = message,
        cause = illegalArgumentException,
        context = context,
      )
    } catch (securityException: SecurityException) {
      NanoAIResult.recoverable(message = message, cause = securityException, context = context)
    }
  }
}
