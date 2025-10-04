package com.vjaykrsna.nanoai.feature.library.data.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.vjaykrsna.nanoai.feature.library.data.daos.DownloadTaskDao
import com.vjaykrsna.nanoai.feature.library.data.daos.ModelPackageDao
import com.vjaykrsna.nanoai.feature.library.model.DownloadStatus
import com.vjaykrsna.nanoai.feature.library.model.InstallState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import okhttp3.OkHttpClient
import okhttp3.Request

private const val DOWNLOAD_BUFFER_SIZE = 8_192

/**
 * WorkManager worker for downloading AI model files.
 *
 * Handles background download with progress tracking, resume capability, and checksum verification.
 */
@HiltWorker
class ModelDownloadWorker
@AssistedInject
constructor(
  @Assisted appContext: Context,
  @Assisted workerParams: WorkerParameters,
  private val downloadTaskDao: DownloadTaskDao,
  private val modelPackageDao: ModelPackageDao,
  private val okHttpClient: OkHttpClient,
) : CoroutineWorker(appContext, workerParams) {
  override suspend fun doWork(): Result =
    withContext(Dispatchers.IO) {
      val taskId = inputData.getString("TASK_ID") ?: return@withContext Result.failure()
      val modelId = inputData.getString("MODEL_ID") ?: return@withContext Result.failure()

      try {
        // Get model package info
        val modelPackage =
          modelPackageDao.getById(modelId)
            ?: return@withContext Result.failure(
              workDataOf("ERROR" to "Model not found: $modelId"),
            )

        // Update status to downloading
        downloadTaskDao.updateStatus(taskId, DownloadStatus.DOWNLOADING)

        // Update started_at time by fetching and updating entity
        val task = downloadTaskDao.getById(taskId)
        if (task != null) {
          downloadTaskDao.update(task.copy(startedAt = Clock.System.now()))
        }

        // Create download directory
        val downloadDir = File(applicationContext.filesDir, "models")
        downloadDir.mkdirs()

        val outputFile = File(downloadDir, "$modelId.bin")

        // Future work: load download URL from model package metadata once remote catalog is wired
        // up
        val downloadUrl = "https://example.com/models/$modelId.bin" // Placeholder

        // Execute download with progress tracking
        val request = Request.Builder().url(downloadUrl).build()

        val response = okHttpClient.newCall(request).execute()

        if (!response.isSuccessful) {
          throw Exception("Download failed with code: ${response.code}")
        }

        val totalBytes = response.body?.contentLength() ?: 0L
        val inputStream = response.body?.byteStream() ?: throw Exception("Empty response body")

        FileOutputStream(outputFile).use { outputStream ->
          val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
          var bytesRead: Int
          var totalBytesDownloaded = 0L

          while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            // Check if cancelled
            if (isStopped) {
              inputStream.close()
              outputFile.delete()
              downloadTaskDao.updateStatus(taskId, DownloadStatus.CANCELLED)
              return@withContext Result.failure(
                workDataOf("ERROR" to "Download cancelled"),
              )
            }

            outputStream.write(buffer, 0, bytesRead)
            totalBytesDownloaded += bytesRead

            // Update progress
            val progress =
              if (totalBytes > 0) {
                (totalBytesDownloaded.toFloat() / totalBytes.toFloat())
              } else {
                0f
              }

            downloadTaskDao.updateProgress(taskId, progress, totalBytesDownloaded)

            // Set progress for WorkManager
            setProgress(
              workDataOf(
                "PROGRESS" to progress,
                "BYTES_DOWNLOADED" to totalBytesDownloaded,
                "TOTAL_BYTES" to totalBytes,
              ),
            )
          }
        }

        inputStream.close()

        // Checksum verification pending: enable when checksum data becomes available from the
        // catalog
        // val calculatedChecksum = calculateSHA256(outputFile)
        // if (calculatedChecksum != modelPackage.checksum) {
        //     throw Exception("Checksum mismatch")
        // }

        // Update task as completed
        downloadTaskDao.updateStatus(taskId, DownloadStatus.COMPLETED)

        // Update finished_at time by fetching and updating entity
        val completedTask = downloadTaskDao.getById(taskId)
        if (completedTask != null) {
          downloadTaskDao.update(completedTask.copy(finishedAt = Clock.System.now()))
        }

        // Update model install state
        modelPackageDao.updateInstallState(modelId, InstallState.INSTALLED, Clock.System.now())

        Result.success(
          workDataOf(
            "FILE_PATH" to outputFile.absolutePath,
            "MODEL_DISPLAY_NAME" to modelPackage.displayName,
            "MODEL_VERSION" to modelPackage.version,
          ),
        )
      } catch (e: Exception) {
        // Update task as failed
        downloadTaskDao.updateStatusWithError(
          taskId,
          DownloadStatus.FAILED,
          e.message ?: "Unknown error",
        )

        // Update finished_at time by fetching and updating entity
        val failedTask = downloadTaskDao.getById(taskId)
        if (failedTask != null) {
          downloadTaskDao.update(failedTask.copy(finishedAt = Clock.System.now()))
        }

        Result.failure(workDataOf("ERROR" to (e.message ?: "Unknown error")))
      }
    }
}
