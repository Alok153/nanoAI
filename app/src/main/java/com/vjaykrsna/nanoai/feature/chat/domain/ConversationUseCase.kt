package com.vjaykrsna.nanoai.feature.chat.domain

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.data.repository.ConversationRepository
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.domain.model.Message
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** Use case for conversation operations. */
@Singleton
class ConversationUseCase
@Inject
constructor(private val conversationRepository: ConversationRepository) {
  /** Observe all threads (reactive stream - not wrapped in NanoAIResult). */
  fun getAllThreadsFlow(): Flow<List<ChatThread>> {
    return conversationRepository.getAllThreadsFlow()
  }

  /** Observe messages for a specific thread (reactive stream - not wrapped in NanoAIResult). */
  fun getMessagesFlow(threadId: UUID): Flow<List<Message>> {
    return conversationRepository.getMessagesFlow(threadId)
  }

  /** Create a new thread. */
  suspend fun createNewThread(personaId: UUID, title: String? = null): NanoAIResult<UUID> {
    return try {
      val threadId = conversationRepository.createNewThread(personaId, title)
      NanoAIResult.success(threadId)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to create new thread",
        cause = e,
        context = mapOf("personaId" to personaId.toString(), "title" to (title ?: "")),
      )
    }
  }

  /** Archive a thread. */
  suspend fun archiveThread(threadId: UUID): NanoAIResult<Unit> {
    return try {
      conversationRepository.archiveThread(threadId)
      NanoAIResult.success(Unit)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to archive thread $threadId",
        cause = e,
        context = mapOf("threadId" to threadId.toString()),
      )
    }
  }

  /** Delete a thread. */
  suspend fun deleteThread(threadId: UUID): NanoAIResult<Unit> {
    return try {
      conversationRepository.deleteThread(threadId)
      NanoAIResult.success(Unit)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to delete thread $threadId",
        cause = e,
        context = mapOf("threadId" to threadId.toString()),
      )
    }
  }

  /** Save a message. */
  suspend fun saveMessage(message: Message): NanoAIResult<Unit> {
    return try {
      conversationRepository.saveMessage(message)
      NanoAIResult.success(Unit)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to save message ${message.messageId}",
        cause = e,
        context =
          mapOf(
            "messageId" to message.messageId.toString(),
            "threadId" to message.threadId.toString(),
            "role" to message.role.toString(),
          ),
      )
    }
  }

  /** Get the current persona for a thread. */
  suspend fun getCurrentPersonaForThread(threadId: UUID): NanoAIResult<UUID?> {
    return try {
      val personaId = conversationRepository.getCurrentPersonaForThread(threadId)
      NanoAIResult.success(personaId)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to get current persona for thread $threadId",
        cause = e,
        context = mapOf("threadId" to threadId.toString()),
      )
    }
  }

  /** Update the persona for a thread. */
  suspend fun updateThreadPersona(threadId: UUID, personaId: UUID?): NanoAIResult<Unit> {
    return try {
      conversationRepository.updateThreadPersona(threadId, personaId)
      NanoAIResult.success(Unit)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to update persona for thread $threadId",
        cause = e,
        context =
          mapOf("threadId" to threadId.toString(), "personaId" to (personaId?.toString() ?: "null")),
      )
    }
  }
}
