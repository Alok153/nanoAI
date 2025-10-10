@file:Suppress("ReturnCount") // Multiple validation paths in use case

package com.vjaykrsna.nanoai.feature.library.domain

import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.data.DownloadManager
import com.vjaykrsna.nanoai.feature.library.data.ModelCatalogRepository
import com.vjaykrsna.nanoai.feature.library.model.DownloadStatus
import com.vjaykrsna.nanoai.feature.library.model.InstallState
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
) {
  /** Queue or start a download based on concurrency limits. */
  suspend fun downloadModel(modelId: String): Result<UUID> = runCatching {
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
    taskId
  }

  /** Validate downloaded checksum and update install state accordingly. */
  suspend fun verifyDownloadChecksum(modelId: String): Result<Boolean> = runCatching {
    val model: ModelPackage =
      modelCatalogRepository.getModelById(modelId).first() ?: error("Model $modelId not found")
    val expectedChecksum =
      model.checksumSha256
        ?: run {
          modelCatalogRepository.updateInstallState(modelId, InstallState.ERROR)
          return@runCatching false
        }
    val actualChecksum =
      downloadManager.getDownloadedChecksum(modelId)
        ?: run {
          modelCatalogRepository.updateInstallState(modelId, InstallState.ERROR)
          return@runCatching false
        }

    val matches = expectedChecksum.equals(actualChecksum, ignoreCase = true)
    if (matches) {
      modelCatalogRepository.updateInstallState(modelId, InstallState.INSTALLED)
      modelCatalogRepository.updateChecksum(modelId, actualChecksum)
      model.downloadTaskId?.let { downloadManager.updateTaskStatus(it, DownloadStatus.COMPLETED) }
    } else {
      modelCatalogRepository.updateInstallState(modelId, InstallState.ERROR)
    }
    matches
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

  /** Delete a model if not active in any chat session. */
  suspend fun deleteModel(modelId: String): Result<Unit> = runCatching {
    val inUse = modelCatalogRepository.isModelActiveInSession(modelId)
    if (inUse) throw ModelInUseException(modelId)

    modelCatalogRepository.deleteModelFiles(modelId)
    modelCatalogRepository.updateInstallState(modelId, InstallState.NOT_INSTALLED)
    modelCatalogRepository.updateDownloadTaskId(modelId, null)
  }

  /** Export personas, provider configs, and optional chat history as bundle. */
  suspend fun exportBackup(
    destinationPath: String,
    includeChatHistory: Boolean = false
  ): Result<String> = runCatching {
    val personas = exportService.gatherPersonas()
    val providers = exportService.gatherAPIProviderConfigs()
    val chatHistory = if (includeChatHistory) exportService.gatherChatHistory() else emptyList()

    val bundlePath =
      exportService.createExportBundle(personas, providers, destinationPath, chatHistory)
    exportService.notifyUnencryptedExport(bundlePath)
    bundlePath
  }

  /** Observe download progress as a Flow. */
  fun getDownloadProgress(taskId: UUID): Flow<Float> = downloadManager.observeProgress(taskId)

  /** Observe a specific download task for status or error updates. */
  suspend fun observeDownloadTask(taskId: UUID): Flow<DownloadTask?> =
    downloadManager.getTaskById(taskId)

  /** Observe queued download tasks. */
  fun getQueuedDownloads(): Flow<List<DownloadTask>> = downloadManager.getQueuedDownloads()

  /** Retry a failed download task. */
  suspend fun retryFailedDownload(taskId: UUID) {
    val task = downloadManager.getTaskById(taskId).first() ?: return
    if (task.status != DownloadStatus.FAILED) return
    val modelId = downloadManager.getModelIdForTask(taskId) ?: return

    downloadManager.resetTask(taskId)
    downloadManager.startDownload(modelId)
    modelCatalogRepository.updateInstallState(modelId, InstallState.DOWNLOADING)
    modelCatalogRepository.updateDownloadTaskId(modelId, taskId)
  }
}

class ModelInUseException(
  modelId: String,
) : IllegalStateException("Model $modelId is active in a conversation")
