package com.vjaykrsna.nanoai.core.data.library.workers

import android.app.Notification

/**
 * Abstraction over app-level notification publishing so WorkManager workers can live in core:data
 * without depending on the application module.
 */
interface DownloadNotificationHelper {
  companion object {
    const val NOTIFICATION_ID_DOWNLOAD_PROGRESS = 1001
  }

  fun buildProgressNotification(
    modelName: String,
    progress: Int,
    taskId: String,
    modelId: String,
    bytesDownloaded: Long = 0L,
    totalBytes: Long = 0L,
  ): Notification

  fun buildCompletionNotification(modelName: String): Notification

  fun buildFailureNotification(modelName: String, errorMessage: String): Notification

  fun notifyProgress(notification: Notification)

  fun notifyCompletion(notification: Notification)

  fun notifyFailure(notification: Notification)

  fun cancelProgressNotification()
}
