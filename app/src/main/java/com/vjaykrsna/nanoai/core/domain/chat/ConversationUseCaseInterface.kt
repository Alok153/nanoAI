package com.vjaykrsna.nanoai.core.domain.chat

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.domain.model.Message
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/** Interface for conversation operations. */
interface ConversationUseCaseInterface {
  fun getAllThreadsFlow(): Flow<List<ChatThread>>

  fun getMessagesFlow(threadId: UUID): Flow<List<Message>>

  suspend fun createNewThread(personaId: UUID, title: String? = null): NanoAIResult<UUID>

  suspend fun archiveThread(threadId: UUID): NanoAIResult<Unit>

  suspend fun deleteThread(threadId: UUID): NanoAIResult<Unit>

  suspend fun saveMessage(message: Message): NanoAIResult<Unit>

  suspend fun getCurrentPersonaForThread(threadId: UUID): NanoAIResult<UUID?>

  suspend fun updateThreadPersona(threadId: UUID, personaId: UUID?): NanoAIResult<Unit>
}
