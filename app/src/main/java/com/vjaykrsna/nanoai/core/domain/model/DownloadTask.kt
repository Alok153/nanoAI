package com.vjaykrsna.nanoai.core.domain.model

import com.vjaykrsna.nanoai.core.domain.model.library.DownloadStatus
import java.util.UUID
import kotlinx.datetime.Instant

/**
 * Domain model for a download task.
 *
 * Clean architecture: Separate from database entities. Used by repositories, use cases, ViewModels,
 * and UI. Mapping to/from entities is handled by
 * [com.vjaykrsna.nanoai.core.data.library.mappers.DownloadTaskMapper].
 */
data class DownloadTask(
  val taskId: UUID,
  val modelId: String,
  val progress: Float = 0f,
  val status: DownloadStatus,
  val bytesDownloaded: Long = 0L,
  val totalBytes: Long = 0L,
  val startedAt: Instant? = null,
  val finishedAt: Instant? = null,
  val errorMessage: String? = null,
)
