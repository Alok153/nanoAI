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
interface ChatThreadDao :
  ChatThreadWriteDao,
  ChatThreadReadDao,
  ChatThreadLifecycleDao,
  ChatThreadMetricsDao,
  ChatThreadMaintenanceDao

/** Write operations for chat threads. */
interface ChatThreadWriteDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(thread: ChatThreadEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(threads: List<ChatThreadEntity>)

  @Update suspend fun update(thread: ChatThreadEntity)

  @Delete suspend fun delete(thread: ChatThreadEntity)
}

/** Query helpers for retrieving chat threads. */
interface ChatThreadReadDao {
  @Query("SELECT * FROM chat_threads WHERE thread_id = :threadId")
  suspend fun getById(threadId: String): ChatThreadEntity?

  @Query("SELECT * FROM chat_threads WHERE thread_id = :threadId")
  fun observeById(threadId: String): Flow<ChatThreadEntity?>

  @Query("SELECT * FROM chat_threads WHERE is_archived = 0 ORDER BY updated_at DESC")
  suspend fun getAllActive(): List<ChatThreadEntity>

  @Query("SELECT * FROM chat_threads WHERE is_archived = 0 ORDER BY updated_at DESC")
  fun observeAllActive(): Flow<List<ChatThreadEntity>>

  @Query("SELECT * FROM chat_threads ORDER BY updated_at DESC")
  suspend fun getAll(): List<ChatThreadEntity>

  @Query("SELECT * FROM chat_threads WHERE is_archived = 1 ORDER BY updated_at DESC")
  suspend fun getArchived(): List<ChatThreadEntity>
}

/** Lifecycle mutations for chat threads. */
interface ChatThreadLifecycleDao {
  @Query("UPDATE chat_threads SET is_archived = 1 WHERE thread_id = :threadId")
  suspend fun archive(threadId: String)

  @Query("UPDATE chat_threads SET is_archived = 0 WHERE thread_id = :threadId")
  suspend fun unarchive(threadId: String)

  @Query(
    """
      UPDATE chat_threads SET persona_id = :personaId, updated_at = :updatedAt 
      WHERE thread_id = :threadId
    """
  )
  suspend fun updatePersona(
    threadId: String,
    personaId: String?,
    updatedAt: kotlinx.datetime.Instant,
  )

  @Query("UPDATE chat_threads SET updated_at = :updatedAt WHERE thread_id = :threadId")
  suspend fun touch(threadId: String, updatedAt: kotlinx.datetime.Instant)
}

/** Metrics helpers for chat thread analytics. */
interface ChatThreadMetricsDao {
  @Query("SELECT COUNT(*) FROM chat_threads WHERE is_archived = 0") suspend fun countActive(): Int

  @Query("SELECT COUNT(*) FROM chat_threads WHERE active_model_id = :modelId AND is_archived = 0")
  suspend fun countActiveByModel(modelId: String): Int
}

/** Cleanup helpers for chat threads (primarily for tests). */
interface ChatThreadMaintenanceDao {
  @Query("DELETE FROM chat_threads") suspend fun deleteAll()
}
