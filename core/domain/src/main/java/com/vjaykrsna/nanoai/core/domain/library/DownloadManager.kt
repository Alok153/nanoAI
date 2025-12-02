package com.vjaykrsna.nanoai.core.domain.library

import com.vjaykrsna.nanoai.core.common.annotations.OneShot
import com.vjaykrsna.nanoai.core.common.annotations.ReactiveStream
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
  @OneShot("Start model download if concurrency allows")
  suspend fun startDownload(modelId: String): UUID

  @OneShot("Queue model download when concurrency exceeded")
  suspend fun queueDownload(modelId: String): UUID
}

/** Control operations for task lifecycle management (pause/resume/cancel/retry). */
interface DownloadTaskControl {
  @OneShot("Pause in-flight download task") suspend fun pauseDownload(taskId: UUID)

  @OneShot("Resume paused download task") suspend fun resumeDownload(taskId: UUID)

  @OneShot("Cancel download task") suspend fun cancelDownload(taskId: UUID)

  @OneShot("Retry failed download task") suspend fun retryDownload(taskId: UUID)

  @OneShot("Reset task metadata state") suspend fun resetTask(taskId: UUID)
}

/** Inspect and observe download tasks within the system. */
interface DownloadTaskInspection {
  @OneShot("Fetch download status snapshot")
  suspend fun getDownloadStatus(taskId: UUID): DownloadTask?

  @ReactiveStream("Observe single download task updates")
  suspend fun getTaskById(taskId: UUID): Flow<DownloadTask?>

  @ReactiveStream("Observe active download list")
  suspend fun getActiveDownloads(): Flow<List<DownloadTask>>

  @OneShot("Fetch active download snapshot")
  suspend fun getActiveDownloadsSnapshot(): List<DownloadTask>

  @ReactiveStream("Observe queued downloads") fun getQueuedDownloads(): Flow<List<DownloadTask>>

  @ReactiveStream("Observe managed downloads")
  fun observeManagedDownloads(): Flow<List<DownloadTask>>

  @ReactiveStream("Observe download progress percentage")
  fun observeProgress(taskId: UUID): Flow<Float>
}

/** Access configuration and status mutation capabilities. */
interface DownloadTaskConfiguration {
  @OneShot("Read max concurrent downloads setting") suspend fun getMaxConcurrentDownloads(): Int

  @OneShot("Persist download task status change")
  suspend fun updateTaskStatus(taskId: UUID, status: DownloadStatus)
}

/** Access model download artifacts and cleanup helpers. */
interface DownloadTaskArtifacts {
  @OneShot("Resolve modelId for task") suspend fun getModelIdForTask(taskId: UUID): String?

  @OneShot("Return checksum for completed download")
  suspend fun getDownloadedChecksum(modelId: String): String?

  @OneShot("Delete partial download artifacts") suspend fun deletePartialFiles(modelId: String)
}
