package com.vjaykrsna.nanoai.feature.library.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vjaykrsna.nanoai.feature.library.model.DownloadStatus
import kotlinx.datetime.Instant

/**
 * Room entity representing a model download task.
 *
 * Tracks download progress, status, and integrates with WorkManager for background download
 * orchestration.
 *
 * @property taskId Unique identifier (UUID string)
 * @property modelId Associated model being downloaded
 * @property progress Download progress (0.0 to 1.0)
 * @property status Current download status
 * @property bytesDownloaded Number of bytes downloaded so far
 * @property startedAt Timestamp when download started (nullable if queued)
 * @property finishedAt Timestamp when download completed/failed (nullable if in progress)
 * @property errorMessage Error message if status is FAILED
 */
@Entity(
  tableName = "download_tasks",
  foreignKeys =
    [
      ForeignKey(
        entity = ModelPackageEntity::class,
        parentColumns = ["model_id"],
        childColumns = ["model_id"],
        onDelete = ForeignKey.CASCADE,
      ),
    ],
  indices =
    [
      Index(value = ["model_id"]),
      Index(value = ["status"]),
    ],
)
data class DownloadTaskEntity(
  @PrimaryKey @ColumnInfo(name = "task_id") val taskId: String,
  @ColumnInfo(name = "model_id") val modelId: String,
  @ColumnInfo(name = "progress") val progress: Float = 0f,
  @ColumnInfo(name = "status") val status: DownloadStatus,
  @ColumnInfo(name = "bytes_downloaded") val bytesDownloaded: Long = 0L,
  @ColumnInfo(name = "started_at") val startedAt: Instant? = null,
  @ColumnInfo(name = "finished_at") val finishedAt: Instant? = null,
  @ColumnInfo(name = "error_message") val errorMessage: String? = null,
)
