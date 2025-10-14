package com.vjaykrsna.nanoai.feature.library.domain

import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/** Interface for model download and export operations. */
interface ModelDownloadsAndExportUseCaseInterface {
  suspend fun downloadModel(modelId: String): Result<UUID>

  suspend fun verifyDownloadChecksum(modelId: String): Result<Boolean>

  suspend fun pauseDownload(taskId: UUID)

  suspend fun resumeDownload(taskId: UUID)

  suspend fun cancelDownload(taskId: UUID)

  suspend fun deleteModel(modelId: String): Result<Unit>

  suspend fun exportBackup(
    destinationPath: String,
    includeChatHistory: Boolean = false
  ): Result<String>

  fun getDownloadProgress(taskId: UUID): Flow<Float>

  suspend fun observeDownloadTask(taskId: UUID): Flow<DownloadTask?>

  fun getQueuedDownloads(): Flow<List<DownloadTask>>

  suspend fun retryFailedDownload(taskId: UUID)
}
