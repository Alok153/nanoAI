package com.vjaykrsna.nanoai.core.data.db.mappers

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.db.entities.PersonaSwitchLogEntity
import com.vjaykrsna.nanoai.core.domain.model.PersonaSwitchLog
import com.vjaykrsna.nanoai.core.model.PersonaSwitchAction
import java.util.UUID
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test

class PersonaSwitchLogMapperTest {

  @Test
  fun `toDomain maps all fields correctly`() {
    val logId = UUID.randomUUID().toString()
    val threadId = UUID.randomUUID().toString()
    val previousPersonaId = UUID.randomUUID().toString()
    val newPersonaId = UUID.randomUUID().toString()
    val createdAt = Instant.parse("2024-01-15T10:30:00Z")

    val entity =
      PersonaSwitchLogEntity(
        logId = logId,
        threadId = threadId,
        previousPersonaId = previousPersonaId,
        newPersonaId = newPersonaId,
        actionTaken = PersonaSwitchAction.CONTINUE_THREAD,
        createdAt = createdAt,
      )

    val domain = PersonaSwitchLogMapper.toDomain(entity)

    assertThat(domain.logId).isEqualTo(UUID.fromString(logId))
    assertThat(domain.threadId).isEqualTo(UUID.fromString(threadId))
    assertThat(domain.previousPersonaId).isEqualTo(UUID.fromString(previousPersonaId))
    assertThat(domain.newPersonaId).isEqualTo(UUID.fromString(newPersonaId))
    assertThat(domain.actionTaken).isEqualTo(PersonaSwitchAction.CONTINUE_THREAD)
    assertThat(domain.createdAt).isEqualTo(createdAt)
  }

  @Test
  fun `toEntity maps all fields correctly`() {
    val logId = UUID.randomUUID()
    val threadId = UUID.randomUUID()
    val previousPersonaId = UUID.randomUUID()
    val newPersonaId = UUID.randomUUID()
    val createdAt = Instant.parse("2024-01-15T10:30:00Z")

    val domain =
      PersonaSwitchLog(
        logId = logId,
        threadId = threadId,
        previousPersonaId = previousPersonaId,
        newPersonaId = newPersonaId,
        actionTaken = PersonaSwitchAction.START_NEW_THREAD,
        createdAt = createdAt,
      )

    val entity = PersonaSwitchLogMapper.toEntity(domain)

    assertThat(entity.logId).isEqualTo(logId.toString())
    assertThat(entity.threadId).isEqualTo(threadId.toString())
    assertThat(entity.previousPersonaId).isEqualTo(previousPersonaId.toString())
    assertThat(entity.newPersonaId).isEqualTo(newPersonaId.toString())
    assertThat(entity.actionTaken).isEqualTo(PersonaSwitchAction.START_NEW_THREAD)
    assertThat(entity.createdAt).isEqualTo(createdAt)
  }

  @Test
  fun `round trip conversion preserves all data`() {
    val original =
      PersonaSwitchLog(
        logId = UUID.randomUUID(),
        threadId = UUID.randomUUID(),
        previousPersonaId = UUID.randomUUID(),
        newPersonaId = UUID.randomUUID(),
        actionTaken = PersonaSwitchAction.CONTINUE_THREAD,
        createdAt = Instant.parse("2024-02-01T08:00:00Z"),
      )

    val entity = PersonaSwitchLogMapper.toEntity(original)
    val roundTrip = PersonaSwitchLogMapper.toDomain(entity)

    assertThat(roundTrip).isEqualTo(original)
  }

  @Test
  fun `handles null previousPersonaId correctly`() {
    val entity =
      PersonaSwitchLogEntity(
        logId = UUID.randomUUID().toString(),
        threadId = UUID.randomUUID().toString(),
        previousPersonaId = null,
        newPersonaId = UUID.randomUUID().toString(),
        actionTaken = PersonaSwitchAction.START_NEW_THREAD,
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
      )

    val domain = PersonaSwitchLogMapper.toDomain(entity)

    assertThat(domain.previousPersonaId).isNull()
  }

  @Test
  fun `round trip with null previousPersonaId`() {
    val original =
      PersonaSwitchLog(
        logId = UUID.randomUUID(),
        threadId = UUID.randomUUID(),
        previousPersonaId = null,
        newPersonaId = UUID.randomUUID(),
        actionTaken = PersonaSwitchAction.START_NEW_THREAD,
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
      )

    val entity = PersonaSwitchLogMapper.toEntity(original)

    assertThat(entity.previousPersonaId).isNull()

    val roundTrip = PersonaSwitchLogMapper.toDomain(entity)
    assertThat(roundTrip).isEqualTo(original)
  }

  @Test
  fun `all PersonaSwitchAction values can be mapped`() {
    PersonaSwitchAction.values().forEach { action ->
      val entity =
        PersonaSwitchLogEntity(
          logId = UUID.randomUUID().toString(),
          threadId = UUID.randomUUID().toString(),
          previousPersonaId = null,
          newPersonaId = UUID.randomUUID().toString(),
          actionTaken = action,
          createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        )

      val domain = PersonaSwitchLogMapper.toDomain(entity)
      val roundTrip = PersonaSwitchLogMapper.toEntity(domain)

      assertThat(roundTrip.actionTaken).isEqualTo(action)
    }
  }

  @Test
  fun `extension function toDomain works correctly`() {
    val entity =
      PersonaSwitchLogEntity(
        logId = UUID.randomUUID().toString(),
        threadId = UUID.randomUUID().toString(),
        previousPersonaId = null,
        newPersonaId = UUID.randomUUID().toString(),
        actionTaken = PersonaSwitchAction.CONTINUE_THREAD,
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
      )

    val domain = entity.toDomain()

    assertThat(domain.actionTaken).isEqualTo(PersonaSwitchAction.CONTINUE_THREAD)
  }

  @Test
  fun `extension function toEntity works correctly`() {
    val domain =
      PersonaSwitchLog(
        logId = UUID.randomUUID(),
        threadId = UUID.randomUUID(),
        previousPersonaId = null,
        newPersonaId = UUID.randomUUID(),
        actionTaken = PersonaSwitchAction.START_NEW_THREAD,
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
      )

    val entity = domain.toEntity()

    assertThat(entity.actionTaken).isEqualTo(PersonaSwitchAction.START_NEW_THREAD)
  }

  @Test
  fun `handles first persona switch in thread`() {
    val domain =
      PersonaSwitchLog(
        logId = UUID.randomUUID(),
        threadId = UUID.randomUUID(),
        previousPersonaId = null,
        newPersonaId = UUID.randomUUID(),
        actionTaken = PersonaSwitchAction.START_NEW_THREAD,
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
      )

    val entity = PersonaSwitchLogMapper.toEntity(domain)
    val roundTrip = PersonaSwitchLogMapper.toDomain(entity)

    assertThat(roundTrip.previousPersonaId).isNull()
    assertThat(roundTrip.actionTaken).isEqualTo(PersonaSwitchAction.START_NEW_THREAD)
  }
}
