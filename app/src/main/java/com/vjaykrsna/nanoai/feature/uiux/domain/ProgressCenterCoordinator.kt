package com.vjaykrsna.nanoai.feature.uiux.domain

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.feature.library.data.DownloadManager
import com.vjaykrsna.nanoai.feature.library.domain.DownloadStatus
import com.vjaykrsna.nanoai.feature.uiux.presentation.JobStatus
import com.vjaykrsna.nanoai.feature.uiux.presentation.JobType
import com.vjaykrsna.nanoai.feature.uiux.presentation.ProgressJob
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import kotlinx.datetime.toJavaInstant

/**
 * Coordinates WorkManager job progress with the Progress Center UI state.
 *
 * Observes active downloads and other background jobs, transforming them into [ProgressJob]
 * instances for display in the unified progress center panel.
 */
@Singleton
class ProgressCenterCoordinator
@Inject
constructor(
  private val downloadManager: DownloadManager,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  /** Flow of all active progress jobs, combining downloads and other WorkManager tasks. */
  val progressJobs: Flow<List<ProgressJob>> =
    combine(observeDownloadJobs(), observeOtherJobs()) { downloads, otherJobs ->
        (downloads + otherJobs).sortedBy { it.jobId.toString() }
      }
      .distinctUntilChanged()

  /**
   * Observes download jobs exposed by [DownloadManager] and maps them into progress center
   * compatible records.
   */
  private fun observeDownloadJobs(): Flow<List<ProgressJob>> =
    combine(
        flow { emitAll(downloadManager.getActiveDownloads()) }.onStart { emit(emptyList()) },
        downloadManager.getQueuedDownloads().onStart { emit(emptyList()) },
      ) { active, queued ->
        mergeDownloadTasks(active, queued)
      }
      .catch { emit(emptyList()) }
      .flowOn(ioDispatcher)

  /**
   * Observes other WorkManager jobs (future expansion for inference, generation, etc.). Currently
   * returns an empty flow as placeholder that will be replaced with actual download and inference
   * job observations.
   */
  private fun observeOtherJobs(): Flow<List<ProgressJob>> = flowOf(emptyList())

  private fun mergeDownloadTasks(
    active: List<DownloadTask>,
    queued: List<DownloadTask>,
  ): List<ProgressJob> {
    if (active.isEmpty() && queued.isEmpty()) return emptyList()

    val combined =
      buildMap<UUID, DownloadTask> {
        active.forEach { put(it.taskId, it) }
        queued.forEach { putIfAbsent(it.taskId, it) }
      }

    return combined.values.map { it.toProgressJob() }.sortedBy { it.jobId.toString() }
  }

  private fun DownloadTask.toProgressJob(): ProgressJob =
    ProgressJob(
      jobId = taskId,
      type = JobType.MODEL_DOWNLOAD,
      status = status.toJobStatus(),
      progress = progress,
      canRetry = status == DownloadStatus.FAILED,
      queuedAt = startedAt?.toJavaInstant() ?: Instant.EPOCH,
    )

  private fun DownloadStatus.toJobStatus(): JobStatus =
    when (this) {
      DownloadStatus.QUEUED -> JobStatus.PENDING
      DownloadStatus.DOWNLOADING -> JobStatus.RUNNING
      DownloadStatus.PAUSED -> JobStatus.PAUSED
      DownloadStatus.FAILED -> JobStatus.FAILED
      DownloadStatus.COMPLETED -> JobStatus.COMPLETED
      DownloadStatus.CANCELLED -> JobStatus.FAILED
    }

  /** Retries a failed job by its ID. */
  suspend fun retryJob(jobId: UUID) {
    withContext(ioDispatcher) {
      // Check if it's a download job
      val task = downloadManager.getDownloadStatus(jobId)
      if (task != null && task.status == DownloadStatus.FAILED) {
        downloadManager.retryDownload(jobId)
      }
      // Future: Handle other job types (inference, generation)
    }
  }

  /** Cancels an active job by its ID. */
  suspend fun cancelJob(jobId: UUID) {
    withContext(ioDispatcher) {
      val task = downloadManager.getDownloadStatus(jobId)
      if (task != null) {
        downloadManager.cancelDownload(jobId)
      }
      // Future: Handle other job types
    }
  }

  /** Pauses a running job by its ID. */
  suspend fun pauseJob(jobId: UUID) {
    withContext(ioDispatcher) {
      val task = downloadManager.getDownloadStatus(jobId)
      if (task != null && task.status == DownloadStatus.DOWNLOADING) {
        downloadManager.pauseDownload(jobId)
      }
      // Future: Handle other job types
    }
  }

  /** Resumes a paused job by its ID. */
  suspend fun resumeJob(jobId: UUID) {
    withContext(ioDispatcher) {
      val task = downloadManager.getDownloadStatus(jobId)
      if (task != null && task.status == DownloadStatus.PAUSED) {
        downloadManager.resumeDownload(jobId)
      }
      // Future: Handle other job types
    }
  }
}
