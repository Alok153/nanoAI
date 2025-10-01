package com.vjaykrsna.nanoai.core.data.db.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vjaykrsna.nanoai.core.data.db.entities.PersonaSwitchLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for PersonaSwitchLog entities.
 * 
 * Provides methods to track and query persona switching history.
 */
@Dao
interface PersonaSwitchLogDao {

    /**
     * Insert a new persona switch log entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: PersonaSwitchLogEntity)

    /**
     * Insert multiple log entries.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<PersonaSwitchLogEntity>)

    /**
     * Delete a log entry.
     */
    @Delete
    suspend fun delete(log: PersonaSwitchLogEntity)

    /**
     * Get all switch logs for a specific thread, ordered by time.
     */
    @Query("SELECT * FROM persona_switch_logs WHERE thread_id = :threadId ORDER BY created_at ASC")
    suspend fun getByThreadId(threadId: String): List<PersonaSwitchLogEntity>

    /**
     * Observe switch logs for a thread (reactive).
     */
    @Query("SELECT * FROM persona_switch_logs WHERE thread_id = :threadId ORDER BY created_at ASC")
    fun observeByThreadId(threadId: String): Flow<List<PersonaSwitchLogEntity>>

    /**
     * Get the most recent switch log for a thread.
     */
    @Query(
        """
        SELECT * FROM persona_switch_logs 
        WHERE thread_id = :threadId 
        ORDER BY created_at DESC 
        LIMIT 1
        """
    )
    suspend fun getLatestForThread(threadId: String): PersonaSwitchLogEntity?

    /**
     * Get all switches involving a specific persona (as either old or new).
     */
    @Query(
        """
        SELECT * FROM persona_switch_logs 
        WHERE new_persona_id = :personaId OR previous_persona_id = :personaId
        ORDER BY created_at DESC
        """
    )
    suspend fun getByPersonaId(personaId: String): List<PersonaSwitchLogEntity>

    /**
     * Count switches for a specific thread.
     */
    @Query("SELECT COUNT(*) FROM persona_switch_logs WHERE thread_id = :threadId")
    suspend fun countByThread(threadId: String): Int

    /**
     * Get all switch logs ordered by time (for analytics).
     */
    @Query("SELECT * FROM persona_switch_logs ORDER BY created_at DESC")
    suspend fun getAll(): List<PersonaSwitchLogEntity>

    /**
     * Delete all logs for a specific thread.
     * Note: CASCADE delete should handle this automatically.
     */
    @Query("DELETE FROM persona_switch_logs WHERE thread_id = :threadId")
    suspend fun deleteByThreadId(threadId: String)

    /**
     * Delete all logs (for testing/debugging).
     */
    @Query("DELETE FROM persona_switch_logs")
    suspend fun deleteAll()
}
