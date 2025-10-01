package com.vjaykrsna.nanoai.core.domain.model

import com.vjaykrsna.nanoai.feature.library.data.entities.DownloadTaskEntity
import com.vjaykrsna.nanoai.feature.library.model.DownloadStatus
import kotlinx.datetime.Instant
import java.util.UUID

/**
 * Domain model for a download task.
 *
 * Clean architecture: Separate from database entities.
 * Used by repositories, use cases, ViewModels, and UI.
 */
data class DownloadTask(
    val taskId: UUID,
    val modelId: String,
    val progress: Float = 0f,
    val status: DownloadStatus,
    val bytesDownloaded: Long = 0L,
    val startedAt: Instant? = null,
    val finishedAt: Instant? = null,
    val errorMessage: String? = null,
)

/**
 * Extension function to convert entity to domain model.
 */
fun DownloadTaskEntity.toDomain(): DownloadTask =
    DownloadTask(
        taskId = UUID.fromString(taskId),
        modelId = modelId,
        progress = progress,
        status = status,
        bytesDownloaded = bytesDownloaded,
        startedAt = startedAt,
        finishedAt = finishedAt,
        errorMessage = errorMessage,
    )

/**
 * Extension function to convert domain model to entity.
 */
fun DownloadTask.toEntity(): DownloadTaskEntity =
    DownloadTaskEntity(
        taskId = taskId.toString(),
        modelId = modelId,
        progress = progress,
        status = status,
        bytesDownloaded = bytesDownloaded,
        startedAt = startedAt,
        finishedAt = finishedAt,
        errorMessage = errorMessage,
    )
