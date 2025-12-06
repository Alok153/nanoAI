package com.vjaykrsna.nanoai.core.data.db.mappers

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.db.entities.MessageEntity
import com.vjaykrsna.nanoai.core.domain.model.Message
import com.vjaykrsna.nanoai.core.model.MessageRole
import com.vjaykrsna.nanoai.core.model.MessageSource
import java.util.UUID
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test

class MessageMapperTest {

  @Test
  fun `toDomain maps all fields correctly`() {
    val messageId = UUID.randomUUID().toString()
    val threadId = UUID.randomUUID().toString()
    val createdAt = Instant.parse("2024-01-15T10:30:00Z")

    val entity =
      MessageEntity(
        messageId = messageId,
        threadId = threadId,
        role = MessageRole.USER,
        text = "Hello, how are you?",
        audioUri = "content://audio/123",
        imageUri = "content://image/456",
        source = MessageSource.LOCAL_MODEL,
        latencyMs = 150L,
        createdAt = createdAt,
        errorCode = null,
      )

    val domain = MessageMapper.toDomain(entity)

    assertThat(domain.messageId).isEqualTo(UUID.fromString(messageId))
    assertThat(domain.threadId).isEqualTo(UUID.fromString(threadId))
    assertThat(domain.role).isEqualTo(MessageRole.USER)
    assertThat(domain.text).isEqualTo("Hello, how are you?")
    assertThat(domain.audioUri).isEqualTo("content://audio/123")
    assertThat(domain.imageUri).isEqualTo("content://image/456")
    assertThat(domain.source).isEqualTo(MessageSource.LOCAL_MODEL)
    assertThat(domain.latencyMs).isEqualTo(150L)
    assertThat(domain.createdAt).isEqualTo(createdAt)
    assertThat(domain.errorCode).isNull()
  }

  @Test
  fun `toEntity maps all fields correctly`() {
    val messageId = UUID.randomUUID()
    val threadId = UUID.randomUUID()
    val createdAt = Instant.parse("2024-01-15T10:30:00Z")

    val domain =
      Message(
        messageId = messageId,
        threadId = threadId,
        role = MessageRole.ASSISTANT,
        text = "I'm doing well, thank you!",
        audioUri = null,
        imageUri = null,
        source = MessageSource.CLOUD_API,
        latencyMs = 200L,
        createdAt = createdAt,
        errorCode = "NETWORK_ERROR",
      )

    val entity = MessageMapper.toEntity(domain)

    assertThat(entity.messageId).isEqualTo(messageId.toString())
    assertThat(entity.threadId).isEqualTo(threadId.toString())
    assertThat(entity.role).isEqualTo(MessageRole.ASSISTANT)
    assertThat(entity.text).isEqualTo("I'm doing well, thank you!")
    assertThat(entity.audioUri).isNull()
    assertThat(entity.imageUri).isNull()
    assertThat(entity.source).isEqualTo(MessageSource.CLOUD_API)
    assertThat(entity.latencyMs).isEqualTo(200L)
    assertThat(entity.createdAt).isEqualTo(createdAt)
    assertThat(entity.errorCode).isEqualTo("NETWORK_ERROR")
  }

  @Test
  fun `round trip conversion preserves all data`() {
    val original =
      Message(
        messageId = UUID.randomUUID(),
        threadId = UUID.randomUUID(),
        role = MessageRole.SYSTEM,
        text = "You are a helpful assistant.",
        audioUri = "content://audio/sys",
        imageUri = "content://image/sys",
        source = MessageSource.LOCAL_MODEL,
        latencyMs = 50L,
        createdAt = Instant.parse("2024-02-01T08:00:00Z"),
        errorCode = null,
      )

    val entity = MessageMapper.toEntity(original)
    val roundTrip = MessageMapper.toDomain(entity)

    assertThat(roundTrip).isEqualTo(original)
  }

  @Test
  fun `extension function toDomain works correctly`() {
    val entity =
      MessageEntity(
        messageId = UUID.randomUUID().toString(),
        threadId = UUID.randomUUID().toString(),
        role = MessageRole.USER,
        text = "Test message",
        source = MessageSource.LOCAL_MODEL,
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
      )

    val domain = entity.toDomain()

    assertThat(domain.text).isEqualTo("Test message")
    assertThat(domain.role).isEqualTo(MessageRole.USER)
  }

  @Test
  fun `extension function toEntity works correctly`() {
    val domain =
      Message(
        messageId = UUID.randomUUID(),
        threadId = UUID.randomUUID(),
        role = MessageRole.ASSISTANT,
        text = "Test response",
        source = MessageSource.CLOUD_API,
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
      )

    val entity = domain.toEntity()

    assertThat(entity.text).isEqualTo("Test response")
    assertThat(entity.role).isEqualTo(MessageRole.ASSISTANT)
  }

  @Test
  fun `handles null optional fields correctly`() {
    val domain =
      Message(
        messageId = UUID.randomUUID(),
        threadId = UUID.randomUUID(),
        role = MessageRole.USER,
        text = null,
        audioUri = null,
        imageUri = null,
        source = MessageSource.LOCAL_MODEL,
        latencyMs = null,
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        errorCode = null,
      )

    val entity = MessageMapper.toEntity(domain)

    assertThat(entity.text).isNull()
    assertThat(entity.audioUri).isNull()
    assertThat(entity.imageUri).isNull()
    assertThat(entity.latencyMs).isNull()
    assertThat(entity.errorCode).isNull()

    val roundTrip = MessageMapper.toDomain(entity)
    assertThat(roundTrip).isEqualTo(domain)
  }
}
