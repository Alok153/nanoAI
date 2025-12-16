package com.vjaykrsna.nanoai.feature.library.data

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.common.NanoAIResult as CommonNanoAIResult
import com.vjaykrsna.nanoai.core.domain.library.ModelDownloadsAndExportUseCaseInterface
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.library.DownloadStatus
import com.vjaykrsna.nanoai.core.model.NanoAIResult
import com.vjaykrsna.nanoai.feature.library.domain.ModelDownloadRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** Data source contract for orchestrating model download and install operations. */
interface ModelDownloadDataSource {
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

  override suspend fun retryDownload(taskId: UUID): NanoAIResult<Unit> =
    downloadsUseCase.retryFailedDownload(taskId)

  override suspend fun deleteModel(modelId: String): NanoAIResult<Unit> =
    downloadsUseCase.deleteModel(modelId)

  override suspend fun verifyDownload(modelId: String): NanoAIResult<Boolean> =
    downloadsUseCase.verifyDownloadChecksum(modelId)
}

/** Repository implementation that isolates the data source behind the domain contract. */
@Singleton
class DefaultModelDownloadRepository
@Inject
constructor(
  private val dataSource: ModelDownloadDataSource,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ModelDownloadRepository {

  private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
  private val verifiedModels = ConcurrentHashMap.newKeySet<String>()

  init {
    observeCompletedDownloads()
  }

  override fun observeDownloads(): Flow<List<DownloadTask>> = dataSource.observeDownloads()

  override suspend fun observeDownload(taskId: UUID): Flow<DownloadTask?> =
    dataSource.observeDownload(taskId)

  override fun observeDownloadProgress(taskId: UUID): Flow<Float> =
    dataSource.observeDownloadProgress(taskId)

  override suspend fun queueDownload(modelId: String): NanoAIResult<UUID> {
    val existing =
      currentDownloads().firstOrNull { download ->
        download.modelId == modelId && download.status.isInFlight()
      }
    if (existing != null) {
      return NanoAIResult.recoverable(
        message = "Download already queued or in progress",
        context =
          mapOf(
            "modelId" to modelId,
            "taskId" to existing.taskId.toString(),
            "status" to existing.status.name,
          ),
      )
    }
    return dataSource.queueDownload(modelId)
  }

  override suspend fun pauseDownload(taskId: UUID): NanoAIResult<Unit> =
    dataSource.pauseDownload(taskId)

  override suspend fun resumeDownload(taskId: UUID): NanoAIResult<Unit> {
    val activeDownloads =
      currentDownloads().filter { it.status == DownloadStatus.DOWNLOADING && it.taskId != taskId }
    if (activeDownloads.size >= MAX_CONCURRENT_DOWNLOADS) {
      return NanoAIResult.recoverable(
        message = "Another download is already in progress",
        context = mapOf("taskId" to taskId.toString(), "active" to activeDownloads.size.toString()),
      )
    }
    return dataSource.resumeDownload(taskId)
  }

  override suspend fun cancelDownload(taskId: UUID): NanoAIResult<Unit> =
    dataSource.cancelDownload(taskId)

  override suspend fun retryDownload(taskId: UUID): NanoAIResult<Unit> =
    dataSource.retryDownload(taskId)

  override suspend fun deleteModel(modelId: String): NanoAIResult<Unit> =
    dataSource.deleteModel(modelId)

  override suspend fun verifyDownload(modelId: String): NanoAIResult<Boolean> =
    dataSource.verifyDownload(modelId)

  private suspend fun currentDownloads(): List<DownloadTask> = dataSource.observeDownloads().first()

  private fun observeCompletedDownloads() {
    dataSource
      .observeDownloads()
      .onEach { downloads ->
        downloads
          .filter { it.status == DownloadStatus.COMPLETED }
          .forEach { completed -> maybeVerify(completed.modelId) }
      }
      .launchIn(scope)
  }

  private fun maybeVerify(modelId: String) {
    if (!verifiedModels.add(modelId)) return
    scope.launch {
      when (val result = dataSource.verifyDownload(modelId)) {
        is CommonNanoAIResult.Success ->
          if (!result.value) {
            verifiedModels.remove(modelId)
          }
        is CommonNanoAIResult.RecoverableError,
        is CommonNanoAIResult.FatalError -> verifiedModels.remove(modelId)
      }
    }
  }

  private fun DownloadStatus.isInFlight(): Boolean =
    this == DownloadStatus.DOWNLOADING ||
      this == DownloadStatus.PAUSED ||
      this == DownloadStatus.QUEUED

  private companion object {
    const val MAX_CONCURRENT_DOWNLOADS = 1
  }
}
