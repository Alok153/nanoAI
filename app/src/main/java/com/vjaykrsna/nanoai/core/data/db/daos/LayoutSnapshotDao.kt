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
 * Provides reactive Flow-based CRUD operations, ordering management,
 * and pinned tool synchronization for saved layout configurations.
 */
@Dao
interface LayoutSnapshotDao {
    /**
     * Observe all layout snapshots for a user, ordered by position.
     *
     * @param userId The unique user identifier
     * @return Flow emitting list of layout snapshots ordered by position
     */
    @Query("SELECT * FROM layout_snapshots WHERE user_id = :userId ORDER BY position ASC")
    fun observeByUserId(userId: String): Flow<List<LayoutSnapshotEntity>>

    /**
     * Observe a single layout snapshot by ID.
     *
     * @param layoutId The unique layout identifier
     * @return Flow emitting the layout snapshot or null if not found
     */
    @Query("SELECT * FROM layout_snapshots WHERE layout_id = :layoutId")
    fun observeById(layoutId: String): Flow<LayoutSnapshotEntity?>

    /**
     * Get a single layout snapshot by ID (one-shot query).
     *
     * @param layoutId The unique layout identifier
     * @return The layout snapshot or null if not found
     */
    @Query("SELECT * FROM layout_snapshots WHERE layout_id = :layoutId")
    suspend fun getById(layoutId: String): LayoutSnapshotEntity?

    /**
     * Get all layout snapshots for a user, ordered by position (one-shot query).
     *
     * @param userId The unique user identifier
     * @return List of layout snapshots ordered by position
     */
    @Query("SELECT * FROM layout_snapshots WHERE user_id = :userId ORDER BY position ASC")
    suspend fun getAllByUserId(userId: String): List<LayoutSnapshotEntity>

    /**
     * Get count of layout snapshots for a user.
     *
     * @param userId The unique user identifier
     * @return Number of layout snapshots for the user
     */
    @Query("SELECT COUNT(*) FROM layout_snapshots WHERE user_id = :userId")
    suspend fun getCountByUserId(userId: String): Int

    /**
     * Insert or replace a layout snapshot.
     *
     * @param snapshot The layout snapshot to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: LayoutSnapshotEntity)

    /**
     * Insert multiple layout snapshots.
     *
     * @param snapshots List of layout snapshots to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(snapshots: List<LayoutSnapshotEntity>)

    /**
     * Update an existing layout snapshot.
     *
     * @param snapshot The layout snapshot with updated fields
     * @return Number of rows updated (should be 1 if successful)
     */
    @Update
    suspend fun update(snapshot: LayoutSnapshotEntity): Int

    /**
     * Delete a layout snapshot by ID.
     *
     * @param layoutId The unique layout identifier
     * @return Number of rows deleted (should be 1 if successful)
     */
    @Query("DELETE FROM layout_snapshots WHERE layout_id = :layoutId")
    suspend fun deleteById(layoutId: String): Int

    /**
     * Delete all layout snapshots for a user.
     *
     * @param userId The unique user identifier
     * @return Number of rows deleted
     */
    @Query("DELETE FROM layout_snapshots WHERE user_id = :userId")
    suspend fun deleteAllByUserId(userId: String): Int

    /**
     * Update the name of a layout snapshot.
     *
     * @param layoutId The unique layout identifier
     * @param name The new name (max 64 characters)
     * @return Number of rows updated (should be 1 if successful)
     */
    @Query("UPDATE layout_snapshots SET name = :name WHERE layout_id = :layoutId")
    suspend fun updateName(
        layoutId: String,
        name: String,
    ): Int

    /**
     * Update the pinned tools for a layout snapshot.
     *
     * @param layoutId The unique layout identifier
     * @param pinnedTools List of tool IDs (max 10 items)
     * @return Number of rows updated (should be 1 if successful)
     */
    @Query("UPDATE layout_snapshots SET pinned_tools = :pinnedTools WHERE layout_id = :layoutId")
    suspend fun updatePinnedTools(
        layoutId: String,
        pinnedTools: List<String>,
    ): Int

    /**
     * Update the compact mode flag for a layout snapshot.
     *
     * @param layoutId The unique layout identifier
     * @param isCompact True to enable compact mode
     * @return Number of rows updated (should be 1 if successful)
     */
    @Query("UPDATE layout_snapshots SET is_compact = :isCompact WHERE layout_id = :layoutId")
    suspend fun updateCompactMode(
        layoutId: String,
        isCompact: Boolean,
    ): Int

    /**
     * Update the position of a layout snapshot.
     *
     * @param layoutId The unique layout identifier
     * @param position The new position (0-based index)
     * @return Number of rows updated (should be 1 if successful)
     */
    @Query("UPDATE layout_snapshots SET position = :position WHERE layout_id = :layoutId")
    suspend fun updatePosition(
        layoutId: String,
        position: Int,
    ): Int

    /**
     * Reorder layout snapshots for a user.
     *
     * This transaction updates positions for all snapshots to match the provided list order.
     *
     * @param userId The unique user identifier
     * @param layoutIds List of layout IDs in desired order
     */
    @Transaction
    suspend fun reorderLayouts(
        userId: String,
        layoutIds: List<String>,
    ) {
        layoutIds.forEachIndexed { index, layoutId ->
            updatePosition(layoutId, index)
        }
    }

    /**
     * Get the maximum position for a user's layout snapshots.
     *
     * Useful for appending new layouts at the end.
     *
     * @param userId The unique user identifier
     * @return The maximum position, or null if no layouts exist
     */
    @Query("SELECT MAX(position) FROM layout_snapshots WHERE user_id = :userId")
    suspend fun getMaxPosition(userId: String): Int?

    /**
     * Find layout snapshots containing a specific tool in their pinned tools.
     *
     * @param userId The unique user identifier
     * @param toolId The tool ID to search for
     * @return List of layout snapshots containing the tool
     */
    @Query(
        """
        SELECT * FROM layout_snapshots 
        WHERE user_id = :userId 
        ORDER BY position ASC
        """,
    )
    suspend fun findLayoutsWithTool(
        userId: String,
        toolId: String,
    ): List<LayoutSnapshotEntity>
}
