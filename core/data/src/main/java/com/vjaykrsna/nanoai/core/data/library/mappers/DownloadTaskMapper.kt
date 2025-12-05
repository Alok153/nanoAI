package com.vjaykrsna.nanoai.core.data.library.mappers

import com.vjaykrsna.nanoai.core.data.library.entities.DownloadTaskEntity
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import java.util.UUID

/** Maps download tasks between Room entities and domain models. */
internal object DownloadTaskMapper {
  /** Converts a [DownloadTaskEntity] database entity to a [DownloadTask] domain model. */
  fun toDomain(entity: DownloadTaskEntity): DownloadTask =
    DownloadTask(
      taskId = UUID.fromString(entity.taskId),
      modelId = entity.modelId,
      progress = entity.progress,
      status = entity.status,
      bytesDownloaded = entity.bytesDownloaded,
      totalBytes = entity.totalBytes,
      startedAt = entity.startedAt,
      finishedAt = entity.finishedAt,
      errorMessage = entity.errorMessage,
    )

  /** Converts a [DownloadTask] domain model to a [DownloadTaskEntity] database entity. */
  fun toEntity(domain: DownloadTask): DownloadTaskEntity =
    DownloadTaskEntity(
      taskId = domain.taskId.toString(),
      modelId = domain.modelId,
      progress = domain.progress,
      status = domain.status,
      bytesDownloaded = domain.bytesDownloaded,
      totalBytes = domain.totalBytes,
      startedAt = domain.startedAt,
      finishedAt = domain.finishedAt,
      errorMessage = domain.errorMessage,
    )
}

/** Extension function to convert entity to domain model. */
fun DownloadTaskEntity.toDomain(): DownloadTask = DownloadTaskMapper.toDomain(this)

/** Extension function to convert domain model to entity. */
fun DownloadTask.toEntity(): DownloadTaskEntity = DownloadTaskMapper.toEntity(this)
