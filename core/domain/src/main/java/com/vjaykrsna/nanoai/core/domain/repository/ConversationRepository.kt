package com.vjaykrsna.nanoai.core.domain.repository

import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.domain.model.Message
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/**
 * Repository for conversation management.
 *
 * Provides clean API for chat threads and messages, abstracting database access through domain
 * models.
 */
interface ChatThreadRepository {
  /** Get a specific chat thread by ID. */
  suspend fun getThread(threadId: UUID): ChatThread?

  /** Get all chat threads (not archived). */
  suspend fun getAllThreads(): List<ChatThread>

  /** Get archived chat threads. */
  suspend fun getArchivedThreads(): List<ChatThread>

  /** Create a new chat thread. */
  suspend fun createThread(thread: ChatThread)

  /** Update an existing chat thread. */
  suspend fun updateThread(thread: ChatThread)

  /** Archive a chat thread. */
  suspend fun archiveThread(threadId: UUID)

  /** Delete a chat thread (cascade deletes messages). */
  suspend fun deleteThread(threadId: UUID)

  /** Observe all threads (reactive updates). */
  fun getAllThreadsFlow(): Flow<List<ChatThread>>

  /** Retrieve the current persona associated with the thread (if any). */
  suspend fun getCurrentPersonaForThread(threadId: UUID): UUID?

  /** Create a brand-new thread seeded with the provided persona. */
  suspend fun createNewThread(personaId: UUID, title: String? = null): UUID

  /** Update a thread's persona association. */
  suspend fun updateThreadPersona(threadId: UUID, personaId: UUID?)
}

interface ChatMessageRepository {
  /** Add a message to a thread. */
  suspend fun addMessage(message: Message)

  /** Save a message (alias for addMessage, kept for test compatibility). */
  suspend fun saveMessage(message: Message) = addMessage(message)

  /** Get all messages for a thread. */
  suspend fun getMessages(threadId: UUID): List<Message>

  /** Observe messages for a thread (reactive updates). */
  fun getMessagesFlow(threadId: UUID): Flow<List<Message>>
}

interface ConversationRepository : ChatThreadRepository, ChatMessageRepository
