package com.vjaykrsna.nanoai.core.data.db.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vjaykrsna.nanoai.core.data.db.entities.ChatThreadEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for ChatThread entities.
 *
 * Provides methods to query, insert, update, and delete chat threads. All methods are suspend
 * functions for use with Kotlin coroutines.
 */
@Dao
@Suppress("TooManyFunctions") // DAOs naturally have many CRUD operations
interface ChatThreadDao {
  /** Insert a new chat thread. Replaces if thread with same ID exists. */
  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(thread: ChatThreadEntity)

  /** Insert multiple chat threads. */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(threads: List<ChatThreadEntity>)

  /** Update an existing chat thread. */
  @Update suspend fun update(thread: ChatThreadEntity)

  /** Delete a chat thread. Associated messages will be cascade deleted. */
  @Delete suspend fun delete(thread: ChatThreadEntity)

  /** Get a specific thread by ID. */
  @Query("SELECT * FROM chat_threads WHERE thread_id = :threadId")
  suspend fun getById(threadId: String): ChatThreadEntity?

  /** Observe a specific thread by ID (reactive). */
  @Query("SELECT * FROM chat_threads WHERE thread_id = :threadId")
  fun observeById(threadId: String): Flow<ChatThreadEntity?>

  /**
   * Get all threads ordered by update time (most recent first). Excludes archived threads by
   * default.
   */
  @Query("SELECT * FROM chat_threads WHERE is_archived = 0 ORDER BY updated_at DESC")
  suspend fun getAllActive(): List<ChatThreadEntity>

  /** Observe all active threads (reactive). */
  @Query("SELECT * FROM chat_threads WHERE is_archived = 0 ORDER BY updated_at DESC")
  fun observeAllActive(): Flow<List<ChatThreadEntity>>

  /** Get all threads including archived ones. */
  @Query("SELECT * FROM chat_threads ORDER BY updated_at DESC")
  suspend fun getAll(): List<ChatThreadEntity>

  /** Get archived threads only. */
  @Query("SELECT * FROM chat_threads WHERE is_archived = 1 ORDER BY updated_at DESC")
  suspend fun getArchived(): List<ChatThreadEntity>

  /** Archive a thread by ID. */
  @Query("UPDATE chat_threads SET is_archived = 1 WHERE thread_id = :threadId")
  suspend fun archive(threadId: String)

  /** Unarchive a thread by ID. */
  @Query("UPDATE chat_threads SET is_archived = 0 WHERE thread_id = :threadId")
  suspend fun unarchive(threadId: String)

  /** Delete all threads (for testing/debugging). */
  @Query("DELETE FROM chat_threads") suspend fun deleteAll()

  /** Count total threads (excluding archived). */
  @Query("SELECT COUNT(*) FROM chat_threads WHERE is_archived = 0") suspend fun countActive(): Int

  /** Count active (non-archived) threads that currently reference the provided model ID. */
  @Query("SELECT COUNT(*) FROM chat_threads WHERE active_model_id = :modelId AND is_archived = 0")
  suspend fun countActiveByModel(modelId: String): Int

  /** Update the persona associated with a thread. */
  @Query(
    """
      UPDATE chat_threads SET persona_id = :personaId, updated_at = :updatedAt 
      WHERE thread_id = :threadId
    """
  )
  suspend fun updatePersona(
    threadId: String,
    personaId: String?,
    updatedAt: kotlinx.datetime.Instant
  )
}
