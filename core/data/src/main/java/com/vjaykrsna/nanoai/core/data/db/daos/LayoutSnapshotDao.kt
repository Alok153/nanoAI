package com.vjaykrsna.nanoai.core.data.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.vjaykrsna.nanoai.core.data.db.entities.LayoutSnapshotEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [LayoutSnapshotEntity].
 *
 * Provides reactive Flow-based CRUD operations, ordering management, and pinned tool
 * synchronization for saved layout configurations.
 */
@Dao
interface LayoutSnapshotDao :
  LayoutSnapshotObservationDao,
  LayoutSnapshotReadDao,
  LayoutSnapshotWriteDao,
  LayoutSnapshotMutationDao,
  LayoutSnapshotMaintenanceDao

/** Observation helpers for layout snapshots. */
interface LayoutSnapshotObservationDao {
  @Query("SELECT * FROM layout_snapshots WHERE user_id = :userId ORDER BY position ASC")
  fun observeByUserId(userId: String): Flow<List<LayoutSnapshotEntity>>

  @Query("SELECT * FROM layout_snapshots WHERE layout_id = :layoutId")
  fun observeById(layoutId: String): Flow<LayoutSnapshotEntity?>
}

/** Read helpers for layout snapshots. */
interface LayoutSnapshotReadDao {
  @Query("SELECT * FROM layout_snapshots WHERE layout_id = :layoutId")
  suspend fun getById(layoutId: String): LayoutSnapshotEntity?

  @Query("SELECT * FROM layout_snapshots WHERE user_id = :userId ORDER BY position ASC")
  suspend fun getAllByUserId(userId: String): List<LayoutSnapshotEntity>

  @Query("SELECT COUNT(*) FROM layout_snapshots WHERE user_id = :userId")
  suspend fun getCountByUserId(userId: String): Int

  @Query("SELECT MAX(position) FROM layout_snapshots WHERE user_id = :userId")
  suspend fun getMaxPosition(userId: String): Int?

  @Query(
    """
        SELECT * FROM layout_snapshots 
        WHERE user_id = :userId 
        ORDER BY position ASC
        """
  )
  suspend fun findAllLayouts(userId: String): List<LayoutSnapshotEntity>
}

/** Write helpers for layout snapshots. */
interface LayoutSnapshotWriteDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(snapshot: LayoutSnapshotEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(snapshots: List<LayoutSnapshotEntity>)

  @Update suspend fun update(snapshot: LayoutSnapshotEntity): Int
}

/** Mutation helpers for layout snapshot attributes and ordering. */
interface LayoutSnapshotMutationDao {
  @Query("UPDATE layout_snapshots SET name = :name WHERE layout_id = :layoutId")
  suspend fun updateName(layoutId: String, name: String): Int

  @Query("UPDATE layout_snapshots SET pinned_tools = :pinnedTools WHERE layout_id = :layoutId")
  suspend fun updatePinnedTools(layoutId: String, pinnedTools: List<String>): Int

  @Query("UPDATE layout_snapshots SET is_compact = :isCompact WHERE layout_id = :layoutId")
  suspend fun updateCompactMode(layoutId: String, isCompact: Boolean): Int

  @Query("UPDATE layout_snapshots SET position = :position WHERE layout_id = :layoutId")
  suspend fun updatePosition(layoutId: String, position: Int): Int

  @Transaction
  suspend fun reorderLayouts(userId: String, layoutIds: List<String>) {
    layoutIds.forEachIndexed { index, layoutId -> updatePosition(layoutId, index) }
  }
}

/** Cleanup helpers for layout snapshots. */
interface LayoutSnapshotMaintenanceDao {
  @Query("DELETE FROM layout_snapshots WHERE layout_id = :layoutId")
  suspend fun deleteById(layoutId: String): Int

  @Query("DELETE FROM layout_snapshots WHERE user_id = :userId")
  suspend fun deleteAllByUserId(userId: String): Int
}
