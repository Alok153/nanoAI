package com.vjaykrsna.nanoai.feature.library.data.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vjaykrsna.nanoai.feature.library.data.entities.DownloadTaskEntity
import com.vjaykrsna.nanoai.feature.library.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for DownloadTask entities.
 *
 * Provides methods to manage model download tasks and progress tracking.
 */
@Dao
interface DownloadTaskDao {
  /** Insert or update a download task. */
  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(task: DownloadTaskEntity)

  /** Insert multiple download tasks. */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(tasks: List<DownloadTaskEntity>)

  /** Update an existing download task. */
  @Update suspend fun update(task: DownloadTaskEntity)

  /** Delete a download task. */
  @Delete suspend fun delete(task: DownloadTaskEntity)

  /** Get a specific task by ID. */
  @Query("SELECT * FROM download_tasks WHERE task_id = :taskId")
  suspend fun getById(taskId: String): DownloadTaskEntity?

  /** Observe a specific task by ID (reactive). */
  @Query("SELECT * FROM download_tasks WHERE task_id = :taskId")
  fun observeById(taskId: String): Flow<DownloadTaskEntity?>

  /** Get task by model ID (one model can only have one active download). */
  @Query("SELECT * FROM download_tasks WHERE model_id = :modelId LIMIT 1")
  suspend fun getByModelId(modelId: String): DownloadTaskEntity?

  /** Get model ID for a given task. */
  @Query("SELECT model_id FROM download_tasks WHERE task_id = :taskId")
  suspend fun getModelIdForTask(taskId: String): String?

  /** Get all tasks with a specific status. */
  @Query("SELECT * FROM download_tasks WHERE status = :status ORDER BY started_at ASC")
  suspend fun getByStatus(status: DownloadStatus): List<DownloadTaskEntity>

  /** Get all queued downloads (for queue management). */
  @Query("SELECT * FROM download_tasks WHERE status = 'QUEUED' ORDER BY started_at ASC")
  suspend fun getQueuedDownloads(): List<DownloadTaskEntity>

  /** Observe queued downloads (reactive). */
  @Query("SELECT * FROM download_tasks WHERE status = 'QUEUED' ORDER BY started_at ASC")
  fun observeQueuedDownloads(): Flow<List<DownloadTaskEntity>>

  /** Get all active downloads (DOWNLOADING status). */
  @Query("SELECT * FROM download_tasks WHERE status = 'DOWNLOADING' ORDER BY started_at ASC")
  suspend fun getActiveDownloads(): List<DownloadTaskEntity>

  /** Observe active downloads (reactive). */
  @Query("SELECT * FROM download_tasks WHERE status = 'DOWNLOADING' ORDER BY started_at ASC")
  fun observeActiveDownloads(): Flow<List<DownloadTaskEntity>>

  /** Get failed downloads for retry. */
  @Query("SELECT * FROM download_tasks WHERE status = 'FAILED' ORDER BY finished_at DESC")
  suspend fun getFailedDownloads(): List<DownloadTaskEntity>

  /** Get all tasks. */
  @Query("SELECT * FROM download_tasks ORDER BY started_at DESC")
  suspend fun getAll(): List<DownloadTaskEntity>

  /** Update task status. */
  @Query("UPDATE download_tasks SET status = :status WHERE task_id = :taskId")
  suspend fun updateStatus(taskId: String, status: DownloadStatus)

  /** Update task status with error message. */
  @Query(
    """
        UPDATE download_tasks
        SET status = :status, error_message = :errorMessage
        WHERE task_id = :taskId
        """,
  )
  suspend fun updateStatusWithError(taskId: String, status: DownloadStatus, errorMessage: String)

  /** Update download progress. */
  @Query(
    """
        UPDATE download_tasks
        SET progress = :progress, bytes_downloaded = :bytesDownloaded
        WHERE task_id = :taskId
        """,
  )
  suspend fun updateProgress(taskId: String, progress: Float, bytesDownloaded: Long)

  /** Delete tasks by status (e.g., cleanup completed tasks). */
  @Query("DELETE FROM download_tasks WHERE status = :status")
  suspend fun deleteByStatus(status: DownloadStatus)

  /** Delete all tasks (for testing/debugging). */
  @Query("DELETE FROM download_tasks") suspend fun deleteAll()

  /** Count tasks by status. */
  @Query("SELECT COUNT(*) FROM download_tasks WHERE status = :status")
  suspend fun countByStatus(status: DownloadStatus): Int

  /** Get download progress for a model. */
  @Query("SELECT progress FROM download_tasks WHERE model_id = :modelId LIMIT 1")
  suspend fun getDownloadProgress(modelId: String): Float?
}
