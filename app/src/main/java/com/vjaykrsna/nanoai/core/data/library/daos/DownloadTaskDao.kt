package com.vjaykrsna.nanoai.core.data.library.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vjaykrsna.nanoai.core.data.library.entities.DownloadTaskEntity
import com.vjaykrsna.nanoai.core.domain.model.library.DownloadStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for DownloadTask entities.
 *
 * Provides methods to manage model download tasks and progress tracking.
 */
@Dao
interface DownloadTaskDao :
  DownloadTaskWriteDao,
  DownloadTaskReadDao,
  DownloadTaskObservationDao,
  DownloadTaskStatusDao,
  DownloadTaskMaintenanceDao,
  DownloadTaskMetricsDao

/** Write helpers for download tasks. */
interface DownloadTaskWriteDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(task: DownloadTaskEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(tasks: List<DownloadTaskEntity>)

  @Update suspend fun update(task: DownloadTaskEntity)

  @Delete suspend fun delete(task: DownloadTaskEntity)
}

/** Read helpers for download task queries. */
interface DownloadTaskReadDao {
  @Query("SELECT * FROM download_tasks WHERE task_id = :taskId")
  suspend fun getById(taskId: String): DownloadTaskEntity?

  @Query("SELECT * FROM download_tasks WHERE model_id = :modelId LIMIT 1")
  suspend fun getByModelId(modelId: String): DownloadTaskEntity?

  @Query("SELECT model_id FROM download_tasks WHERE task_id = :taskId")
  suspend fun getModelIdForTask(taskId: String): String?

  @Query("SELECT * FROM download_tasks WHERE status = :status ORDER BY started_at ASC")
  suspend fun getByStatus(status: DownloadStatus): List<DownloadTaskEntity>

  @Query("SELECT * FROM download_tasks WHERE status = 'QUEUED' ORDER BY started_at ASC")
  suspend fun getQueuedDownloads(): List<DownloadTaskEntity>

  @Query("SELECT * FROM download_tasks WHERE status = 'DOWNLOADING' ORDER BY started_at ASC")
  suspend fun getActiveDownloads(): List<DownloadTaskEntity>

  @Query("SELECT * FROM download_tasks WHERE status = 'FAILED' ORDER BY finished_at DESC")
  suspend fun getFailedDownloads(): List<DownloadTaskEntity>

  @Query("SELECT * FROM download_tasks ORDER BY started_at DESC")
  suspend fun getAll(): List<DownloadTaskEntity>

  @Query("SELECT progress FROM download_tasks WHERE model_id = :modelId LIMIT 1")
  suspend fun getDownloadProgress(modelId: String): Float?
}

/** Observation helpers for monitoring download tasks. */
interface DownloadTaskObservationDao {
  @Query("SELECT * FROM download_tasks WHERE task_id = :taskId")
  fun observeById(taskId: String): Flow<DownloadTaskEntity?>

  @Query("SELECT * FROM download_tasks WHERE status = 'QUEUED' ORDER BY started_at ASC")
  fun observeQueuedDownloads(): Flow<List<DownloadTaskEntity>>

  @Query(
    """
        SELECT *
        FROM download_tasks
        WHERE status IN ('QUEUED','DOWNLOADING','PAUSED','FAILED')
        ORDER BY started_at ASC
        """
  )
  fun observeManagedDownloads(): Flow<List<DownloadTaskEntity>>

  @Query("SELECT * FROM download_tasks WHERE status = 'DOWNLOADING' ORDER BY started_at ASC")
  fun observeActiveDownloads(): Flow<List<DownloadTaskEntity>>
}

/** Status mutation helpers for download tasks. */
interface DownloadTaskStatusDao {
  @Query("UPDATE download_tasks SET status = :status WHERE task_id = :taskId")
  suspend fun updateStatus(taskId: String, status: DownloadStatus)

  @Query(
    """
        UPDATE download_tasks
        SET status = :status, error_message = :errorMessage
        WHERE task_id = :taskId
        """
  )
  suspend fun updateStatusWithError(taskId: String, status: DownloadStatus, errorMessage: String)

  @Query(
    """
        UPDATE download_tasks
        SET progress = :progress, bytes_downloaded = :bytesDownloaded
        WHERE task_id = :taskId
        """
  )
  suspend fun updateProgress(taskId: String, progress: Float, bytesDownloaded: Long)
}

/** Maintenance helpers for download tasks. */
interface DownloadTaskMaintenanceDao {
  @Query("DELETE FROM download_tasks WHERE status = :status")
  suspend fun deleteByStatus(status: DownloadStatus)

  @Query("DELETE FROM download_tasks") suspend fun deleteAll()
}

/** Metrics helpers for download task analytics. */
interface DownloadTaskMetricsDao {
  @Query("SELECT COUNT(*) FROM download_tasks WHERE status = :status")
  suspend fun countByStatus(status: DownloadStatus): Int
}
