package com.vjaykrsna.nanoai.core.data.db

import android.os.Build
import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.db.daos.ChatThreadDao
import com.vjaykrsna.nanoai.core.data.db.daos.MessageDao
import com.vjaykrsna.nanoai.core.data.db.entities.ChatThreadEntity
import com.vjaykrsna.nanoai.core.data.db.entities.MessageEntity
import com.vjaykrsna.nanoai.core.model.MessageSource
import com.vjaykrsna.nanoai.core.model.Role
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
@OptIn(ExperimentalCoroutinesApi::class)
class ChatMessageDaoTest {
    private lateinit var database: NanoAIDatabase
    private lateinit var chatThreadDao: ChatThreadDao
    private lateinit var messageDao: MessageDao

    @Before
    fun setup() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    RuntimeEnvironment.getApplication(),
                    NanoAIDatabase::class.java,
                ).allowMainThreadQueries()
                .build()

        chatThreadDao = database.chatThreadDao()
        messageDao = database.messageDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertThread_persistsEntity() =
        runTest {
            val threadId = UUID.randomUUID().toString()
            val now = Clock.System.now()
            val thread = buildThread(threadId, updatedAt = now)

            chatThreadDao.insert(thread)

            val stored = chatThreadDao.getById(threadId)
            assertThat(stored).isNotNull()
            assertThat(stored?.title).isEqualTo(thread.title)
        }

    @Test
    fun deleteThread_cascadesMessages() =
        runTest {
            val threadId = UUID.randomUUID().toString()
            chatThreadDao.insert(buildThread(threadId))

            messageDao.insert(buildMessage(threadId, Clock.System.now()))
            chatThreadDao.delete(chatThreadDao.getById(threadId)!!)

            assertThat(messageDao.getByThreadId(threadId)).isEmpty()
        }

    @Test
    fun getByThreadId_ordersByCreatedAt() =
        runTest {
            val threadId = UUID.randomUUID().toString()
            chatThreadDao.insert(buildThread(threadId))

            val base = Clock.System.now()
            val first = buildMessage(threadId, base.minus(2.seconds), "first")
            val second = buildMessage(threadId, base.minus(1.seconds), "second")
            val third = buildMessage(threadId, base, "third")

            messageDao.insert(third)
            messageDao.insert(first)
            messageDao.insert(second)

            val ordered = messageDao.getByThreadId(threadId)
            assertThat(ordered.map { it.text }).containsExactly("first", "second", "third").inOrder()
        }

    private fun buildThread(
        threadId: String,
        updatedAt: Instant = Clock.System.now(),
    ): ChatThreadEntity =
        ChatThreadEntity(
            threadId = threadId,
            title = "Thread $threadId",
            personaId = null,
            activeModelId = "gemini-proto",
            createdAt = updatedAt.minus(5.seconds),
            updatedAt = updatedAt,
            isArchived = false,
        )

    private fun buildMessage(
        threadId: String,
        createdAt: Instant,
        text: String = "message",
    ): MessageEntity =
        MessageEntity(
            messageId = UUID.randomUUID().toString(),
            threadId = threadId,
            role = Role.USER,
            text = text,
            source = MessageSource.LOCAL_MODEL,
            createdAt = createdAt,
        )
}
