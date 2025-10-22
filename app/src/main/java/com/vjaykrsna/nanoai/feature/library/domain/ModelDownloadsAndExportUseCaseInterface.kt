package com.vjaykrsna.nanoai.feature.library.domain

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/** Interface for model download and export operations. */
interface ModelDownloadsAndExportUseCaseInterface {
  suspend fun downloadModel(modelId: String): NanoAIResult<UUID>

  suspend fun verifyDownloadChecksum(modelId: String): NanoAIResult<Boolean>

  suspend fun pauseDownload(taskId: UUID): NanoAIResult<Unit>

  suspend fun resumeDownload(taskId: UUID): NanoAIResult<Unit>

  suspend fun cancelDownload(taskId: UUID): NanoAIResult<Unit>

  suspend fun deleteModel(modelId: String): NanoAIResult<Unit>

  suspend fun exportBackup(
    destinationPath: String,
    includeChatHistory: Boolean = false,
  ): NanoAIResult<String>

  fun getDownloadProgress(taskId: UUID): Flow<Float>

  suspend fun observeDownloadTask(taskId: UUID): Flow<DownloadTask?>

  fun observeDownloadTasks(): Flow<List<DownloadTask>>

  suspend fun retryFailedDownload(taskId: UUID): NanoAIResult<Unit>
}
