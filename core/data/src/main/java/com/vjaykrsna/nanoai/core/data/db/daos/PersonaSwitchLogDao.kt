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
  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(log: PersonaSwitchLogEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(logs: List<PersonaSwitchLogEntity>)

  @Delete suspend fun delete(log: PersonaSwitchLogEntity)

  @Query("SELECT * FROM persona_switch_logs WHERE thread_id = :threadId ORDER BY created_at ASC")
  suspend fun getByThreadId(threadId: String): List<PersonaSwitchLogEntity>

  @Query("SELECT * FROM persona_switch_logs WHERE thread_id = :threadId ORDER BY created_at ASC")
  fun observeByThreadId(threadId: String): Flow<List<PersonaSwitchLogEntity>>

  @Query(
    """
        SELECT * FROM persona_switch_logs 
        WHERE thread_id = :threadId 
        ORDER BY created_at DESC 
        LIMIT 1
        """
  )
  suspend fun getLatestForThread(threadId: String): PersonaSwitchLogEntity?

  @Query(
    """
        SELECT * FROM persona_switch_logs 
        WHERE new_persona_id = :personaId OR previous_persona_id = :personaId
        ORDER BY created_at DESC
        """
  )
  suspend fun getByPersonaId(personaId: String): List<PersonaSwitchLogEntity>

  @Query("SELECT COUNT(*) FROM persona_switch_logs WHERE thread_id = :threadId")
  suspend fun countByThread(threadId: String): Int

  @Query("SELECT * FROM persona_switch_logs ORDER BY created_at DESC")
  suspend fun getAll(): List<PersonaSwitchLogEntity>

  @Query("DELETE FROM persona_switch_logs WHERE thread_id = :threadId")
  suspend fun deleteByThreadId(threadId: String)

  @Query("DELETE FROM persona_switch_logs") suspend fun deleteAll()
}
