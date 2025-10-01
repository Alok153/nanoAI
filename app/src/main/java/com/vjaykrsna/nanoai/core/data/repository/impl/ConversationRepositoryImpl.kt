package com.vjaykrsna.nanoai.core.data.repository.impl

import com.vjaykrsna.nanoai.core.data.db.daos.ChatThreadDao
import com.vjaykrsna.nanoai.core.data.db.daos.MessageDao
import com.vjaykrsna.nanoai.core.data.repository.ConversationRepository
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.domain.model.Message
import com.vjaykrsna.nanoai.core.domain.model.toDomain
import com.vjaykrsna.nanoai.core.domain.model.toEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

/**
 * Implementation of ConversationRepository.
 *
 * Wraps ChatThreadDao and MessageDao, converting between entities and domain models.
 */
@Singleton
class ConversationRepositoryImpl
@Inject
constructor(private val chatThreadDao: ChatThreadDao, private val messageDao: MessageDao) :
        ConversationRepository {

    companion object {
        private const val DEFAULT_MODEL_ID = "local-default"
    }

    override suspend fun getThread(threadId: UUID): ChatThread? {
        return chatThreadDao.getById(threadId.toString())?.toDomain()
    }

    override suspend fun getAllThreads(): List<ChatThread> {
        return chatThreadDao.getAllActive().map { it.toDomain() }
    }

    override suspend fun getArchivedThreads(): List<ChatThread> {
        return chatThreadDao.getArchived().map { it.toDomain() }
    }

    override suspend fun createThread(thread: ChatThread) {
        chatThreadDao.insert(thread.toEntity())
    }

    override suspend fun updateThread(thread: ChatThread) {
        chatThreadDao.update(thread.toEntity())
    }

    override suspend fun archiveThread(threadId: UUID) {
        chatThreadDao.archive(threadId.toString())
    }

    override suspend fun deleteThread(threadId: UUID) {
        val thread = chatThreadDao.getById(threadId.toString())
        if (thread != null) {
            chatThreadDao.delete(thread)
        }
    }

    override suspend fun addMessage(message: Message) {
        messageDao.insert(message.toEntity())
    }

    override suspend fun saveMessage(message: Message) {
        addMessage(message)
    }

    override suspend fun getMessages(threadId: UUID): List<Message> {
        return messageDao.getByThreadId(threadId.toString()).map { it.toDomain() }
    }

    override fun getMessagesFlow(threadId: UUID): Flow<List<Message>> {
        return messageDao.observeByThreadId(threadId.toString()).map { messages ->
            messages.map { it.toDomain() }
        }
    }

    override fun getAllThreadsFlow(): Flow<List<ChatThread>> {
        return chatThreadDao.observeAllActive().map { threads -> threads.map { it.toDomain() } }
    }

    override suspend fun getCurrentPersonaForThread(threadId: UUID): UUID? {
        val entity = chatThreadDao.getById(threadId.toString()) ?: return null
        return entity.personaId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    }

    override suspend fun createNewThread(personaId: UUID, title: String?): UUID {
        val now = Clock.System.now()
        val threadId = UUID.randomUUID()
    val thread =
        ChatThread(
            threadId = threadId,
            title = title,
            personaId = personaId,
            activeModelId = DEFAULT_MODEL_ID,
            createdAt = now,
            updatedAt = now,
            isArchived = false
        )
        chatThreadDao.insert(thread.toEntity())
        return threadId
    }

    override suspend fun updateThreadPersona(threadId: UUID, personaId: UUID?) {
        chatThreadDao.updatePersona(
                threadId = threadId.toString(),
                personaId = personaId?.toString(),
                updatedAt = Clock.System.now()
        )
    }
}
