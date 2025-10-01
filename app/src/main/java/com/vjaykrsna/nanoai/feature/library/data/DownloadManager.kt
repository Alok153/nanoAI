package com.vjaykrsna.nanoai.feature.library.data

import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.feature.library.model.DownloadStatus
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Manager for model download operations.
 *
 * Integrates WorkManager for background downloads with database tracking. Enforces max 2 concurrent
 * downloads.
 */
interface DownloadManager {
    /** Start downloading a model. Queues if max concurrent downloads reached. */
    suspend fun startDownload(modelId: String): UUID

    /** Queue a download without starting immediately. */
    suspend fun queueDownload(modelId: String): UUID

    /** Pause an active download. */
    suspend fun pauseDownload(taskId: UUID)

    /** Resume a previously paused download. */
    suspend fun resumeDownload(taskId: UUID)

    /** Cancel a download and clean up. */
    suspend fun cancelDownload(taskId: UUID)

    /** Retry a failed download. */
    suspend fun retryDownload(taskId: UUID)

    /** Reset internal task state before retry. */
    suspend fun resetTask(taskId: UUID)

    /** Get current status of a download task. */
    suspend fun getDownloadStatus(taskId: UUID): DownloadTask?

    /** Observe a specific download task. */
    suspend fun getTaskById(taskId: UUID): Flow<DownloadTask?>

    /** Get all active downloads. */
    suspend fun getActiveDownloads(): Flow<List<DownloadTask>>

    /** Get queued downloads. */
    fun getQueuedDownloads(): Flow<List<DownloadTask>>

    /** Observe download progress for a task. */
    fun observeProgress(taskId: UUID): Flow<Float>

    /** Get max concurrent downloads configured by user. */
    suspend fun getMaxConcurrentDownloads(): Int

    /** Update the status of a download task. */
    suspend fun updateTaskStatus(
        taskId: UUID,
        status: DownloadStatus,
    )

    /** Map task to model ID. */
    suspend fun getModelIdForTask(taskId: UUID): String?

    /** Compute checksum of downloaded model artifacts. */
    suspend fun getDownloadedChecksum(modelId: String): String?

    /** Delete partially downloaded files for the model. */
    suspend fun deletePartialFiles(modelId: String)

    companion object {
        const val DEFAULT_MAX_CONCURRENT_DOWNLOADS = 2
    }
}
