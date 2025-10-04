package com.vjaykrsna.nanoai.core.maintenance.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/** DAO for code quality metric snapshots. */
@Dao
interface CodeQualityMetricDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(metric: CodeQualityMetricEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(metrics: List<CodeQualityMetricEntity>)

  @Query("SELECT * FROM code_quality_metrics WHERE metric_id = :id LIMIT 1")
  suspend fun getById(id: String): CodeQualityMetricEntity?

  @Query("SELECT * FROM code_quality_metrics WHERE task_id = :taskId ORDER BY occurrences DESC")
  fun observeByTask(taskId: String): Flow<List<CodeQualityMetricEntity>>

  @Query(
    """
      SELECT * FROM code_quality_metrics WHERE occurrences > 0 
      ORDER BY CASE severity WHEN 'ERROR' THEN 2 ELSE 1 END DESC, occurrences DESC
    """,
  )
  fun observeOpenIssues(): Flow<List<CodeQualityMetricEntity>>

  @Query("SELECT * FROM code_quality_metrics WHERE occurrences = 0 ORDER BY resolved_at DESC")
  fun observeResolved(): Flow<List<CodeQualityMetricEntity>>

  @Query(
    """
      UPDATE code_quality_metrics SET occurrences = :occurrences, resolved_at = :resolvedAt 
      WHERE metric_id = :id
    """,
  )
  suspend fun updateOccurrences(id: String, occurrences: Int, resolvedAt: Instant?)

  @Query("DELETE FROM code_quality_metrics WHERE metric_id = :id") suspend fun delete(id: String)

  @Query("DELETE FROM code_quality_metrics") suspend fun clear()
}
