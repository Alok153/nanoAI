package com.vjaykrsna.nanoai.core.data.repository.impl

import android.os.Build
import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.db.NanoAIDatabase
import com.vjaykrsna.nanoai.core.data.db.daos.ChatThreadDao
import com.vjaykrsna.nanoai.core.data.db.daos.MessageDao
import com.vjaykrsna.nanoai.core.data.repository.ConversationRepository
import com.vjaykrsna.nanoai.core.model.MessageSource
import com.vjaykrsna.nanoai.core.model.Role
import com.vjaykrsna.nanoai.testing.DomainTestBuilders
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Tests for [ConversationRepositoryImpl].
 *
 * Validates Room DAO operations with thread/message CRUD using an in-memory database.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
@OptIn(ExperimentalCoroutinesApi::class)
class ConversationRepositoryImplTest {

  private lateinit var database: NanoAIDatabase
  private lateinit var chatThreadDao: ChatThreadDao
  private lateinit var messageDao: MessageDao
  private lateinit var repository: ConversationRepository

  @Before
  fun setup() {
    database =
      Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), NanoAIDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    chatThreadDao = database.chatThreadDao()
    messageDao = database.messageDao()
    repository = ConversationRepositoryImpl(chatThreadDao, messageDao)
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun `createThread persists thread to database`() = runTest {
    val thread = DomainTestBuilders.buildChatThread()

    repository.createThread(thread)

    val retrieved = repository.getThread(thread.threadId)
    assertThat(retrieved).isNotNull()
    assertThat(retrieved?.threadId).isEqualTo(thread.threadId)
    assertThat(retrieved?.title).isEqualTo(thread.title)
  }

  @Test
  fun `createNewThread generates new thread with persona`() = runTest {
    val personaId = UUID.randomUUID()
    val title = "Test Thread"

    val threadId = repository.createNewThread(personaId, title)

    val retrieved = repository.getThread(threadId)
    assertThat(retrieved).isNotNull()
    assertThat(retrieved?.personaId).isEqualTo(personaId)
    assertThat(retrieved?.title).isEqualTo(title)
  }

  @Test
  fun `getAllThreads returns only non-archived threads`() = runTest {
    val activeThread = DomainTestBuilders.buildChatThread(isArchived = false)
    val archivedThread = DomainTestBuilders.buildChatThread(isArchived = true)

    repository.createThread(activeThread)
    repository.createThread(archivedThread)

    val allThreads = repository.getAllThreads()
    assertThat(allThreads).hasSize(1)
    assertThat(allThreads.first().threadId).isEqualTo(activeThread.threadId)
  }

  @Test
  fun `getArchivedThreads returns only archived threads`() = runTest {
    val activeThread = DomainTestBuilders.buildChatThread(isArchived = false)
    val archivedThread = DomainTestBuilders.buildChatThread(isArchived = true)

    repository.createThread(activeThread)
    repository.createThread(archivedThread)

    val archived = repository.getArchivedThreads()
    assertThat(archived).hasSize(1)
    assertThat(archived.first().threadId).isEqualTo(archivedThread.threadId)
  }

  @Test
  fun `updateThread modifies existing thread`() = runTest {
    val thread = DomainTestBuilders.buildChatThread(title = "Original")
    repository.createThread(thread)

    val updated = thread.copy(title = "Updated")
    repository.updateThread(updated)

    val retrieved = repository.getThread(thread.threadId)
    assertThat(retrieved?.title).isEqualTo("Updated")
  }

  @Test
  fun `archiveThread marks thread as archived`() = runTest {
    val thread = DomainTestBuilders.buildChatThread(isArchived = false)
    repository.createThread(thread)

    repository.archiveThread(thread.threadId)

    val retrieved = repository.getThread(thread.threadId)
    assertThat(retrieved?.isArchived).isTrue()
  }

  @Test
  fun `deleteThread removes thread from database`() = runTest {
    val thread = DomainTestBuilders.buildChatThread()
    repository.createThread(thread)

    repository.deleteThread(thread.threadId)

    val retrieved = repository.getThread(thread.threadId)
    assertThat(retrieved).isNull()
  }

  @Test
  fun `deleteThread cascades to messages`() = runTest {
    val thread = DomainTestBuilders.buildChatThread()
    repository.createThread(thread)

    val message = DomainTestBuilders.buildUserMessage(threadId = thread.threadId)
    repository.addMessage(message)

    repository.deleteThread(thread.threadId)

    val messages = repository.getMessages(thread.threadId)
    assertThat(messages).isEmpty()
  }

  @Test
  fun `addMessage persists message to database`() = runTest {
    val thread = DomainTestBuilders.buildChatThread()
    repository.createThread(thread)

    val message = DomainTestBuilders.buildUserMessage(threadId = thread.threadId, text = "Hello")
    repository.addMessage(message)

    val retrieved = repository.getMessages(thread.threadId)
    assertThat(retrieved).hasSize(1)
    assertThat(retrieved.first().text).isEqualTo("Hello")
  }

  @Test
  fun `getMessages returns messages ordered by creation time`() = runTest {
    val thread = DomainTestBuilders.buildChatThread()
    repository.createThread(thread)

    val now = Clock.System.now()
    val message1 =
      DomainTestBuilders.buildUserMessage(threadId = thread.threadId, text = "First")
        .copy(createdAt = now)
    val message2 =
      DomainTestBuilders.buildUserMessage(threadId = thread.threadId, text = "Second")
        .copy(createdAt = now.plus(kotlin.time.Duration.parse("1s")))
    val message3 =
      DomainTestBuilders.buildUserMessage(threadId = thread.threadId, text = "Third")
        .copy(createdAt = now.plus(kotlin.time.Duration.parse("2s")))

    repository.addMessage(message3)
    repository.addMessage(message1)
    repository.addMessage(message2)

    val retrieved = repository.getMessages(thread.threadId)
    assertThat(retrieved.map { it.text }).containsExactly("First", "Second", "Third").inOrder()
  }

  @Test
  fun `getMessagesFlow emits updates reactively`() = runTest {
    val thread = DomainTestBuilders.buildChatThread()
    repository.createThread(thread)

    val flow = repository.getMessagesFlow(thread.threadId)

    // Initially empty
    assertThat(flow.first()).isEmpty()

    // Add message
    val message = DomainTestBuilders.buildUserMessage(threadId = thread.threadId)
    repository.addMessage(message)

    // Flow should emit new list
    assertThat(flow.first()).hasSize(1)
  }

  @Test
  fun `getAllThreadsFlow emits updates reactively`() = runTest {
    val flow = repository.getAllThreadsFlow()

    // Initially empty
    assertThat(flow.first()).isEmpty()

    // Add thread
    val thread = DomainTestBuilders.buildChatThread()
    repository.createThread(thread)

    // Flow should emit new list
    assertThat(flow.first()).hasSize(1)
  }

  @Test
  fun `getCurrentPersonaForThread returns thread's persona`() = runTest {
    val personaId = UUID.randomUUID()
    val thread = DomainTestBuilders.buildChatThread(personaId = personaId)
    repository.createThread(thread)

    val retrieved = repository.getCurrentPersonaForThread(thread.threadId)
    assertThat(retrieved).isEqualTo(personaId)
  }

  @Test
  fun `getCurrentPersonaForThread returns null for thread without persona`() = runTest {
    val thread = DomainTestBuilders.buildChatThread(personaId = null)
    repository.createThread(thread)

    val retrieved = repository.getCurrentPersonaForThread(thread.threadId)
    assertThat(retrieved).isNull()
  }

  @Test
  fun `updateThreadPersona changes thread's persona`() = runTest {
    val initialPersonaId = UUID.randomUUID()
    val newPersonaId = UUID.randomUUID()
    val thread = DomainTestBuilders.buildChatThread(personaId = initialPersonaId)
    repository.createThread(thread)

    repository.updateThreadPersona(thread.threadId, newPersonaId)

    val retrieved = repository.getCurrentPersonaForThread(thread.threadId)
    assertThat(retrieved).isEqualTo(newPersonaId)
  }

  @Test
  fun `updateThreadPersona clears persona when null`() = runTest {
    val personaId = UUID.randomUUID()
    val thread = DomainTestBuilders.buildChatThread(personaId = personaId)
    repository.createThread(thread)

    repository.updateThreadPersona(thread.threadId, null)

    val retrieved = repository.getCurrentPersonaForThread(thread.threadId)
    assertThat(retrieved).isNull()
  }

  @Test
  fun `saveMessage is alias for addMessage`() = runTest {
    val thread = DomainTestBuilders.buildChatThread()
    repository.createThread(thread)

    val message = DomainTestBuilders.buildUserMessage(threadId = thread.threadId)
    repository.saveMessage(message)

    val retrieved = repository.getMessages(thread.threadId)
    assertThat(retrieved).hasSize(1)
  }

  @Test
  fun `addMessage supports assistant messages with latency`() = runTest {
    val thread = DomainTestBuilders.buildChatThread()
    repository.createThread(thread)

    val message =
      DomainTestBuilders.buildAssistantMessage(threadId = thread.threadId, text = "Response")
        .copy(latencyMs = 1500L)
    repository.addMessage(message)

    val retrieved = repository.getMessages(thread.threadId)
    assertThat(retrieved.first().role).isEqualTo(Role.ASSISTANT)
    assertThat(retrieved.first().latencyMs).isEqualTo(1500L)
  }

  @Test
  fun `addMessage updates thread timestamp`() = runTest {
    val thread = DomainTestBuilders.buildChatThread()
    repository.createThread(thread)
    val newTimestamp = Clock.System.now().plus(kotlin.time.Duration.parse("5s"))
    val message =
      DomainTestBuilders.buildUserMessage(threadId = thread.threadId).copy(createdAt = newTimestamp)

    repository.addMessage(message)

    val updatedThread = repository.getThread(thread.threadId)
    assertThat(updatedThread?.updatedAt).isEqualTo(newTimestamp)
  }

  @Test
  fun `addMessage supports error messages`() = runTest {
    val thread = DomainTestBuilders.buildChatThread()
    repository.createThread(thread)

    val message =
      DomainTestBuilders.buildAssistantMessage(threadId = thread.threadId, text = null)
        .copy(errorCode = "INFERENCE_FAILED")
    repository.addMessage(message)

    val retrieved = repository.getMessages(thread.threadId)
    assertThat(retrieved.first().errorCode).isEqualTo("INFERENCE_FAILED")
    assertThat(retrieved.first().text).isNull()
  }

  @Test
  fun `getMessages filters by thread correctly`() = runTest {
    val thread1 = DomainTestBuilders.buildChatThread()
    val thread2 = DomainTestBuilders.buildChatThread()
    repository.createThread(thread1)
    repository.createThread(thread2)

    val message1 =
      DomainTestBuilders.buildUserMessage(threadId = thread1.threadId, text = "Thread 1")
    val message2 =
      DomainTestBuilders.buildUserMessage(threadId = thread2.threadId, text = "Thread 2")
    repository.addMessage(message1)
    repository.addMessage(message2)

    val retrieved1 = repository.getMessages(thread1.threadId)
    val retrieved2 = repository.getMessages(thread2.threadId)

    assertThat(retrieved1).hasSize(1)
    assertThat(retrieved2).hasSize(1)
    assertThat(retrieved1.first().text).isEqualTo("Thread 1")
    assertThat(retrieved2.first().text).isEqualTo("Thread 2")
  }

  @Test
  fun `thread with null persona can be created and retrieved`() = runTest {
    val thread = DomainTestBuilders.buildChatThread(personaId = null)
    repository.createThread(thread)

    val retrieved = repository.getThread(thread.threadId)
    assertThat(retrieved?.personaId).isNull()
  }

  @Test
  fun `message with different sources are stored correctly`() = runTest {
    val thread = DomainTestBuilders.buildChatThread()
    repository.createThread(thread)

    val localMessage =
      DomainTestBuilders.buildAssistantMessage(threadId = thread.threadId, text = "Local")
        .copy(source = MessageSource.LOCAL_MODEL)
    val cloudMessage =
      DomainTestBuilders.buildAssistantMessage(threadId = thread.threadId, text = "Cloud")
        .copy(source = MessageSource.CLOUD_API)

    repository.addMessage(localMessage)
    repository.addMessage(cloudMessage)

    val retrieved = repository.getMessages(thread.threadId)
    assertThat(retrieved).hasSize(2)
    assertThat(retrieved.map { it.source })
      .containsExactly(MessageSource.LOCAL_MODEL, MessageSource.CLOUD_API)
  }
}
