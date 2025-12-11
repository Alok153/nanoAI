package com.vjaykrsna.nanoai.feature.library.data

import com.vjaykrsna.nanoai.core.domain.library.ModelDownloadsAndExportUseCaseInterface
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.model.NanoAIResult
import com.vjaykrsna.nanoai.feature.library.domain.ModelDownloadRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** Data source contract for orchestrating model download and install operations. */
interface ModelDownloadDataSource {
  fun observeDownloads(): Flow<List<DownloadTask>>

  suspend fun observeDownload(taskId: UUID): Flow<DownloadTask?>

  fun observeDownloadProgress(taskId: UUID): Flow<Float>

  suspend fun queueDownload(modelId: String): NanoAIResult<UUID>

  suspend fun pauseDownload(taskId: UUID): NanoAIResult<Unit>

  suspend fun resumeDownload(taskId: UUID): NanoAIResult<Unit>

  suspend fun cancelDownload(taskId: UUID): NanoAIResult<Unit>

  suspend fun deleteModel(modelId: String): NanoAIResult<Unit>

  suspend fun verifyDownload(modelId: String): NanoAIResult<Boolean>
}

/** Data source implementation backed by the shared model download use case. */
@Singleton
class CoreModelDownloadDataSource
@Inject
constructor(private val downloadsUseCase: ModelDownloadsAndExportUseCaseInterface) :
  ModelDownloadDataSource {

  override fun observeDownloads(): Flow<List<DownloadTask>> =
    downloadsUseCase.observeDownloadTasks()

  override suspend fun observeDownload(taskId: UUID): Flow<DownloadTask?> =
    downloadsUseCase.observeDownloadTask(taskId)

  override fun observeDownloadProgress(taskId: UUID): Flow<Float> =
    downloadsUseCase.getDownloadProgress(taskId)

  override suspend fun queueDownload(modelId: String): NanoAIResult<UUID> =
    downloadsUseCase.downloadModel(modelId)

  override suspend fun pauseDownload(taskId: UUID): NanoAIResult<Unit> =
    downloadsUseCase.pauseDownload(taskId)

  override suspend fun resumeDownload(taskId: UUID): NanoAIResult<Unit> =
    downloadsUseCase.resumeDownload(taskId)

  override suspend fun cancelDownload(taskId: UUID): NanoAIResult<Unit> =
    downloadsUseCase.cancelDownload(taskId)

  override suspend fun deleteModel(modelId: String): NanoAIResult<Unit> =
    downloadsUseCase.deleteModel(modelId)

  override suspend fun verifyDownload(modelId: String): NanoAIResult<Boolean> =
    downloadsUseCase.verifyDownloadChecksum(modelId)
}

/** Repository implementation that isolates the data source behind the domain contract. */
@Singleton
class DefaultModelDownloadRepository
@Inject
constructor(private val dataSource: ModelDownloadDataSource) : ModelDownloadRepository {

  override fun observeDownloads(): Flow<List<DownloadTask>> = dataSource.observeDownloads()

  override suspend fun observeDownload(taskId: UUID): Flow<DownloadTask?> =
    dataSource.observeDownload(taskId)

  override fun observeDownloadProgress(taskId: UUID): Flow<Float> =
    dataSource.observeDownloadProgress(taskId)

  override suspend fun queueDownload(modelId: String): NanoAIResult<UUID> =
    dataSource.queueDownload(modelId)

  override suspend fun pauseDownload(taskId: UUID): NanoAIResult<Unit> =
    dataSource.pauseDownload(taskId)

  override suspend fun resumeDownload(taskId: UUID): NanoAIResult<Unit> =
    dataSource.resumeDownload(taskId)

  override suspend fun cancelDownload(taskId: UUID): NanoAIResult<Unit> =
    dataSource.cancelDownload(taskId)

  override suspend fun deleteModel(modelId: String): NanoAIResult<Unit> =
    dataSource.deleteModel(modelId)

  override suspend fun verifyDownload(modelId: String): NanoAIResult<Boolean> =
    dataSource.verifyDownload(modelId)
}
