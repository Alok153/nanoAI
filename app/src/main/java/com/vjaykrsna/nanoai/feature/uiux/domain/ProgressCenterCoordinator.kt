package com.vjaykrsna.nanoai.feature.uiux.domain

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.feature.library.data.DownloadManager
import com.vjaykrsna.nanoai.feature.library.model.DownloadStatus
import com.vjaykrsna.nanoai.feature.uiux.state.ProgressJob
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
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
