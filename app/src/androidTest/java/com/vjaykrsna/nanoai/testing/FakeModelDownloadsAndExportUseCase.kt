package com.vjaykrsna.nanoai.testing

import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.feature.library.domain.ModelDownloadsAndExportUseCaseInterface
import com.vjaykrsna.nanoai.feature.library.model.DownloadStatus
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Fake implementation of ModelDownloadsAndExportUseCase for instrumentation testing. Provides
 * controllable behavior for download and export operations.
 */
class FakeModelDownloadsAndExportUseCase : ModelDownloadsAndExportUseCaseInterface {
  private val _queuedDownloads = MutableStateFlow<List<DownloadTask>>(emptyList())
  private val _downloadProgress = mutableMapOf<UUID, MutableStateFlow<Float>>()
  private val _downloadTasks = mutableMapOf<UUID, MutableStateFlow<DownloadTask?>>()

  var shouldFailOnDownload = false
  var shouldFailOnDelete = false
  var shouldFailOnExport = false

  var lastDownloadedModelId: String? = null
  var lastDeletedModelId: String? = null
  var lastExportPath: String? = null

  fun addDownloadTask(task: DownloadTask) {
    _queuedDownloads.value += task
    _downloadTasks[task.taskId] = MutableStateFlow(task)
    _downloadProgress[task.taskId] = MutableStateFlow(0f)
  }

  fun updateDownloadProgress(taskId: UUID, progress: Float) {
    _downloadProgress[taskId]?.value = progress
  }

  fun updateDownloadStatus(taskId: UUID, status: DownloadStatus) {
    val task = _downloadTasks[taskId]?.value ?: return
    _downloadTasks[taskId]?.value = task.copy(status = status)
  }

  fun clearAll() {
    _queuedDownloads.value = emptyList()
    _downloadProgress.clear()
    _downloadTasks.clear()
    shouldFailOnDownload = false
    shouldFailOnDelete = false
    shouldFailOnExport = false
    lastDownloadedModelId = null
    lastDeletedModelId = null
    lastExportPath = null
  }

  override suspend fun downloadModel(modelId: String): Result<UUID> {
    lastDownloadedModelId = modelId
    return if (shouldFailOnDownload) {
      Result.failure(Exception("Download failed"))
    } else {
      val taskId = UUID.randomUUID()
      val task =
        DownloadTask(
          taskId = taskId,
          modelId = modelId,
          status = DownloadStatus.QUEUED,
          progress = 0f,
          errorMessage = null,
        )
      addDownloadTask(task)
      Result.success(taskId)
    }
  }

  override suspend fun deleteModel(modelId: String): Result<Unit> {
    lastDeletedModelId = modelId
    return if (shouldFailOnDelete) {
      Result.failure(Exception("Delete failed"))
    } else {
      Result.success(Unit)
    }
  }

  override fun getDownloadProgress(taskId: UUID): Flow<Float> =
    _downloadProgress[taskId] ?: flowOf(0f)

  override suspend fun observeDownloadTask(taskId: UUID): Flow<DownloadTask?> =
    _downloadTasks[taskId] ?: flowOf(null)

  override fun observeDownloadTasks(): Flow<List<DownloadTask>> = _queuedDownloads

  override suspend fun retryFailedDownload(taskId: UUID) {
    updateDownloadStatus(taskId, DownloadStatus.QUEUED)
  }

  override suspend fun exportBackup(
    destinationPath: String,
    includeChatHistory: Boolean
  ): Result<String> {
    lastExportPath = destinationPath
    return if (shouldFailOnExport) {
      Result.failure(Exception("Export failed"))
    } else {
      Result.success(destinationPath)
    }
  }

  override suspend fun verifyDownloadChecksum(modelId: String): Result<Boolean> {
    return Result.success(true)
  }

  override suspend fun pauseDownload(taskId: UUID) {
    updateDownloadStatus(taskId, DownloadStatus.PAUSED)
  }

  override suspend fun resumeDownload(taskId: UUID) {
    updateDownloadStatus(taskId, DownloadStatus.DOWNLOADING)
  }

  override suspend fun cancelDownload(taskId: UUID) {
    val task = _downloadTasks[taskId]?.value ?: return
    _downloadTasks[taskId]?.value = task.copy(status = DownloadStatus.CANCELLED)
    _queuedDownloads.value = _queuedDownloads.value.filter { it.taskId != taskId }
  }
}
