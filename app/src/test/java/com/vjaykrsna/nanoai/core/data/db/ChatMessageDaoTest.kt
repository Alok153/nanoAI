package com.vjaykrsna.nanoai.core.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.UUID

/**
 * Room DAO integration test for ChatThread and Message entities.
 * Tests cascading deletes, ordering indexes, and persona log joins.
 *
 * TDD: This test is written BEFORE entities/DAOs are implemented.
 * Expected to FAIL with compilation errors until:
 * - NanoAIDatabase is created
 * - ChatThreadEntity, ChatThreadDao are defined
 * - MessageEntity, MessageDao are defined
 * - PersonaSwitchLogEntity, PersonaSwitchLogDao are defined
 */
@RunWith(AndroidJUnit4::class)
class ChatMessageDaoTest {

    private lateinit var database: NanoAIDatabase
    private lateinit var chatThreadDao: ChatThreadDao
    private lateinit var messageDao: MessageDao
    private lateinit var personaSwitchLogDao: PersonaSwitchLogDao

    @Before
    fun setup() {
        // Create an in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NanoAIDatabase::class.java
        ).allowMainThreadQueries().build()

        chatThreadDao = database.chatThreadDao()
        messageDao = database.messageDao()
        personaSwitchLogDao = database.personaSwitchLogDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertChatThread_shouldPersistAndRetrieve() = runTest {
        // Arrange
        val threadId = UUID.randomUUID()
        val thread = ChatThreadEntity(
            threadId = threadId,
            title = "Test Conversation",
            personaId = null,
            activeModelId = "gemini-2.0-flash-lite",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            isArchived = false
        )

        // Act
        chatThreadDao.insert(thread)
        val retrieved = chatThreadDao.getById(threadId)

        // Assert
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.title).isEqualTo("Test Conversation")
        assertThat(retrieved?.activeModelId).isEqualTo("gemini-2.0-flash-lite")
        assertThat(retrieved?.isArchived).isFalse()
    }

    @Test
    fun deleteChatThread_shouldCascadeDeleteMessages() = runTest {
        // Arrange: Create thread with messages
        val threadId = UUID.randomUUID()
        val thread = ChatThreadEntity(
            threadId = threadId,
            title = "Test Thread",
            personaId = null,
            activeModelId = "test-model",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            isArchived = false
        )
        chatThreadDao.insert(thread)

        val message1 = MessageEntity(
            messageId = UUID.randomUUID(),
            threadId = threadId,
            role = MessageRole.USER,
            text = "Hello",
            audioUri = null,
            imageUri = null,
            source = MessageSource.LOCAL_MODEL,
            latencyMs = null,
            createdAt = Instant.now(),
            errorCode = null
        )
        val message2 = MessageEntity(
            messageId = UUID.randomUUID(),
            threadId = threadId,
            role = MessageRole.ASSISTANT,
            text = "Hi there!",
            audioUri = null,
            imageUri = null,
            source = MessageSource.LOCAL_MODEL,
            latencyMs = 1500,
            createdAt = Instant.now(),
            errorCode = null
        )
        messageDao.insert(message1)
        messageDao.insert(message2)

        // Act: Delete thread
        chatThreadDao.delete(thread)

        // Assert: Messages should be cascade-deleted
        val remainingMessages = messageDao.getMessagesByThreadId(threadId).first()
        assertThat(remainingMessages).isEmpty()
    }

    @Test
    fun getMessagesByThreadId_shouldReturnOrderedByCreatedAt() = runTest {
        // Arrange: Create thread with messages at different times
        val threadId = UUID.randomUUID()
        val thread = ChatThreadEntity(
            threadId = threadId,
            title = "Ordered Test",
            personaId = null,
            activeModelId = "test-model",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            isArchived = false
        )
        chatThreadDao.insert(thread)

        val now = Instant.now()
        val message1 = MessageEntity(
            messageId = UUID.randomUUID(),
            threadId = threadId,
            role = MessageRole.USER,
            text = "First message",
            audioUri = null,
            imageUri = null,
            source = MessageSource.LOCAL_MODEL,
            latencyMs = null,
            createdAt = now.minusSeconds(60),
            errorCode = null
        )
        val message2 = MessageEntity(
            messageId = UUID.randomUUID(),
            threadId = threadId,
            role = MessageRole.ASSISTANT,
            text = "Second message",
            audioUri = null,
            imageUri = null,
            source = MessageSource.LOCAL_MODEL,
            latencyMs = 1000,
            createdAt = now.minusSeconds(30),
            errorCode = null
        )
        val message3 = MessageEntity(
            messageId = UUID.randomUUID(),
            threadId = threadId,
            role = MessageRole.USER,
            text = "Third message",
            audioUri = null,
            imageUri = null,
            source = MessageSource.CLOUD_API,
            latencyMs = null,
            createdAt = now,
            errorCode = null
        )

        // Insert in random order
        messageDao.insert(message2)
        messageDao.insert(message3)
        messageDao.insert(message1)

        // Act
        val messages = messageDao.getMessagesByThreadId(threadId).first()

        // Assert: Should be ordered by createdAt ASC
        assertThat(messages).hasSize(3)
        assertThat(messages[0].text).isEqualTo("First message")
        assertThat(messages[1].text).isEqualTo("Second message")
        assertThat(messages[2].text).isEqualTo("Third message")
    }

    @Test
    fun messageEntity_shouldStoreRoleEnum() = runTest {
        // Arrange
        val threadId = UUID.randomUUID()
        val thread = ChatThreadEntity(
            threadId = threadId,
            title = "Role Test",
            personaId = null,
            activeModelId = "test-model",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            isArchived = false
        )
        chatThreadDao.insert(thread)

        val userMessage = MessageEntity(
            messageId = UUID.randomUUID(),
            threadId = threadId,
            role = MessageRole.USER,
            text = "User message",
            audioUri = null,
            imageUri = null,
            source = MessageSource.LOCAL_MODEL,
            latencyMs = null,
            createdAt = Instant.now(),
            errorCode = null
        )
        val assistantMessage = MessageEntity(
            messageId = UUID.randomUUID(),
            threadId = threadId,
            role = MessageRole.ASSISTANT,
            text = "Assistant message",
            audioUri = null,
            imageUri = null,
            source = MessageSource.LOCAL_MODEL,
            latencyMs = 1500,
            createdAt = Instant.now(),
            errorCode = null
        )
        val systemMessage = MessageEntity(
            messageId = UUID.randomUUID(),
            threadId = threadId,
            role = MessageRole.SYSTEM,
            text = "System message",
            audioUri = null,
            imageUri = null,
            source = MessageSource.LOCAL_MODEL,
            latencyMs = null,
            createdAt = Instant.now(),
            errorCode = null
        )

        // Act
        messageDao.insert(userMessage)
        messageDao.insert(assistantMessage)
        messageDao.insert(systemMessage)

        val messages = messageDao.getMessagesByThreadId(threadId).first()

        // Assert: Roles are preserved
        assertThat(messages).hasSize(3)
        assertThat(messages.map { it.role }).containsExactly(
            MessageRole.USER,
            MessageRole.ASSISTANT,
            MessageRole.SYSTEM
        )
    }

    @Test
    fun messageEntity_shouldStoreSourceEnum() = runTest {
        // Arrange
        val threadId = UUID.randomUUID()
        val thread = ChatThreadEntity(
            threadId = threadId,
            title = "Source Test",
            personaId = null,
            activeModelId = "test-model",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            isArchived = false
        )
        chatThreadDao.insert(thread)

        val localMessage = MessageEntity(
            messageId = UUID.randomUUID(),
            threadId = threadId,
            role = MessageRole.ASSISTANT,
            text = "Local response",
            audioUri = null,
            imageUri = null,
            source = MessageSource.LOCAL_MODEL,
            latencyMs = 1200,
            createdAt = Instant.now(),
            errorCode = null
        )
        val cloudMessage = MessageEntity(
            messageId = UUID.randomUUID(),
            threadId = threadId,
            role = MessageRole.ASSISTANT,
            text = "Cloud response",
            audioUri = null,
            imageUri = null,
            source = MessageSource.CLOUD_API,
            latencyMs = 3500,
            createdAt = Instant.now(),
            errorCode = null
        )

        // Act
        messageDao.insert(localMessage)
        messageDao.insert(cloudMessage)

        val messages = messageDao.getMessagesByThreadId(threadId).first()

        // Assert: Sources are preserved
        assertThat(messages).hasSize(2)
        assertThat(messages[0].source).isEqualTo(MessageSource.LOCAL_MODEL)
        assertThat(messages[1].source).isEqualTo(MessageSource.CLOUD_API)
    }

    @Test
    fun messageEntity_shouldStoreLatencyMetrics() = runTest {
        // Arrange
        val threadId = UUID.randomUUID()
        val thread = ChatThreadEntity(
            threadId = threadId,
            title = "Latency Test",
            personaId = null,
            activeModelId = "test-model",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            isArchived = false
        )
        chatThreadDao.insert(thread)

        val message = MessageEntity(
            messageId = UUID.randomUUID(),
            threadId = threadId,
            role = MessageRole.ASSISTANT,
            text = "Response",
            audioUri = null,
            imageUri = null,
            source = MessageSource.LOCAL_MODEL,
            latencyMs = 2345,
            createdAt = Instant.now(),
            errorCode = null
        )

        // Act
        messageDao.insert(message)
        val retrieved = messageDao.getMessagesByThreadId(threadId).first().first()

        // Assert: Latency is stored
        assertThat(retrieved.latencyMs).isEqualTo(2345)
    }

    @Test
    fun messageEntity_shouldStoreErrorCode() = runTest {
        // Arrange
        val threadId = UUID.randomUUID()
        val thread = ChatThreadEntity(
            threadId = threadId,
            title = "Error Test",
            personaId = null,
            activeModelId = "test-model",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            isArchived = false
        )
        chatThreadDao.insert(thread)

        val errorMessage = MessageEntity(
            messageId = UUID.randomUUID(),
            threadId = threadId,
            role = MessageRole.ASSISTANT,
            text = null,
            audioUri = null,
            imageUri = null,
            source = MessageSource.CLOUD_API,
            latencyMs = null,
            createdAt = Instant.now(),
            errorCode = "RATE_LIMIT_EXCEEDED"
        )

        // Act
        messageDao.insert(errorMessage)
        val retrieved = messageDao.getMessagesByThreadId(threadId).first().first()

        // Assert: Error code is stored
        assertThat(retrieved.errorCode).isEqualTo("RATE_LIMIT_EXCEEDED")
        assertThat(retrieved.text).isNull()
    }

    @Test
    fun personaSwitchLog_shouldTrackSwitches() = runTest {
        // Arrange
        val threadId = UUID.randomUUID()
        val thread = ChatThreadEntity(
            threadId = threadId,
            title = "Persona Switch Test",
            personaId = null,
            activeModelId = "test-model",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            isArchived = false
        )
        chatThreadDao.insert(thread)

        val persona1Id = UUID.randomUUID()
        val persona2Id = UUID.randomUUID()

        val switchLog = PersonaSwitchLogEntity(
            logId = UUID.randomUUID(),
            threadId = threadId,
            previousPersonaId = persona1Id,
            newPersonaId = persona2Id,
            actionTaken = PersonaSwitchAction.CONTINUE_THREAD,
            createdAt = Instant.now()
        )

        // Act
        personaSwitchLogDao.insert(switchLog)
        val logs = personaSwitchLogDao.getLogsByThreadId(threadId).first()

        // Assert
        assertThat(logs).hasSize(1)
        assertThat(logs[0].previousPersonaId).isEqualTo(persona1Id)
        assertThat(logs[0].newPersonaId).isEqualTo(persona2Id)
        assertThat(logs[0].actionTaken).isEqualTo(PersonaSwitchAction.CONTINUE_THREAD)
    }

    @Test
    fun personaSwitchLog_shouldSupportNullPreviousPersona() = runTest {
        // Arrange: First persona assignment
        val threadId = UUID.randomUUID()
        val thread = ChatThreadEntity(
            threadId = threadId,
            title = "First Persona Test",
            personaId = null,
            activeModelId = "test-model",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            isArchived = false
        )
        chatThreadDao.insert(thread)

        val newPersonaId = UUID.randomUUID()
        val switchLog = PersonaSwitchLogEntity(
            logId = UUID.randomUUID(),
            threadId = threadId,
            previousPersonaId = null, // No previous persona
            newPersonaId = newPersonaId,
            actionTaken = PersonaSwitchAction.START_NEW_THREAD,
            createdAt = Instant.now()
        )

        // Act
        personaSwitchLogDao.insert(switchLog)
        val logs = personaSwitchLogDao.getLogsByThreadId(threadId).first()

        // Assert
        assertThat(logs).hasSize(1)
        assertThat(logs[0].previousPersonaId).isNull()
        assertThat(logs[0].newPersonaId).isEqualTo(newPersonaId)
    }

    @Test
    fun chatThreadDao_shouldFilterArchivedThreads() = runTest {
        // Arrange
        val activeThread = ChatThreadEntity(
            threadId = UUID.randomUUID(),
            title = "Active Thread",
            personaId = null,
            activeModelId = "test-model",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            isArchived = false
        )
        val archivedThread = ChatThreadEntity(
            threadId = UUID.randomUUID(),
            title = "Archived Thread",
            personaId = null,
            activeModelId = "test-model",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            isArchived = true
        )

        chatThreadDao.insert(activeThread)
        chatThreadDao.insert(archivedThread)

        // Act
        val allThreads = chatThreadDao.getAllThreads().first()
        val activeThreads = chatThreadDao.getActiveThreads().first()

        // Assert
        assertThat(allThreads).hasSize(2)
        assertThat(activeThreads).hasSize(1)
        assertThat(activeThreads[0].title).isEqualTo("Active Thread")
    }
}
