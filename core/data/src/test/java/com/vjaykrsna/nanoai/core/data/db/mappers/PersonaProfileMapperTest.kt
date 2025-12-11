package com.vjaykrsna.nanoai.core.data.db.mappers

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.db.entities.PersonaProfileEntity
import com.vjaykrsna.nanoai.core.domain.model.PersonaProfile
import java.util.UUID
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test

class PersonaProfileMapperTest {

  @Test
  fun `toDomain maps all fields correctly`() {
    val personaId = UUID.randomUUID().toString()
    val createdAt = Instant.parse("2024-01-15T10:30:00Z")
    val updatedAt = Instant.parse("2024-01-15T11:00:00Z")

    val entity =
      PersonaProfileEntity(
        personaId = personaId,
        name = "Creative Writer",
        description = "A creative writing assistant",
        systemPrompt = "You are a creative writer. Help with stories and poems.",
        defaultModelPreference = "gemma-7b-it",
        temperature = 0.8f,
        topP = 0.95f,
        defaultVoice = "en-US-Wavenet-F",
        defaultImageStyle = "watercolor",
        createdAt = createdAt,
        updatedAt = updatedAt,
      )

    val domain = PersonaProfileMapper.toDomain(entity)

    assertThat(domain.personaId).isEqualTo(UUID.fromString(personaId))
    assertThat(domain.name).isEqualTo("Creative Writer")
    assertThat(domain.description).isEqualTo("A creative writing assistant")
    assertThat(domain.systemPrompt)
      .isEqualTo("You are a creative writer. Help with stories and poems.")
    assertThat(domain.defaultModelPreference).isEqualTo("gemma-7b-it")
    assertThat(domain.temperature).isEqualTo(0.8f)
    assertThat(domain.topP).isEqualTo(0.95f)
    assertThat(domain.defaultVoice).isEqualTo("en-US-Wavenet-F")
    assertThat(domain.defaultImageStyle).isEqualTo("watercolor")
    assertThat(domain.createdAt).isEqualTo(createdAt)
    assertThat(domain.updatedAt).isEqualTo(updatedAt)
  }

  @Test
  fun `toEntity maps all fields correctly`() {
    val personaId = UUID.randomUUID()
    val createdAt = Instant.parse("2024-01-15T10:30:00Z")
    val updatedAt = Instant.parse("2024-01-15T11:00:00Z")

    val domain =
      PersonaProfile(
        personaId = personaId,
        name = "Code Helper",
        description = "Helps with programming",
        systemPrompt = "You are an expert programmer. Help with code.",
        defaultModelPreference = "codellama-7b",
        temperature = 0.2f,
        topP = 0.9f,
        defaultVoice = null,
        defaultImageStyle = null,
        createdAt = createdAt,
        updatedAt = updatedAt,
      )

    val entity = PersonaProfileMapper.toEntity(domain)

    assertThat(entity.personaId).isEqualTo(personaId.toString())
    assertThat(entity.name).isEqualTo("Code Helper")
    assertThat(entity.description).isEqualTo("Helps with programming")
    assertThat(entity.systemPrompt).isEqualTo("You are an expert programmer. Help with code.")
    assertThat(entity.defaultModelPreference).isEqualTo("codellama-7b")
    assertThat(entity.temperature).isEqualTo(0.2f)
    assertThat(entity.topP).isEqualTo(0.9f)
    assertThat(entity.defaultVoice).isNull()
    assertThat(entity.defaultImageStyle).isNull()
    assertThat(entity.createdAt).isEqualTo(createdAt)
    assertThat(entity.updatedAt).isEqualTo(updatedAt)
  }

  @Test
  fun `round trip conversion preserves all data`() {
    val original =
      PersonaProfile(
        personaId = UUID.randomUUID(),
        name = "Default Assistant",
        description = "General purpose AI assistant",
        systemPrompt = "You are a helpful assistant.",
        defaultModelPreference = "llama-2-7b",
        temperature = 0.7f,
        topP = 1.0f,
        defaultVoice = "en-GB-Wavenet-A",
        defaultImageStyle = "photorealistic",
        createdAt = Instant.parse("2024-02-01T08:00:00Z"),
        updatedAt = Instant.parse("2024-02-01T09:00:00Z"),
      )

    val entity = PersonaProfileMapper.toEntity(original)
    val roundTrip = PersonaProfileMapper.toDomain(entity)

    assertThat(roundTrip).isEqualTo(original)
  }

  @Test
  fun `handles null optional fields correctly`() {
    val domain =
      PersonaProfile(
        personaId = UUID.randomUUID(),
        name = "Simple Persona",
        description = "Basic persona",
        systemPrompt = "You are helpful.",
        defaultModelPreference = null,
        temperature = 1.0f,
        topP = 1.0f,
        defaultVoice = null,
        defaultImageStyle = null,
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
      )

    val entity = PersonaProfileMapper.toEntity(domain)

    assertThat(entity.defaultModelPreference).isNull()
    assertThat(entity.defaultVoice).isNull()
    assertThat(entity.defaultImageStyle).isNull()

    val roundTrip = PersonaProfileMapper.toDomain(entity)
    assertThat(roundTrip).isEqualTo(domain)
  }

  @Test
  fun `extension function toDomain works correctly`() {
    val entity =
      PersonaProfileEntity(
        personaId = UUID.randomUUID().toString(),
        name = "Extension Test",
        description = "Test description",
        systemPrompt = "Test prompt",
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
      )

    val domain = entity.toDomain()

    assertThat(domain.name).isEqualTo("Extension Test")
  }

  @Test
  fun `extension function toEntity works correctly`() {
    val domain =
      PersonaProfile(
        personaId = UUID.randomUUID(),
        name = "Extension Entity Test",
        description = "Test description",
        systemPrompt = "Test prompt",
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
      )

    val entity = domain.toEntity()

    assertThat(entity.name).isEqualTo("Extension Entity Test")
  }

  @Test
  fun `default temperature and topP values are preserved`() {
    val domain =
      PersonaProfile(
        personaId = UUID.randomUUID(),
        name = "Defaults Test",
        description = "Test defaults",
        systemPrompt = "Test prompt",
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
      )

    val entity = PersonaProfileMapper.toEntity(domain)

    assertThat(entity.temperature).isEqualTo(1.0f)
    assertThat(entity.topP).isEqualTo(1.0f)
  }

  @Test
  fun `handles extreme temperature values`() {
    val domain =
      PersonaProfile(
        personaId = UUID.randomUUID(),
        name = "Temperature Test",
        description = "Test",
        systemPrompt = "Prompt",
        temperature = 0.0f,
        topP = 0.1f,
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
      )

    val entity = PersonaProfileMapper.toEntity(domain)
    val roundTrip = PersonaProfileMapper.toDomain(entity)

    assertThat(roundTrip.temperature).isEqualTo(0.0f)
    assertThat(roundTrip.topP).isEqualTo(0.1f)
  }
}
