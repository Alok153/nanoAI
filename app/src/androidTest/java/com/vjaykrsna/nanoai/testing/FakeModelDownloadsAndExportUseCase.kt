package com.vjaykrsna.nanoai.testing

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.feature.library.domain.ModelDownloadsAndExportUseCaseInterface
import com.vjaykrsna.nanoai.feature.library.domain.DownloadStatus
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

  override suspend fun downloadModel(modelId: String): NanoAIResult<UUID> {
    lastDownloadedModelId = modelId
    return if (shouldFailOnDownload) {
      NanoAIResult.recoverable(message = "Download failed")
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
      NanoAIResult.success(taskId)
    }
  }

  override suspend fun verifyDownloadChecksum(modelId: String): NanoAIResult<Boolean> {
    return NanoAIResult.success(true)
  }

  override suspend fun pauseDownload(taskId: UUID): NanoAIResult<Unit> {
    updateDownloadStatus(taskId, DownloadStatus.PAUSED)
    return NanoAIResult.success(Unit)
  }

  override suspend fun resumeDownload(taskId: UUID): NanoAIResult<Unit> {
    updateDownloadStatus(taskId, DownloadStatus.DOWNLOADING)
    return NanoAIResult.success(Unit)
  }

  override suspend fun cancelDownload(taskId: UUID): NanoAIResult<Unit> {
    updateDownloadStatus(taskId, DownloadStatus.CANCELLED)
    return NanoAIResult.success(Unit)
  }

  override suspend fun deleteModel(modelId: String): NanoAIResult<Unit> {
    lastDeletedModelId = modelId
    return if (shouldFailOnDelete) {
      NanoAIResult.recoverable(message = "Delete failed")
    } else {
      NanoAIResult.success(Unit)
    }
  }

  override fun getDownloadProgress(taskId: UUID): Flow<Float> =
    _downloadProgress[taskId] ?: flowOf(0f)

  override suspend fun observeDownloadTask(taskId: UUID): Flow<DownloadTask?> =
    _downloadTasks[taskId] ?: flowOf(null)

  override fun observeDownloadTasks(): Flow<List<DownloadTask>> = _queuedDownloads

  override suspend fun retryFailedDownload(taskId: UUID): NanoAIResult<Unit> {
    updateDownloadStatus(taskId, DownloadStatus.QUEUED)
    return NanoAIResult.success(Unit)
  }

  override suspend fun exportBackup(
    destinationPath: String,
    includeChatHistory: Boolean,
  ): NanoAIResult<String> {
    lastExportPath = destinationPath
    return if (shouldFailOnExport) {
      NanoAIResult.recoverable(message = "Export failed")
    } else {
      NanoAIResult.success(destinationPath)
    }
  }
}
