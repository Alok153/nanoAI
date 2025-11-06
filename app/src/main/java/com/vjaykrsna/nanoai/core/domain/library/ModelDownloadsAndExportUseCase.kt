@file:Suppress("ReturnCount") // Multiple validation paths in use case

package com.vjaykrsna.nanoai.core.domain.library

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.library.DownloadStatus
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Orchestrates model download lifecycle management and export bundle creation. */
@Singleton
class ModelDownloadsAndExportUseCase
@Inject
constructor(
  private val modelCatalogRepository: ModelCatalogRepository,
  private val downloadManager: DownloadManager,
  private val exportService: ExportService,
) : ModelDownloadsAndExportUseCaseInterface {
  /** Queue or start a download based on concurrency limits. */
  override suspend fun downloadModel(modelId: String): NanoAIResult<UUID> {
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

  /** Validate downloaded checksum and update install state accordingly. */
  override suspend fun verifyDownloadChecksum(modelId: String): NanoAIResult<Boolean> {
    return try {
      val model: ModelPackage =
        modelCatalogRepository.getModelById(modelId).first()
          ?: return NanoAIResult.recoverable(
            message = "Model $modelId not found",
            context = mapOf("modelId" to modelId),
          )

      val expectedChecksum =
        model.checksumSha256
          ?: run {
            modelCatalogRepository.updateInstallState(modelId, InstallState.ERROR)
            return NanoAIResult.recoverable(
              message = "No checksum available for model $modelId",
              context = mapOf("modelId" to modelId),
            )
          }

      val actualChecksum =
        downloadManager.getDownloadedChecksum(modelId)
          ?: run {
            modelCatalogRepository.updateInstallState(modelId, InstallState.ERROR)
            return NanoAIResult.recoverable(
              message = "Downloaded checksum not available for model $modelId",
              context = mapOf("modelId" to modelId),
            )
          }

      val matches = expectedChecksum.equals(actualChecksum, ignoreCase = true)
      if (matches) {
        modelCatalogRepository.updateInstallState(modelId, InstallState.INSTALLED)
        modelCatalogRepository.updateChecksum(modelId, actualChecksum)
        model.downloadTaskId?.let { downloadManager.updateTaskStatus(it, DownloadStatus.COMPLETED) }
      } else {
        modelCatalogRepository.updateInstallState(modelId, InstallState.ERROR)
      }
      NanoAIResult.success(matches)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to verify checksum for model $modelId",
        cause = e,
        context = mapOf("modelId" to modelId),
      )
    }
  }

  /** Pause a download task and persist status. */
  override suspend fun pauseDownload(taskId: UUID): NanoAIResult<Unit> {
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
  override suspend fun resumeDownload(taskId: UUID): NanoAIResult<Unit> {
    return try {
      val task =
        downloadManager.getTaskById(taskId).first()
          ?: return NanoAIResult.recoverable(
            message = "Download task $taskId not found",
            context = mapOf("taskId" to taskId.toString()),
          )
      if (task.status != DownloadStatus.PAUSED)
        return NanoAIResult.recoverable(
          message = "Download task $taskId is not paused",
          context = mapOf("taskId" to taskId.toString(), "status" to task.status.toString()),
        )

      downloadManager.resumeDownload(taskId)
      downloadManager.updateTaskStatus(taskId, DownloadStatus.DOWNLOADING)
      NanoAIResult.success(Unit)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to resume download $taskId",
        cause = e,
        context = mapOf("taskId" to taskId.toString()),
      )
    }
  }

  /** Cancel a download and cleanup associated files. */
  override suspend fun cancelDownload(taskId: UUID): NanoAIResult<Unit> {
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

  /** Delete a model if not active in any chat session. */
  override suspend fun deleteModel(modelId: String): NanoAIResult<Unit> {
    return try {
      val inUse = modelCatalogRepository.isModelActiveInSession(modelId)
      if (inUse) {
        return NanoAIResult.recoverable(
          message = "Model $modelId is active in a conversation",
          context = mapOf("modelId" to modelId),
        )
      }

      modelCatalogRepository.deleteModelFiles(modelId)
      modelCatalogRepository.updateInstallState(modelId, InstallState.NOT_INSTALLED)
      modelCatalogRepository.updateDownloadTaskId(modelId, null)
      NanoAIResult.success(Unit)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to delete model $modelId",
        cause = e,
        context = mapOf("modelId" to modelId),
      )
    }
  }

  /** Export personas, provider configs, and optional chat history as bundle. */
  override suspend fun exportBackup(
    destinationPath: String,
    includeChatHistory: Boolean,
  ): NanoAIResult<String> {
    return try {
      val personas = exportService.gatherPersonas()
      val providers = exportService.gatherAPIProviderConfigs()
      val chatHistory = if (includeChatHistory) exportService.gatherChatHistory() else emptyList()

      val bundlePath =
        exportService.createExportBundle(personas, providers, destinationPath, chatHistory)
      exportService.notifyUnencryptedExport(bundlePath)
      NanoAIResult.success(bundlePath)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to export backup to $destinationPath",
        cause = e,
        context =
          mapOf(
            "destinationPath" to destinationPath,
            "includeChatHistory" to includeChatHistory.toString(),
          ),
      )
    }
  }

  /** Observe download progress as a Flow. */
  override fun getDownloadProgress(taskId: UUID): Flow<Float> =
    downloadManager.observeProgress(taskId)

  /** Observe a specific download task for status or error updates. */
  override suspend fun observeDownloadTask(taskId: UUID): Flow<DownloadTask?> =
    downloadManager.getTaskById(taskId)

  /** Observe queued download tasks. */
  override fun observeDownloadTasks(): Flow<List<DownloadTask>> =
    downloadManager.observeManagedDownloads()

  /** Retry a failed download task. */
  override suspend fun retryFailedDownload(taskId: UUID): NanoAIResult<Unit> {
    return try {
      val task =
        downloadManager.getTaskById(taskId).first()
          ?: return NanoAIResult.recoverable(
            message = "Download task $taskId not found",
            context = mapOf("taskId" to taskId.toString()),
          )
      if (task.status != DownloadStatus.FAILED)
        return NanoAIResult.recoverable(
          message = "Download task $taskId is not failed",
          context = mapOf("taskId" to taskId.toString(), "status" to task.status.toString()),
        )
      val modelId =
        downloadManager.getModelIdForTask(taskId)
          ?: return NanoAIResult.recoverable(
            message = "Model ID not found for task $taskId",
            context = mapOf("taskId" to taskId.toString()),
          )

      downloadManager.resetTask(taskId)
      downloadManager.startDownload(modelId)
      modelCatalogRepository.updateInstallState(modelId, InstallState.DOWNLOADING)
      modelCatalogRepository.updateDownloadTaskId(modelId, taskId)
      NanoAIResult.success(Unit)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to retry download $taskId",
        cause = e,
        context = mapOf("taskId" to taskId.toString()),
      )
    }
  }
}
