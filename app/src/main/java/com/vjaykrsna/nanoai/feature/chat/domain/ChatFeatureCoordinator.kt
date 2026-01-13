package com.vjaykrsna.nanoai.feature.chat.domain

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.chat.ConversationUseCase
import com.vjaykrsna.nanoai.core.domain.chat.PromptAttachments
import com.vjaykrsna.nanoai.core.domain.chat.SendPromptUseCase
import com.vjaykrsna.nanoai.core.domain.chat.SwitchPersonaUseCase
import com.vjaykrsna.nanoai.core.domain.library.Model
import com.vjaykrsna.nanoai.core.domain.library.ModelCatalogUseCase
import com.vjaykrsna.nanoai.core.domain.library.toModel
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.domain.model.Message
import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import com.vjaykrsna.nanoai.core.domain.usecase.GetDefaultPersonaUseCase
import com.vjaykrsna.nanoai.core.domain.usecase.ObservePersonasUseCase
import com.vjaykrsna.nanoai.core.model.PersonaSwitchAction
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Aggregates chat-specific domain operations behind a feature-friendly facade. */
@Suppress("TooManyFunctions")
interface ChatFeatureCoordinator {
  fun observeThreads(): Flow<List<ChatThread>>

  fun observeMessages(threadId: UUID): Flow<List<Message>>

  fun observePersonas(): Flow<List<PersonaProfile>>

  fun observeInstalledModels(): Flow<List<Model>>

  suspend fun getDefaultPersona(): PersonaProfile?

  suspend fun createThread(personaId: UUID, title: String? = null): NanoAIResult<UUID>

  suspend fun archiveThread(threadId: UUID): NanoAIResult<Unit>

  suspend fun deleteThread(threadId: UUID): NanoAIResult<Unit>

  suspend fun updateThread(thread: ChatThread): NanoAIResult<Unit>

  suspend fun saveMessage(message: Message): NanoAIResult<Unit>

  suspend fun sendPrompt(
    threadId: UUID,
    text: String,
    personaId: UUID,
    attachments: PromptAttachments,
  ): NanoAIResult<Unit>

  suspend fun switchPersona(
    threadId: UUID,
    personaId: UUID,
    action: PersonaSwitchAction,
  ): NanoAIResult<UUID>
}

@Singleton
class DefaultChatFeatureCoordinator
@Inject
constructor(
  private val sendPromptUseCase: SendPromptUseCase,
  private val switchPersonaUseCase: SwitchPersonaUseCase,
  private val conversationUseCase: ConversationUseCase,
  private val observePersonasUseCase: ObservePersonasUseCase,
  private val getDefaultPersonaUseCase: GetDefaultPersonaUseCase,
  private val modelCatalogUseCase: ModelCatalogUseCase,
) : ChatFeatureCoordinator {

  override fun observeThreads(): Flow<List<ChatThread>> = conversationUseCase.getAllThreadsFlow()

  override fun observeMessages(threadId: UUID): Flow<List<Message>> =
    conversationUseCase.getMessagesFlow(threadId)

  override fun observePersonas(): Flow<List<PersonaProfile>> = observePersonasUseCase()

  override fun observeInstalledModels(): Flow<List<Model>> =
    modelCatalogUseCase.observeInstalledModels().map { packages -> packages.map { it.toModel() } }

  override suspend fun getDefaultPersona(): PersonaProfile? = getDefaultPersonaUseCase()

  override suspend fun createThread(personaId: UUID, title: String?): NanoAIResult<UUID> =
    conversationUseCase.createNewThread(personaId, title)

  override suspend fun archiveThread(threadId: UUID): NanoAIResult<Unit> =
    conversationUseCase.archiveThread(threadId)

  override suspend fun deleteThread(threadId: UUID): NanoAIResult<Unit> =
    conversationUseCase.deleteThread(threadId)

  override suspend fun updateThread(thread: ChatThread): NanoAIResult<Unit> =
    conversationUseCase.updateThread(thread)

  override suspend fun saveMessage(message: Message): NanoAIResult<Unit> =
    conversationUseCase.saveMessage(message)

  override suspend fun sendPrompt(
    threadId: UUID,
    text: String,
    personaId: UUID,
    attachments: PromptAttachments,
  ): NanoAIResult<Unit> = sendPromptUseCase(threadId, text, personaId, attachments)

  override suspend fun switchPersona(
    threadId: UUID,
    personaId: UUID,
    action: PersonaSwitchAction,
  ): NanoAIResult<UUID> = switchPersonaUseCase(threadId, personaId, action)
}
