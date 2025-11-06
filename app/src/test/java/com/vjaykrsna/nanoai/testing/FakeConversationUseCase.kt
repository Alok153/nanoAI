package com.vjaykrsna.nanoai.testing

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.domain.model.Message
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
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
    return guard(
      message = "Failed to create new thread",
      context = mapOf("personaId" to personaId.toString(), "title" to (title ?: "")),
    ) {
      fakeConversationRepository.createNewThread(personaId, title)
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
    return guard(
      message = "Failed to archive thread $threadId",
      context = mapOf("threadId" to threadId.toString()),
    ) {
      fakeConversationRepository.archiveThread(threadId)
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
    return guard(
      message = "Failed to delete thread $threadId",
      context = mapOf("threadId" to threadId.toString()),
    ) {
      fakeConversationRepository.deleteThread(threadId)
    }
  }

  suspend fun saveMessage(message: Message): NanoAIResult<Unit> {
    return guard(
      message = "Failed to save message ${message.messageId}",
      context =
        mapOf(
          "messageId" to message.messageId.toString(),
          "threadId" to message.threadId.toString(),
          "role" to message.role.toString(),
        ),
    ) {
      fakeConversationRepository.saveMessage(message)
    }
  }

  suspend fun getCurrentPersonaForThread(threadId: UUID): NanoAIResult<UUID?> {
    return guard(
      message = "Failed to get current persona for thread $threadId",
      context = mapOf("threadId" to threadId.toString()),
    ) {
      fakeConversationRepository.getCurrentPersonaForThread(threadId)
    }
  }

  suspend fun updateThreadPersona(threadId: UUID, personaId: UUID?): NanoAIResult<Unit> {
    return guard(
      message = "Failed to update persona for thread $threadId",
      context =
        mapOf("threadId" to threadId.toString(), "personaId" to (personaId?.toString() ?: "null")),
    ) {
      fakeConversationRepository.updateThreadPersona(threadId, personaId)
    }
  }

  private suspend inline fun <T> guard(
    message: String,
    context: Map<String, String>,
    block: suspend () -> T,
  ): NanoAIResult<T> {
    return try {
      val value = block()
      NanoAIResult.success(value)
    } catch (cancellationException: CancellationException) {
      throw cancellationException
    } catch (illegalStateException: IllegalStateException) {
      NanoAIResult.recoverable(message = message, cause = illegalStateException, context = context)
    } catch (illegalArgumentException: IllegalArgumentException) {
      NanoAIResult.recoverable(
        message = message,
        cause = illegalArgumentException,
        context = context,
      )
    }
  }
}
