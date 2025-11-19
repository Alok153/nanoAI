package com.vjaykrsna.nanoai.core.domain.chat

import android.database.sqlite.SQLiteException
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.annotations.OneShot
import com.vjaykrsna.nanoai.core.common.annotations.ReactiveStream
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.domain.model.Message
import com.vjaykrsna.nanoai.core.domain.repository.ConversationRepository
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

/** Use case for conversation operations. */
@Singleton
class ConversationUseCase
@Inject
constructor(private val conversationRepository: ConversationRepository) :
  ConversationUseCaseInterface {
  /** Observe all threads (reactive stream - not wrapped in NanoAIResult). */
  @ReactiveStream("Full chat thread catalog")
  override fun getAllThreadsFlow(): Flow<List<ChatThread>> {
    return conversationRepository.getAllThreadsFlow()
  }

  /** Observe messages for a specific thread (reactive stream - not wrapped in NanoAIResult). */
  @ReactiveStream("Messages for an individual thread")
  override fun getMessagesFlow(threadId: UUID): Flow<List<Message>> {
    return conversationRepository.getMessagesFlow(threadId)
  }

  /** Fetch all threads once (not reactive). */
  @OneShot("Snapshot all chat threads")
  override suspend fun getAllThreads(): NanoAIResult<List<ChatThread>> =
    guardConversationOperation(
      message = "Failed to load chat history",
      context = mapOf("operation" to "getAllThreads"),
    ) {
      val threads = conversationRepository.getAllThreads()
      NanoAIResult.success(threads)
    }

  /** Create a new thread. */
  @OneShot("Create a chat thread")
  override suspend fun createNewThread(personaId: UUID, title: String?): NanoAIResult<UUID> =
    guardConversationOperation(
      message = "Failed to create new thread",
      context = mapOf("personaId" to personaId.toString(), "title" to (title ?: "")),
    ) {
      val threadId = conversationRepository.createNewThread(personaId, title)
      NanoAIResult.success(threadId)
    }

  /** Archive a thread. */
  @OneShot("Archive existing thread")
  override suspend fun archiveThread(threadId: UUID): NanoAIResult<Unit> =
    guardConversationOperation(
      message = "Failed to archive thread $threadId",
      context = mapOf("threadId" to threadId.toString()),
    ) {
      conversationRepository.archiveThread(threadId)
      NanoAIResult.success(Unit)
    }

  /** Delete a thread. */
  @OneShot("Delete existing thread")
  override suspend fun deleteThread(threadId: UUID): NanoAIResult<Unit> =
    guardConversationOperation(
      message = "Failed to delete thread $threadId",
      context = mapOf("threadId" to threadId.toString()),
    ) {
      conversationRepository.deleteThread(threadId)
      NanoAIResult.success(Unit)
    }

  /** Save a message. */
  @OneShot("Persist a chat message")
  override suspend fun saveMessage(message: Message): NanoAIResult<Unit> =
    guardConversationOperation(
      message = "Failed to save message ${message.messageId}",
      context =
        mapOf(
          "messageId" to message.messageId.toString(),
          "threadId" to message.threadId.toString(),
          "role" to message.role.toString(),
        ),
    ) {
      conversationRepository.saveMessage(message)
      NanoAIResult.success(Unit)
    }

  /** Get the current persona for a thread. */
  @OneShot("Resolve persona for a thread")
  override suspend fun getCurrentPersonaForThread(threadId: UUID): NanoAIResult<UUID?> =
    guardConversationOperation(
      message = "Failed to get current persona for thread $threadId",
      context = mapOf("threadId" to threadId.toString()),
    ) {
      val personaId = conversationRepository.getCurrentPersonaForThread(threadId)
      NanoAIResult.success(personaId)
    }

  /** Update a thread's persona. */
  @OneShot("Update persona on a thread")
  override suspend fun updateThreadPersona(threadId: UUID, personaId: UUID?): NanoAIResult<Unit> =
    guardConversationOperation(
      message = "Failed to update persona for thread $threadId",
      context =
        mapOf("threadId" to threadId.toString(), "personaId" to (personaId?.toString() ?: "null")),
    ) {
      conversationRepository.updateThreadPersona(threadId, personaId)
      NanoAIResult.success(Unit)
    }

  /** Update a thread. */
  @OneShot("Update thread metadata")
  suspend fun updateThread(thread: ChatThread): NanoAIResult<Unit> =
    guardConversationOperation(
      message = "Failed to update thread ${thread.threadId}",
      context = mapOf("threadId" to thread.threadId.toString()),
    ) {
      conversationRepository.updateThread(thread)
      NanoAIResult.success(Unit)
    }

  private inline fun <T> guardConversationOperation(
    message: String,
    context: Map<String, String> = emptyMap(),
    block: () -> NanoAIResult<T>,
  ): NanoAIResult<T> {
    return try {
      block()
    } catch (cancellation: CancellationException) {
      throw cancellation
    } catch (sqliteException: SQLiteException) {
      NanoAIResult.recoverable(message = message, cause = sqliteException, context = context)
    } catch (ioException: IOException) {
      NanoAIResult.recoverable(message = message, cause = ioException, context = context)
    } catch (illegalStateException: IllegalStateException) {
      NanoAIResult.recoverable(message = message, cause = illegalStateException, context = context)
    } catch (illegalArgumentException: IllegalArgumentException) {
      NanoAIResult.recoverable(
        message = message,
        cause = illegalArgumentException,
        context = context,
      )
    } catch (securityException: SecurityException) {
      NanoAIResult.recoverable(message = message, cause = securityException, context = context)
    }
  }
}
