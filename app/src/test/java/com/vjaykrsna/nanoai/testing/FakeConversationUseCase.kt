package com.vjaykrsna.nanoai.testing

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.domain.model.Message
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** Fake implementation of [ConversationUseCase] for testing. */
@Singleton
class FakeConversationUseCase
@Inject
constructor(private val fakeConversationRepository: FakeConversationRepository) {

  var shouldFailOnArchiveThread = false
  var shouldFailOnDeleteThread = false

  fun getAllThreadsFlow(): Flow<List<ChatThread>> {
    return fakeConversationRepository.getAllThreadsFlow()
  }

  fun getMessagesFlow(threadId: UUID): Flow<List<Message>> {
    return fakeConversationRepository.getMessagesFlow(threadId)
  }

  suspend fun createNewThread(personaId: UUID, title: String?): NanoAIResult<UUID> {
    return try {
      val threadId = fakeConversationRepository.createNewThread(personaId, title)
      NanoAIResult.success(threadId)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to create new thread",
        cause = e,
        context = mapOf("personaId" to personaId.toString(), "title" to (title ?: "")),
      )
    }
  }

  suspend fun archiveThread(threadId: UUID): NanoAIResult<Unit> {
    if (shouldFailOnArchiveThread) {
      return NanoAIResult.recoverable(
        message = "Failed to archive thread $threadId",
        cause = null,
        context = mapOf("threadId" to threadId.toString()),
      )
    }
    return try {
      fakeConversationRepository.archiveThread(threadId)
      NanoAIResult.success(Unit)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to archive thread $threadId",
        cause = e,
        context = mapOf("threadId" to threadId.toString()),
      )
    }
  }

  suspend fun deleteThread(threadId: UUID): NanoAIResult<Unit> {
    if (shouldFailOnDeleteThread) {
      return NanoAIResult.recoverable(
        message = "Failed to delete thread $threadId",
        cause = null,
        context = mapOf("threadId" to threadId.toString()),
      )
    }
    return try {
      fakeConversationRepository.deleteThread(threadId)
      NanoAIResult.success(Unit)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to delete thread $threadId",
        cause = e,
        context = mapOf("threadId" to threadId.toString()),
      )
    }
  }

  suspend fun saveMessage(message: Message): NanoAIResult<Unit> {
    return try {
      fakeConversationRepository.saveMessage(message)
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

  suspend fun getCurrentPersonaForThread(threadId: UUID): NanoAIResult<UUID?> {
    return try {
      val personaId = fakeConversationRepository.getCurrentPersonaForThread(threadId)
      NanoAIResult.success(personaId)
    } catch (e: Exception) {
      NanoAIResult.recoverable(
        message = "Failed to get current persona for thread $threadId",
        cause = e,
        context = mapOf("threadId" to threadId.toString()),
      )
    }
  }

  suspend fun updateThreadPersona(threadId: UUID, personaId: UUID?): NanoAIResult<Unit> {
    return try {
      fakeConversationRepository.updateThreadPersona(threadId, personaId)
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
