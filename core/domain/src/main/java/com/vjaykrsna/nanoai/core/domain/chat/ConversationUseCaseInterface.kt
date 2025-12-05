package com.vjaykrsna.nanoai.core.domain.chat

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.annotations.OneShot
import com.vjaykrsna.nanoai.core.common.annotations.ReactiveStream
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.domain.model.Message
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/** Interface for conversation operations. */
interface ConversationUseCaseInterface {
  @ReactiveStream("Full chat thread catalog") fun getAllThreadsFlow(): Flow<List<ChatThread>>

  @ReactiveStream("Messages for an individual thread")
  fun getMessagesFlow(threadId: UUID): Flow<List<Message>>

  @OneShot("Fetch all chat threads once")
  suspend fun getAllThreads(): NanoAIResult<List<ChatThread>>

  @OneShot("Create new chat thread")
  suspend fun createNewThread(personaId: UUID, title: String? = null): NanoAIResult<UUID>

  @OneShot("Archive chat thread") suspend fun archiveThread(threadId: UUID): NanoAIResult<Unit>

  @OneShot("Delete chat thread") suspend fun deleteThread(threadId: UUID): NanoAIResult<Unit>

  @OneShot("Persist chat message") suspend fun saveMessage(message: Message): NanoAIResult<Unit>

  @OneShot("Fetch persona assigned to thread")
  suspend fun getCurrentPersonaForThread(threadId: UUID): NanoAIResult<UUID?>

  @OneShot("Update thread persona association")
  suspend fun updateThreadPersona(threadId: UUID, personaId: UUID?): NanoAIResult<Unit>

  @OneShot("Update thread metadata")
  suspend fun updateThread(thread: ChatThread): NanoAIResult<Unit>
}
