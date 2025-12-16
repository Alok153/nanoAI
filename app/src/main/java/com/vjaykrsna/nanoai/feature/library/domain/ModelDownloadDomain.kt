package com.vjaykrsna.nanoai.feature.library.domain

import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.model.NanoAIResult
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Feature-level repository for model downloads and lifecycle actions. */
interface ModelDownloadRepository {
  fun observeDownloads(): Flow<List<DownloadTask>>

  suspend fun observeDownload(taskId: UUID): Flow<DownloadTask?>

  fun observeDownloadProgress(taskId: UUID): Flow<Float>

  suspend fun queueDownload(modelId: String): NanoAIResult<UUID>

  suspend fun pauseDownload(taskId: UUID): NanoAIResult<Unit>

  suspend fun resumeDownload(taskId: UUID): NanoAIResult<Unit>

  suspend fun cancelDownload(taskId: UUID): NanoAIResult<Unit>

  suspend fun retryDownload(taskId: UUID): NanoAIResult<Unit>

  suspend fun deleteModel(modelId: String): NanoAIResult<Unit>

  suspend fun verifyDownload(modelId: String): NanoAIResult<Boolean>
}

class QueueModelDownloadUseCase
@Inject
constructor(private val repository: ModelDownloadRepository) {
  suspend operator fun invoke(modelId: String): NanoAIResult<UUID> =
    repository.queueDownload(modelId)
}

class PauseModelDownloadUseCase
@Inject
constructor(private val repository: ModelDownloadRepository) {
  suspend operator fun invoke(taskId: UUID): NanoAIResult<Unit> = repository.pauseDownload(taskId)
}

class ResumeModelDownloadUseCase
@Inject
constructor(private val repository: ModelDownloadRepository) {
  suspend operator fun invoke(taskId: UUID): NanoAIResult<Unit> = repository.resumeDownload(taskId)
}

class CancelModelDownloadUseCase
@Inject
constructor(private val repository: ModelDownloadRepository) {
  suspend operator fun invoke(taskId: UUID): NanoAIResult<Unit> = repository.cancelDownload(taskId)
}

class RetryModelDownloadUseCase
@Inject
constructor(private val repository: ModelDownloadRepository) {
  suspend operator fun invoke(taskId: UUID): NanoAIResult<Unit> = repository.retryDownload(taskId)
}

class DeleteModelUseCase @Inject constructor(private val repository: ModelDownloadRepository) {
  suspend operator fun invoke(modelId: String): NanoAIResult<Unit> = repository.deleteModel(modelId)
}

class VerifyModelDownloadUseCase
@Inject
constructor(private val repository: ModelDownloadRepository) {
  suspend operator fun invoke(modelId: String): NanoAIResult<Boolean> =
    repository.verifyDownload(modelId)
}

class ObserveDownloadTaskUseCase
@Inject
constructor(private val repository: ModelDownloadRepository) {
  suspend operator fun invoke(taskId: UUID): Flow<DownloadTask?> =
    repository.observeDownload(taskId)
}

class ObserveDownloadTasksUseCase
@Inject
constructor(private val repository: ModelDownloadRepository) {
  operator fun invoke(): Flow<List<DownloadTask>> = repository.observeDownloads()
}

class ObserveDownloadProgressUseCase
@Inject
constructor(private val repository: ModelDownloadRepository) {
  operator fun invoke(taskId: UUID): Flow<Float> = repository.observeDownloadProgress(taskId)
}
