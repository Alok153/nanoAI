package com.vjaykrsna.nanoai.core.data.db.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vjaykrsna.nanoai.core.data.db.entities.MessageEntity
import com.vjaykrsna.nanoai.core.model.MessageRole
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Message entities.
 *
 * Provides methods to query, insert, update, and delete messages. Messages are automatically
 * ordered by creation timestamp within threads.
 */
@Dao
interface MessageDao : MessageWriteDao, MessageReadDao, MessageAnalyticsDao, MessageMaintenanceDao

/** Write operations for message entities. */
interface MessageWriteDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(message: MessageEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(messages: List<MessageEntity>)

  @Update suspend fun update(message: MessageEntity)

  @Delete suspend fun delete(message: MessageEntity)
}

/** Read helpers for messages within threads. */
interface MessageReadDao {
  @Query("SELECT * FROM messages WHERE message_id = :messageId")
  suspend fun getById(messageId: String): MessageEntity?

  @Query("SELECT * FROM messages WHERE thread_id = :threadId ORDER BY created_at ASC")
  suspend fun getByThreadId(threadId: String): List<MessageEntity>

  @Query("SELECT * FROM messages WHERE thread_id = :threadId ORDER BY created_at ASC")
  fun observeByThreadId(threadId: String): Flow<List<MessageEntity>>

  @Query(
    """
        SELECT * FROM messages 
        WHERE thread_id = :threadId 
        ORDER BY created_at DESC 
        LIMIT :limit
        """
  )
  suspend fun getLatestMessages(threadId: String, limit: Int): List<MessageEntity>

  @Query(
    "SELECT * FROM messages WHERE thread_id = :threadId AND role = :role ORDER BY created_at ASC"
  )
  suspend fun getByRole(threadId: String, role: MessageRole): List<MessageEntity>

  @Query(
    """
        SELECT * FROM messages 
        WHERE thread_id = :threadId 
        ORDER BY created_at DESC 
        LIMIT 1
        """
  )
  suspend fun getLastMessage(threadId: String): MessageEntity?
}

/** Analytics helpers for message diagnostics. */
interface MessageAnalyticsDao {
  @Query("SELECT COUNT(*) FROM messages WHERE thread_id = :threadId")
  suspend fun countByThread(threadId: String): Int

  @Query("SELECT * FROM messages WHERE error_code IS NOT NULL ORDER BY created_at DESC")
  suspend fun getMessagesWithErrors(): List<MessageEntity>

  @Query(
    """
        SELECT AVG(latency_ms) 
        FROM messages 
        WHERE thread_id = :threadId AND latency_ms IS NOT NULL
        """
  )
  suspend fun getAverageLatency(threadId: String): Double?
}

/** Cleanup helpers for message entities. */
interface MessageMaintenanceDao {
  @Query("DELETE FROM messages WHERE thread_id = :threadId")
  suspend fun deleteByThreadId(threadId: String)

  @Query("DELETE FROM messages") suspend fun deleteAll()
}
