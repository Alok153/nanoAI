package com.vjaykrsna.nanoai.feature.uiux.domain

import androidx.work.WorkManager
import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.feature.library.data.DownloadManager
import com.vjaykrsna.nanoai.feature.library.model.DownloadStatus
import com.vjaykrsna.nanoai.feature.uiux.state.JobStatus
import com.vjaykrsna.nanoai.feature.uiux.state.JobType
import com.vjaykrsna.nanoai.feature.uiux.state.ProgressJob
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

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
  private val workManager: WorkManager,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

  /** Flow of all active progress jobs, combining downloads and other WorkManager tasks. */
  val progressJobs: Flow<List<ProgressJob>> = observeOtherJobs()

  /**
   * Observes other WorkManager jobs (future expansion for inference, generation, etc.). Currently
   * returns an empty flow as placeholder that will be replaced with actual download and inference
   * job observations.
   */
  private fun observeOtherJobs(): Flow<List<ProgressJob>> = flowOf(emptyList())

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

/** Extension to check if a download task is active (not completed or cancelled). */
private fun DownloadTask.isActive(): Boolean =
  status != DownloadStatus.COMPLETED &&
    status != DownloadStatus.CANCELLED &&
    status != DownloadStatus.FAILED

/** Maps a DownloadTask to a ProgressJob for UI display. */
private fun DownloadTask.toProgressJob(): ProgressJob {
  val jobStatus =
    when (status) {
      DownloadStatus.QUEUED -> JobStatus.PENDING
      DownloadStatus.DOWNLOADING -> JobStatus.RUNNING
      DownloadStatus.PAUSED -> JobStatus.PAUSED
      DownloadStatus.FAILED -> JobStatus.FAILED
      DownloadStatus.COMPLETED -> JobStatus.COMPLETED
      DownloadStatus.CANCELLED -> JobStatus.FAILED
    }

  val canRetry = status == DownloadStatus.FAILED
  val progressValue = progress.coerceIn(0f, 1f)

  // Estimate ETA based on progress and elapsed time
  val eta =
    if (status == DownloadStatus.DOWNLOADING && progress > 0f && startedAt != null) {
      val elapsed =
        java.time.Duration.between(
          startedAt!!.toJavaInstant(),
          kotlinx.datetime.Clock.System.now().toJavaInstant()
        )
      val estimatedTotal = elapsed.toMillis() / progress
      val remaining = estimatedTotal - elapsed.toMillis()
      Duration.ofMillis(remaining.toLong())
    } else {
      null
    }

  val queuedAtJava = startedAt?.toJavaInstant() ?: Instant.now()

  return ProgressJob(
    jobId = taskId,
    type = JobType.MODEL_DOWNLOAD,
    status = jobStatus,
    progress = progressValue,
    eta = eta,
    canRetry = canRetry,
    queuedAt = queuedAtJava,
  )
}

/** Extension to convert kotlinx.datetime.Instant to java.time.Instant. */
private fun kotlinx.datetime.Instant.toJavaInstant(): Instant =
  Instant.ofEpochSecond(this.epochSeconds, this.nanosecondsOfSecond.toLong())
