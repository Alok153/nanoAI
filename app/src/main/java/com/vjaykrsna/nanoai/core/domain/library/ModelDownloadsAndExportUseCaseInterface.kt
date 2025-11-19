package com.vjaykrsna.nanoai.core.domain.library

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.annotations.OneShot
import com.vjaykrsna.nanoai.core.common.annotations.ReactiveStream
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/** Interface for model download and export operations. */
interface ModelDownloadsAndExportUseCaseInterface {
  @OneShot("Queue or start a model download")
  suspend fun downloadModel(modelId: String): NanoAIResult<UUID>

  @OneShot("Verify download checksum")
  suspend fun verifyDownloadChecksum(modelId: String): NanoAIResult<Boolean>

  @OneShot("Pause download task") suspend fun pauseDownload(taskId: UUID): NanoAIResult<Unit>

  @OneShot("Resume download task") suspend fun resumeDownload(taskId: UUID): NanoAIResult<Unit>

  @OneShot("Cancel download task") suspend fun cancelDownload(taskId: UUID): NanoAIResult<Unit>

  @OneShot("Delete installed model") suspend fun deleteModel(modelId: String): NanoAIResult<Unit>

  @OneShot("Export backup bundle")
  suspend fun exportBackup(
    destinationPath: String,
    includeChatHistory: Boolean = false,
  ): NanoAIResult<String>

  @ReactiveStream("Observe download progress percentage")
  fun getDownloadProgress(taskId: UUID): Flow<Float>

  @ReactiveStream("Observe single download task state")
  suspend fun observeDownloadTask(taskId: UUID): Flow<DownloadTask?>

  @ReactiveStream("Observe all managed downloads")
  fun observeDownloadTasks(): Flow<List<DownloadTask>>

  @OneShot("Retry failed download task")
  suspend fun retryFailedDownload(taskId: UUID): NanoAIResult<Unit>
}
