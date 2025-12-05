package com.vjaykrsna.nanoai.core.data.library.impl

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.vjaykrsna.nanoai.core.data.library.ModelArtifactStore
import com.vjaykrsna.nanoai.core.data.library.daos.DownloadTaskDao
import com.vjaykrsna.nanoai.core.data.library.mappers.toDomain
import com.vjaykrsna.nanoai.core.data.library.mappers.toEntity
import com.vjaykrsna.nanoai.core.data.library.workers.ModelDownloadWorker
import com.vjaykrsna.nanoai.core.domain.library.DownloadManager
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.library.DownloadStatus
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

/**
 * Implementation of DownloadManager.
 *
 * Wraps DownloadTaskDao and integrates with WorkManager for background download orchestration.
 */
@Singleton
class DownloadManagerImpl
@Inject
constructor(
  private val downloadTaskDao: DownloadTaskDao,
  private val workManager: WorkManager,
  private val artifactStore: ModelArtifactStore,
) : DownloadManager {
  override suspend fun startDownload(modelId: String): UUID {
    val taskId = queueDownload(modelId)
    enqueueWorker(taskId, modelId, ExistingWorkPolicy.KEEP)
    return taskId
  }

  override suspend fun queueDownload(modelId: String): UUID {
    val taskId = UUID.randomUUID()
    val now = Clock.System.now()
    val task =
      DownloadTask(
        taskId = taskId,
        modelId = modelId,
        status = DownloadStatus.QUEUED,
        progress = 0f,
        bytesDownloaded = 0L,
        startedAt = now,
        finishedAt = null,
        errorMessage = null,
      )
    downloadTaskDao.insert(task.toEntity())
    return taskId
  }

  override suspend fun pauseDownload(taskId: UUID) {
    workManager.cancelAllWorkByTag(tagFor(taskId))
    downloadTaskDao.updateStatus(taskId.toString(), DownloadStatus.PAUSED)
  }

  override suspend fun resumeDownload(taskId: UUID) {
    val task = downloadTaskDao.getById(taskId.toString()) ?: return
    downloadTaskDao.updateStatus(taskId.toString(), DownloadStatus.DOWNLOADING)
    enqueueWorker(taskId, task.modelId, ExistingWorkPolicy.REPLACE)
  }

  override suspend fun cancelDownload(taskId: UUID) {
    workManager.cancelAllWorkByTag(tagFor(taskId))
    downloadTaskDao.updateStatus(taskId.toString(), DownloadStatus.CANCELLED)
  }

  override suspend fun retryDownload(taskId: UUID) {
    val task = downloadTaskDao.getById(taskId.toString()) ?: return
    resetTask(taskId)
    enqueueWorker(taskId, task.modelId, ExistingWorkPolicy.REPLACE)
  }

  override suspend fun resetTask(taskId: UUID) {
    val task = downloadTaskDao.getById(taskId.toString()) ?: return
    val reset =
      task.copy(
        status = DownloadStatus.QUEUED,
        progress = 0f,
        bytesDownloaded = 0L,
        errorMessage = null,
        startedAt = Clock.System.now(),
        finishedAt = null,
      )
    downloadTaskDao.update(reset)
  }

  override suspend fun getDownloadStatus(taskId: UUID): DownloadTask? =
    downloadTaskDao.getById(taskId.toString())?.toDomain()

  override suspend fun getTaskById(taskId: UUID): Flow<DownloadTask?> =
    downloadTaskDao.observeById(taskId.toString()).map { it?.toDomain() }

  override suspend fun getActiveDownloads(): Flow<List<DownloadTask>> =
    downloadTaskDao.observeActiveDownloads().map { tasks -> tasks.map { it.toDomain() } }

  override suspend fun getActiveDownloadsSnapshot(): List<DownloadTask> =
    downloadTaskDao.getActiveDownloads().map { it.toDomain() }

  override fun getQueuedDownloads(): Flow<List<DownloadTask>> =
    downloadTaskDao.observeQueuedDownloads().map { tasks -> tasks.map { it.toDomain() } }

  override fun observeManagedDownloads(): Flow<List<DownloadTask>> =
    downloadTaskDao.observeManagedDownloads().map { tasks -> tasks.map { it.toDomain() } }

  override fun observeProgress(taskId: UUID): Flow<Float> =
    downloadTaskDao.observeById(taskId.toString()).map { it?.progress ?: 0f }

  override suspend fun getMaxConcurrentDownloads(): Int =
    DownloadManager.DEFAULT_MAX_CONCURRENT_DOWNLOADS

  override suspend fun updateTaskStatus(taskId: UUID, status: DownloadStatus) {
    downloadTaskDao.updateStatus(taskId.toString(), status)
  }

  override suspend fun getModelIdForTask(taskId: UUID): String? =
    downloadTaskDao.getModelIdForTask(taskId.toString())

  override suspend fun getDownloadedChecksum(modelId: String): String? =
    artifactStore.checksumForModel(modelId)

  override suspend fun deletePartialFiles(modelId: String) {
    artifactStore.deleteArtifacts(modelId)
  }

  private fun enqueueWorker(taskId: UUID, modelId: String, policy: ExistingWorkPolicy) {
    val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    val workRequest =
      OneTimeWorkRequestBuilder<ModelDownloadWorker>()
        .setConstraints(constraints)
        .setInputData(workDataOf("TASK_ID" to taskId.toString(), "MODEL_ID" to modelId))
        .addTag(tagFor(taskId))
        .build()

    workManager.enqueueUniqueWork(tagFor(taskId), policy, workRequest)
  }

  private fun tagFor(taskId: UUID): String = "download_$taskId"
}
