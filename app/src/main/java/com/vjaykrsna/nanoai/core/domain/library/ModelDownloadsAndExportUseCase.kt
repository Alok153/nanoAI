package com.vjaykrsna.nanoai.core.domain.library

import android.database.sqlite.SQLiteException
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.fold
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.library.DownloadStatus
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Orchestrates model download lifecycle management and export bundle creation. */
@Singleton
class ModelDownloadsAndExportUseCase
@Inject
constructor(
  private val modelCatalogRepository: ModelCatalogRepository,
  private val downloadManager: DownloadManager,
  private val exportService: ExportService,
) : ModelDownloadsAndExportUseCaseInterface {
  /** Queue or start a download based on concurrency limits. */
  override suspend fun downloadModel(modelId: String): NanoAIResult<UUID> =
    guardOperation(
      message = "Failed to start download for model $modelId",
      context = mapOf("modelId" to modelId),
    ) {
      val activeDownloads = downloadManager.getActiveDownloads().first()
      val activeCount = activeDownloads.count { it.status == DownloadStatus.DOWNLOADING }
      val maxConcurrent = downloadManager.getMaxConcurrentDownloads()

      val taskId =
        if (activeCount >= maxConcurrent) {
          downloadManager.queueDownload(modelId)
        } else {
          downloadManager.startDownload(modelId)
        }

      modelCatalogRepository.updateInstallState(modelId, InstallState.DOWNLOADING)
      modelCatalogRepository.updateDownloadTaskId(modelId, taskId)
      NanoAIResult.success(taskId)
    }

  /** Validate downloaded checksum and update install state accordingly. */
  override suspend fun verifyDownloadChecksum(modelId: String): NanoAIResult<Boolean> =
    guardOperation(
      message = "Failed to verify checksum for model $modelId",
      context = mapOf("modelId" to modelId),
    ) {
      loadChecksumContext(modelId)
        .fold(
          onSuccess = { context ->
            val matches = context.expectedChecksum.equals(context.actualChecksum, ignoreCase = true)
            if (matches) {
              modelCatalogRepository.updateInstallState(modelId, InstallState.INSTALLED)
              modelCatalogRepository.updateChecksum(modelId, context.actualChecksum)
              context.model.downloadTaskId?.let {
                downloadManager.updateTaskStatus(it, DownloadStatus.COMPLETED)
              }
            } else {
              modelCatalogRepository.updateInstallState(modelId, InstallState.ERROR)
            }
            NanoAIResult.success(matches)
          },
          onFailure = { error -> error },
        )
    }

  /** Pause a download task and persist status. */
  override suspend fun pauseDownload(taskId: UUID): NanoAIResult<Unit> =
    guardOperation(
      message = "Failed to pause download $taskId",
      context = mapOf("taskId" to taskId.toString()),
    ) {
      downloadManager.pauseDownload(taskId)
      downloadManager.updateTaskStatus(taskId, DownloadStatus.PAUSED)
      NanoAIResult.success(Unit)
    }

  /** Resume a paused download. */
  override suspend fun resumeDownload(taskId: UUID): NanoAIResult<Unit> =
    guardOperation(
      message = "Failed to resume download $taskId",
      context = mapOf("taskId" to taskId.toString()),
    ) {
      requireDownloadTask(taskId)
        .fold(
          onSuccess = { task ->
            ensureTaskStatus(
                task = task,
                expectedStatus = DownloadStatus.PAUSED,
                taskId = taskId,
                errorMessage = "Download task $taskId is not paused",
              )
              .fold(
                onSuccess = {
                  downloadManager.resumeDownload(taskId)
                  downloadManager.updateTaskStatus(taskId, DownloadStatus.DOWNLOADING)
                  NanoAIResult.success(Unit)
                },
                onFailure = { error -> error },
              )
          },
          onFailure = { error -> error },
        )
    }

  /** Cancel a download and cleanup associated files. */
  override suspend fun cancelDownload(taskId: UUID): NanoAIResult<Unit> =
    guardOperation(
      message = "Failed to cancel download $taskId",
      context = mapOf("taskId" to taskId.toString()),
    ) {
      val modelId = downloadManager.getModelIdForTask(taskId)
      downloadManager.cancelDownload(taskId)
      modelId?.let {
        downloadManager.deletePartialFiles(it)
        modelCatalogRepository.updateInstallState(it, InstallState.NOT_INSTALLED)
        modelCatalogRepository.updateDownloadTaskId(it, null)
      }
      NanoAIResult.success(Unit)
    }

  /** Delete a model if not active in any chat session. */
  override suspend fun deleteModel(modelId: String): NanoAIResult<Unit> =
    guardOperation(
      message = "Failed to delete model $modelId",
      context = mapOf("modelId" to modelId),
    ) {
      val inUse = modelCatalogRepository.isModelActiveInSession(modelId)
      if (inUse) {
        return@guardOperation NanoAIResult.recoverable(
          message = "Model $modelId is active in a conversation",
          context = mapOf("modelId" to modelId),
        )
      }

      modelCatalogRepository.deleteModelFiles(modelId)
      modelCatalogRepository.updateInstallState(modelId, InstallState.NOT_INSTALLED)
      modelCatalogRepository.updateDownloadTaskId(modelId, null)
      NanoAIResult.success(Unit)
    }

  /** Export personas, provider configs, and optional chat history as bundle. */
  override suspend fun exportBackup(
    destinationPath: String,
    includeChatHistory: Boolean,
  ): NanoAIResult<String> =
    guardOperation(
      message = "Failed to export backup to $destinationPath",
      context =
        mapOf(
          "destinationPath" to destinationPath,
          "includeChatHistory" to includeChatHistory.toString(),
        ),
    ) {
      val personas = exportService.gatherPersonas()
      val providers = exportService.gatherAPIProviderConfigs()
      val chatHistory = if (includeChatHistory) exportService.gatherChatHistory() else emptyList()

      val bundlePath =
        exportService.createExportBundle(personas, providers, destinationPath, chatHistory)
      exportService.notifyUnencryptedExport(bundlePath)
      NanoAIResult.success(bundlePath)
    }

  /** Observe download progress as a Flow. */
  override fun getDownloadProgress(taskId: UUID): Flow<Float> =
    downloadManager.observeProgress(taskId)

  /** Observe a specific download task for status or error updates. */
  override suspend fun observeDownloadTask(taskId: UUID): Flow<DownloadTask?> =
    downloadManager.getTaskById(taskId)

  /** Observe queued download tasks. */
  override fun observeDownloadTasks(): Flow<List<DownloadTask>> =
    downloadManager.observeManagedDownloads()

  /** Retry a failed download task. */
  override suspend fun retryFailedDownload(taskId: UUID): NanoAIResult<Unit> =
    guardOperation(
      message = "Failed to retry download $taskId",
      context = mapOf("taskId" to taskId.toString()),
    ) {
      requireDownloadTask(taskId)
        .fold(
          onSuccess = { task ->
            ensureTaskStatus(
                task = task,
                expectedStatus = DownloadStatus.FAILED,
                taskId = taskId,
                errorMessage = "Download task $taskId is not failed",
              )
              .fold(
                onSuccess = {
                  requireModelIdForTask(taskId)
                    .fold(
                      onSuccess = { modelId ->
                        downloadManager.resetTask(taskId)
                        downloadManager.startDownload(modelId)
                        modelCatalogRepository.updateInstallState(modelId, InstallState.DOWNLOADING)
                        modelCatalogRepository.updateDownloadTaskId(modelId, taskId)
                        NanoAIResult.success(Unit)
                      },
                      onFailure = { error -> error },
                    )
                },
                onFailure = { error -> error },
              )
          },
          onFailure = { error -> error },
        )
    }

  private inline fun <T> guardOperation(
    message: String,
    context: Map<String, String>,
    block: () -> NanoAIResult<T>,
  ): NanoAIResult<T> {
    return try {
      block()
    } catch (cancellation: CancellationException) {
      throw cancellation
    } catch (sqliteException: SQLiteException) {
      NanoAIResult.recoverable(message = message, cause = sqliteException, context = context)
    } catch (ioException: IOException) {
      NanoAIResult.recoverable(message = message, cause = ioException, context = context)
    } catch (illegalStateException: IllegalStateException) {
      NanoAIResult.recoverable(message = message, cause = illegalStateException, context = context)
    } catch (illegalArgumentException: IllegalArgumentException) {
      NanoAIResult.recoverable(
        message = message,
        cause = illegalArgumentException,
        context = context,
      )
    } catch (securityException: SecurityException) {
      NanoAIResult.recoverable(message = message, cause = securityException, context = context)
    }
  }

  private data class ChecksumContext(
    val model: ModelPackage,
    val expectedChecksum: String,
    val actualChecksum: String,
  )

  private suspend fun loadChecksumContext(modelId: String): NanoAIResult<ChecksumContext> {
    val model = modelCatalogRepository.getModelById(modelId).first()
    return when {
      model == null ->
        NanoAIResult.recoverable(
          message = "Model $modelId not found",
          context = mapOf("modelId" to modelId),
        )
      model.checksumSha256 == null -> {
        modelCatalogRepository.updateInstallState(modelId, InstallState.ERROR)
        NanoAIResult.recoverable(
          message = "No checksum available for model $modelId",
          context = mapOf("modelId" to modelId),
        )
      }
      else -> {
        val expectedChecksum = model.checksumSha256
        val actualChecksum = downloadManager.getDownloadedChecksum(modelId)
        if (actualChecksum == null) {
          modelCatalogRepository.updateInstallState(modelId, InstallState.ERROR)
          NanoAIResult.recoverable(
            message = "Downloaded checksum not available for model $modelId",
            context = mapOf("modelId" to modelId),
          )
        } else {
          NanoAIResult.success(
            ChecksumContext(
              model = model,
              expectedChecksum = expectedChecksum,
              actualChecksum = actualChecksum,
            )
          )
        }
      }
    }
  }

  private suspend fun requireDownloadTask(taskId: UUID): NanoAIResult<DownloadTask> {
    val task = downloadManager.getTaskById(taskId).first()
    return if (task != null) {
      NanoAIResult.success(task)
    } else {
      NanoAIResult.recoverable(
        message = "Download task $taskId not found",
        context = mapOf("taskId" to taskId.toString()),
      )
    }
  }

  private fun ensureTaskStatus(
    task: DownloadTask,
    expectedStatus: DownloadStatus,
    taskId: UUID,
    errorMessage: String,
  ): NanoAIResult<DownloadTask> {
    return if (task.status == expectedStatus) {
      NanoAIResult.success(task)
    } else {
      NanoAIResult.recoverable(
        message = errorMessage,
        context = mapOf("taskId" to taskId.toString(), "status" to task.status.toString()),
      )
    }
  }

  private suspend fun requireModelIdForTask(taskId: UUID): NanoAIResult<String> {
    val modelId = downloadManager.getModelIdForTask(taskId)
    return if (modelId != null) {
      NanoAIResult.success(modelId)
    } else {
      NanoAIResult.recoverable(
        message = "Model ID not found for task $taskId",
        context = mapOf("taskId" to taskId.toString()),
      )
    }
  }
}
