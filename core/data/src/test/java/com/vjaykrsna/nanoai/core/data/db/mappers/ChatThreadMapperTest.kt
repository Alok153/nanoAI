package com.vjaykrsna.nanoai.core.data.db.mappers

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.db.entities.ChatThreadEntity
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import java.util.UUID
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test

class ChatThreadMapperTest {

  @Test
  fun `toDomain maps all fields correctly`() {
    val threadId = UUID.randomUUID().toString()
    val personaId = UUID.randomUUID().toString()
    val createdAt = Instant.parse("2024-01-15T10:30:00Z")
    val updatedAt = Instant.parse("2024-01-15T11:00:00Z")

    val entity =
      ChatThreadEntity(
        threadId = threadId,
        title = "My Conversation",
        personaId = personaId,
        activeModelId = "gemma-2b-it",
        createdAt = createdAt,
        updatedAt = updatedAt,
        isArchived = false,
      )

    val domain = ChatThreadMapper.toDomain(entity)

    assertThat(domain.threadId).isEqualTo(UUID.fromString(threadId))
    assertThat(domain.title).isEqualTo("My Conversation")
    assertThat(domain.personaId).isEqualTo(UUID.fromString(personaId))
    assertThat(domain.activeModelId).isEqualTo("gemma-2b-it")
    assertThat(domain.createdAt).isEqualTo(createdAt)
    assertThat(domain.updatedAt).isEqualTo(updatedAt)
    assertThat(domain.isArchived).isFalse()
  }

  @Test
  fun `toEntity maps all fields correctly`() {
    val threadId = UUID.randomUUID()
    val personaId = UUID.randomUUID()
    val createdAt = Instant.parse("2024-01-15T10:30:00Z")
    val updatedAt = Instant.parse("2024-01-15T11:00:00Z")

    val domain =
      ChatThread(
        threadId = threadId,
        title = "Test Thread",
        personaId = personaId,
        activeModelId = "phi-2",
        createdAt = createdAt,
        updatedAt = updatedAt,
        isArchived = true,
      )

    val entity = ChatThreadMapper.toEntity(domain)

    assertThat(entity.threadId).isEqualTo(threadId.toString())
    assertThat(entity.title).isEqualTo("Test Thread")
    assertThat(entity.personaId).isEqualTo(personaId.toString())
    assertThat(entity.activeModelId).isEqualTo("phi-2")
    assertThat(entity.createdAt).isEqualTo(createdAt)
    assertThat(entity.updatedAt).isEqualTo(updatedAt)
    assertThat(entity.isArchived).isTrue()
  }

  @Test
  fun `round trip conversion preserves all data`() {
    val original =
      ChatThread(
        threadId = UUID.randomUUID(),
        title = "Round Trip Test",
        personaId = UUID.randomUUID(),
        activeModelId = "llama-3b",
        createdAt = Instant.parse("2024-02-01T08:00:00Z"),
        updatedAt = Instant.parse("2024-02-01T09:00:00Z"),
        isArchived = false,
      )

    val entity = ChatThreadMapper.toEntity(original)
    val roundTrip = ChatThreadMapper.toDomain(entity)

    assertThat(roundTrip).isEqualTo(original)
  }

  @Test
  fun `handles null title correctly`() {
    val entity =
      ChatThreadEntity(
        threadId = UUID.randomUUID().toString(),
        title = null,
        personaId = null,
        activeModelId = "default-model",
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
        isArchived = false,
      )

    val domain = ChatThreadMapper.toDomain(entity)

    assertThat(domain.title).isNull()
  }

  @Test
  fun `handles null personaId correctly`() {
    val domain =
      ChatThread(
        threadId = UUID.randomUUID(),
        title = "No Persona Thread",
        personaId = null,
        activeModelId = "model-id",
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
        isArchived = false,
      )

    val entity = ChatThreadMapper.toEntity(domain)

    assertThat(entity.personaId).isNull()

    val roundTrip = ChatThreadMapper.toDomain(entity)
    assertThat(roundTrip.personaId).isNull()
  }

  @Test
  fun `extension function toDomain works correctly`() {
    val entity =
      ChatThreadEntity(
        threadId = UUID.randomUUID().toString(),
        title = "Extension Test",
        personaId = null,
        activeModelId = "test-model",
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
      )

    val domain = entity.toDomain()

    assertThat(domain.title).isEqualTo("Extension Test")
  }

  @Test
  fun `extension function toEntity works correctly`() {
    val domain =
      ChatThread(
        threadId = UUID.randomUUID(),
        title = "Extension Entity Test",
        personaId = null,
        activeModelId = "test-model",
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
      )

    val entity = domain.toEntity()

    assertThat(entity.title).isEqualTo("Extension Entity Test")
  }

  @Test
  fun `isArchived defaults to false`() {
    val entity =
      ChatThreadEntity(
        threadId = UUID.randomUUID().toString(),
        title = "Default Archive Test",
        personaId = null,
        activeModelId = "model",
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
      )

    val domain = ChatThreadMapper.toDomain(entity)

    assertThat(domain.isArchived).isFalse()
  }
}
