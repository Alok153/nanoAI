package com.vjaykrsna.nanoai.core.domain.library

import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.library.DownloadStatus
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for coordinating model downloads.
 *
 * The contract is split into focused capabilities to avoid large, catch-all interfaces while still
 * exposing a single aggregation type (`DownloadManager`) to consumers.
 */
interface DownloadManager :
  DownloadTaskScheduling,
  DownloadTaskControl,
  DownloadTaskInspection,
  DownloadTaskConfiguration,
  DownloadTaskArtifacts {

  companion object {
    const val DEFAULT_MAX_CONCURRENT_DOWNLOADS = 2
  }
}

/** Start or queue downloads within concurrency limits. */
interface DownloadTaskScheduling {
  suspend fun startDownload(modelId: String): UUID

  suspend fun queueDownload(modelId: String): UUID
}

/** Control operations for task lifecycle management (pause/resume/cancel/retry). */
interface DownloadTaskControl {
  suspend fun pauseDownload(taskId: UUID)

  suspend fun resumeDownload(taskId: UUID)

  suspend fun cancelDownload(taskId: UUID)

  suspend fun retryDownload(taskId: UUID)

  suspend fun resetTask(taskId: UUID)
}

/** Inspect and observe download tasks within the system. */
interface DownloadTaskInspection {
  suspend fun getDownloadStatus(taskId: UUID): DownloadTask?

  suspend fun getTaskById(taskId: UUID): Flow<DownloadTask?>

  suspend fun getActiveDownloads(): Flow<List<DownloadTask>>

  fun getQueuedDownloads(): Flow<List<DownloadTask>>

  fun observeManagedDownloads(): Flow<List<DownloadTask>>

  fun observeProgress(taskId: UUID): Flow<Float>
}

/** Access configuration and status mutation capabilities. */
interface DownloadTaskConfiguration {
  suspend fun getMaxConcurrentDownloads(): Int

  suspend fun updateTaskStatus(taskId: UUID, status: DownloadStatus)
}

/** Access model download artifacts and cleanup helpers. */
interface DownloadTaskArtifacts {
  suspend fun getModelIdForTask(taskId: UUID): String?

  suspend fun getDownloadedChecksum(modelId: String): String?

  suspend fun deletePartialFiles(modelId: String)
}
