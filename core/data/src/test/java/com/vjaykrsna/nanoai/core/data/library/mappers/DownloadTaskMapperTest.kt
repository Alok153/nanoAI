package com.vjaykrsna.nanoai.core.data.library.mappers

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.library.entities.DownloadTaskEntity
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.library.DownloadStatus
import java.util.UUID
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test

class DownloadTaskMapperTest {

  @Test
  fun `toDomain maps all fields correctly`() {
    val taskId = UUID.randomUUID().toString()
    val startedAt = Instant.parse("2024-01-15T10:30:00Z")
    val finishedAt = Instant.parse("2024-01-15T10:35:00Z")

    val entity =
      DownloadTaskEntity(
        taskId = taskId,
        modelId = "gemma-2b-it",
        progress = 0.75f,
        status = DownloadStatus.DOWNLOADING,
        bytesDownloaded = 750_000_000L,
        totalBytes = 1_000_000_000L,
        startedAt = startedAt,
        finishedAt = null,
        errorMessage = null,
      )

    val domain = DownloadTaskMapper.toDomain(entity)

    assertThat(domain.taskId).isEqualTo(UUID.fromString(taskId))
    assertThat(domain.modelId).isEqualTo("gemma-2b-it")
    assertThat(domain.progress).isEqualTo(0.75f)
    assertThat(domain.status).isEqualTo(DownloadStatus.DOWNLOADING)
    assertThat(domain.bytesDownloaded).isEqualTo(750_000_000L)
    assertThat(domain.totalBytes).isEqualTo(1_000_000_000L)
    assertThat(domain.startedAt).isEqualTo(startedAt)
    assertThat(domain.finishedAt).isNull()
    assertThat(domain.errorMessage).isNull()
  }

  @Test
  fun `toEntity maps all fields correctly`() {
    val taskId = UUID.randomUUID()
    val startedAt = Instant.parse("2024-01-15T10:30:00Z")
    val finishedAt = Instant.parse("2024-01-15T10:35:00Z")

    val domain =
      DownloadTask(
        taskId = taskId,
        modelId = "phi-2",
        progress = 1.0f,
        status = DownloadStatus.COMPLETED,
        bytesDownloaded = 2_000_000_000L,
        totalBytes = 2_000_000_000L,
        startedAt = startedAt,
        finishedAt = finishedAt,
        errorMessage = null,
      )

    val entity = DownloadTaskMapper.toEntity(domain)

    assertThat(entity.taskId).isEqualTo(taskId.toString())
    assertThat(entity.modelId).isEqualTo("phi-2")
    assertThat(entity.progress).isEqualTo(1.0f)
    assertThat(entity.status).isEqualTo(DownloadStatus.COMPLETED)
    assertThat(entity.bytesDownloaded).isEqualTo(2_000_000_000L)
    assertThat(entity.totalBytes).isEqualTo(2_000_000_000L)
    assertThat(entity.startedAt).isEqualTo(startedAt)
    assertThat(entity.finishedAt).isEqualTo(finishedAt)
    assertThat(entity.errorMessage).isNull()
  }

  @Test
  fun `round trip conversion preserves all data`() {
    val original =
      DownloadTask(
        taskId = UUID.randomUUID(),
        modelId = "llama-3b",
        progress = 0.5f,
        status = DownloadStatus.PAUSED,
        bytesDownloaded = 500_000_000L,
        totalBytes = 1_000_000_000L,
        startedAt = Instant.parse("2024-02-01T08:00:00Z"),
        finishedAt = null,
        errorMessage = null,
      )

    val entity = DownloadTaskMapper.toEntity(original)
    val roundTrip = DownloadTaskMapper.toDomain(entity)

    assertThat(roundTrip).isEqualTo(original)
  }

  @Test
  fun `handles queued status correctly`() {
    val entity =
      DownloadTaskEntity(
        taskId = UUID.randomUUID().toString(),
        modelId = "test-model",
        progress = 0f,
        status = DownloadStatus.QUEUED,
        bytesDownloaded = 0L,
        totalBytes = 0L,
        startedAt = null,
        finishedAt = null,
        errorMessage = null,
      )

    val domain = DownloadTaskMapper.toDomain(entity)

    assertThat(domain.status).isEqualTo(DownloadStatus.QUEUED)
    assertThat(domain.progress).isEqualTo(0f)
    assertThat(domain.startedAt).isNull()
  }

  @Test
  fun `handles failed status with error message`() {
    val entity =
      DownloadTaskEntity(
        taskId = UUID.randomUUID().toString(),
        modelId = "failed-model",
        progress = 0.3f,
        status = DownloadStatus.FAILED,
        bytesDownloaded = 300_000_000L,
        totalBytes = 1_000_000_000L,
        startedAt = Instant.parse("2024-01-01T00:00:00Z"),
        finishedAt = Instant.parse("2024-01-01T00:05:00Z"),
        errorMessage = "Network connection lost",
      )

    val domain = DownloadTaskMapper.toDomain(entity)

    assertThat(domain.status).isEqualTo(DownloadStatus.FAILED)
    assertThat(domain.errorMessage).isEqualTo("Network connection lost")
    assertThat(domain.finishedAt).isNotNull()
  }

  @Test
  fun `handles cancelled status correctly`() {
    val domain =
      DownloadTask(
        taskId = UUID.randomUUID(),
        modelId = "cancelled-model",
        progress = 0.5f,
        status = DownloadStatus.CANCELLED,
        bytesDownloaded = 500_000_000L,
        totalBytes = 1_000_000_000L,
        startedAt = Instant.parse("2024-01-01T00:00:00Z"),
        finishedAt = Instant.parse("2024-01-01T00:03:00Z"),
        errorMessage = null,
      )

    val entity = DownloadTaskMapper.toEntity(domain)
    val roundTrip = DownloadTaskMapper.toDomain(entity)

    assertThat(roundTrip.status).isEqualTo(DownloadStatus.CANCELLED)
    assertThat(roundTrip).isEqualTo(domain)
  }

  @Test
  fun `extension function toDomain works correctly`() {
    val entity =
      DownloadTaskEntity(
        taskId = UUID.randomUUID().toString(),
        modelId = "extension-test",
        progress = 0.25f,
        status = DownloadStatus.DOWNLOADING,
        bytesDownloaded = 0L,
        totalBytes = 0L,
      )

    val domain = entity.toDomain()

    assertThat(domain.modelId).isEqualTo("extension-test")
    assertThat(domain.progress).isEqualTo(0.25f)
  }

  @Test
  fun `extension function toEntity works correctly`() {
    val domain =
      DownloadTask(
        taskId = UUID.randomUUID(),
        modelId = "extension-entity-test",
        progress = 0.8f,
        status = DownloadStatus.DOWNLOADING,
      )

    val entity = domain.toEntity()

    assertThat(entity.modelId).isEqualTo("extension-entity-test")
    assertThat(entity.progress).isEqualTo(0.8f)
  }

  @Test
  fun `handles zero bytes correctly`() {
    val domain =
      DownloadTask(
        taskId = UUID.randomUUID(),
        modelId = "zero-bytes",
        progress = 0f,
        status = DownloadStatus.QUEUED,
        bytesDownloaded = 0L,
        totalBytes = 0L,
      )

    val entity = DownloadTaskMapper.toEntity(domain)
    val roundTrip = DownloadTaskMapper.toDomain(entity)

    assertThat(roundTrip.bytesDownloaded).isEqualTo(0L)
    assertThat(roundTrip.totalBytes).isEqualTo(0L)
  }

  @Test
  fun `all download statuses can be mapped`() {
    DownloadStatus.values().forEach { status ->
      val entity =
        DownloadTaskEntity(
          taskId = UUID.randomUUID().toString(),
          modelId = "status-test",
          progress = 0f,
          status = status,
          bytesDownloaded = 0L,
          totalBytes = 0L,
        )

      val domain = DownloadTaskMapper.toDomain(entity)
      val roundTrip = DownloadTaskMapper.toEntity(domain)

      assertThat(roundTrip.status).isEqualTo(status)
    }
  }
}
