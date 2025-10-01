package com.vjaykrsna.nanoai.core.data.db.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.vjaykrsna.nanoai.core.data.db.entities.MessageEntity
import com.vjaykrsna.nanoai.core.model.Role
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Message entities.
 * 
 * Provides methods to query, insert, update, and delete messages.
 * Messages are automatically ordered by creation timestamp within threads.
 */
@Dao
interface MessageDao {

    /**
     * Insert a new message. Replaces if message with same ID exists.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    /**
     * Insert multiple messages.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    /**
     * Update an existing message.
     */
    @Update
    suspend fun update(message: MessageEntity)

    /**
     * Delete a message.
     */
    @Delete
    suspend fun delete(message: MessageEntity)

    /**
     * Get a specific message by ID.
     */
    @Query("SELECT * FROM messages WHERE message_id = :messageId")
    suspend fun getById(messageId: String): MessageEntity?

    /**
     * Get all messages for a thread, ordered by creation time (ascending).
     * Uses composite index on (thread_id, created_at) for performance.
     */
    @Query("SELECT * FROM messages WHERE thread_id = :threadId ORDER BY created_at ASC")
    suspend fun getByThreadId(threadId: String): List<MessageEntity>

    /**
     * Observe all messages for a thread (reactive).
     */
    @Query("SELECT * FROM messages WHERE thread_id = :threadId ORDER BY created_at ASC")
    fun observeByThreadId(threadId: String): Flow<List<MessageEntity>>

    /**
     * Get the latest N messages for a thread (for context window).
     */
    @Query(
        """
        SELECT * FROM messages 
        WHERE thread_id = :threadId 
        ORDER BY created_at DESC 
        LIMIT :limit
        """
    )
    suspend fun getLatestMessages(threadId: String, limit: Int): List<MessageEntity>

    /**
     * Get messages by role (e.g., all USER messages).
     */
    @Query("SELECT * FROM messages WHERE thread_id = :threadId AND role = :role ORDER BY created_at ASC")
    suspend fun getByRole(threadId: String, role: Role): List<MessageEntity>

    /**
     * Get the last message in a thread.
     */
    @Query(
        """
        SELECT * FROM messages 
        WHERE thread_id = :threadId 
        ORDER BY created_at DESC 
        LIMIT 1
        """
    )
    suspend fun getLastMessage(threadId: String): MessageEntity?

    /**
     * Count messages in a thread.
     */
    @Query("SELECT COUNT(*) FROM messages WHERE thread_id = :threadId")
    suspend fun countByThread(threadId: String): Int

    /**
     * Delete all messages for a specific thread.
     * Note: CASCADE delete should handle this automatically when thread is deleted.
     */
    @Query("DELETE FROM messages WHERE thread_id = :threadId")
    suspend fun deleteByThreadId(threadId: String)

    /**
     * Delete all messages (for testing/debugging).
     */
    @Query("DELETE FROM messages")
    suspend fun deleteAll()

    /**
     * Get messages with errors (for debugging/monitoring).
     */
    @Query("SELECT * FROM messages WHERE error_code IS NOT NULL ORDER BY created_at DESC")
    suspend fun getMessagesWithErrors(): List<MessageEntity>

    /**
     * Get average latency for a specific thread.
     */
    @Query(
        """
        SELECT AVG(latency_ms) 
        FROM messages 
        WHERE thread_id = :threadId AND latency_ms IS NOT NULL
        """
    )
    suspend fun getAverageLatency(threadId: String): Double?
}
