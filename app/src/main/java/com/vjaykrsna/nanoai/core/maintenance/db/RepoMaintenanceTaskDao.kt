package com.vjaykrsna.nanoai.core.maintenance.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vjaykrsna.nanoai.core.maintenance.model.MaintenanceCategory
import com.vjaykrsna.nanoai.core.maintenance.model.MaintenanceStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/** DAO for interacting with repo maintenance tasks stored in Room. */
@Dao
interface RepoMaintenanceTaskDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(task: RepoMaintenanceTaskEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(tasks: List<RepoMaintenanceTaskEntity>)

  @Query("SELECT * FROM repo_maintenance_tasks WHERE task_id = :id LIMIT 1")
  suspend fun getById(id: String): RepoMaintenanceTaskEntity?

  @Query(
    """
      SELECT * FROM repo_maintenance_tasks 
      ORDER BY CASE priority 
        WHEN 'CRITICAL' THEN 4 WHEN 'HIGH' THEN 3 
        WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 1 ELSE 0 
      END DESC, updated_at DESC
    """
  )
  fun observeBacklog(): Flow<List<RepoMaintenanceTaskEntity>>

  @Query("SELECT * FROM repo_maintenance_tasks WHERE status = :status ORDER BY updated_at DESC")
  fun observeByStatus(status: MaintenanceStatus): Flow<List<RepoMaintenanceTaskEntity>>

  @Query("SELECT * FROM repo_maintenance_tasks WHERE category = :category ORDER BY updated_at DESC")
  fun observeByCategory(category: MaintenanceCategory): Flow<List<RepoMaintenanceTaskEntity>>

  @Query(
    """
      UPDATE repo_maintenance_tasks SET status = :status, updated_at = :updatedAt 
      WHERE task_id = :id
    """
  )
  suspend fun updateStatus(id: String, status: MaintenanceStatus, updatedAt: Instant)

  @Query("DELETE FROM repo_maintenance_tasks WHERE task_id = :id") suspend fun delete(id: String)

  @Query("DELETE FROM repo_maintenance_tasks") suspend fun clear()
}
