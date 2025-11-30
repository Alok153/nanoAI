package com.vjaykrsna.nanoai.core.common

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.vjaykrsna.nanoai.MainActivity
import com.vjaykrsna.nanoai.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(@ApplicationContext private val context: Context) {

  companion object {
    const val CHANNEL_ID_BACKGROUND_TASKS = "work_manager_channel"
    const val NOTIFICATION_ID_DOWNLOAD_PROGRESS = 1001
    const val NOTIFICATION_ID_DOWNLOAD_COMPLETE = 1002
    const val NOTIFICATION_ID_DOWNLOAD_FAILED = 1003

    // Action constants
    const val ACTION_RESUME_DOWNLOAD = "com.vjaykrsna.nanoai.ACTION_RESUME_DOWNLOAD"
    const val ACTION_PAUSE_DOWNLOAD = "com.vjaykrsna.nanoai.ACTION_PAUSE_DOWNLOAD"
    const val ACTION_CANCEL_DOWNLOAD = "com.vjaykrsna.nanoai.ACTION_CANCEL_DOWNLOAD"
    const val EXTRA_TASK_ID = "task_id"
    const val EXTRA_MODEL_ID = "model_id"

    // Request code offsets for pending intents
    private const val REQUEST_CODE_RESUME_OFFSET = 1
    private const val REQUEST_CODE_PAUSE_OFFSET = 2
    private const val REQUEST_CODE_CANCEL_OFFSET = 3
    private const val REQUEST_CODE_ACTIVITY = 0

    // Progress notification constants
    private const val PROGRESS_MAX_VALUE = 100

    // Size formatting constants
    private const val BYTES_PER_MB = 1_000_000.0
  }

  private val notificationManager = NotificationManagerCompat.from(context)

  init {
    createNotificationChannel()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
        NotificationChannel(
            CHANNEL_ID_BACKGROUND_TASKS,
            context.getString(R.string.notification_channel_background_tasks),
            NotificationManager.IMPORTANCE_DEFAULT,
          )
          .apply {
            description =
              context.getString(R.string.notification_channel_background_tasks_description)
          }
      val manager = context.getSystemService(NotificationManager::class.java)
      manager.createNotificationChannel(channel)
    }
  }

  private fun createActionIntent(action: String, taskId: String, modelId: String): Intent =
    Intent(action).apply {
      putExtra(EXTRA_TASK_ID, taskId)
      putExtra(EXTRA_MODEL_ID, modelId)
    }

  private fun createPendingIntent(intent: Intent, requestCode: Int): PendingIntent =
    PendingIntent.getBroadcast(
      context,
      requestCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

  private fun createActivityPendingIntent(): PendingIntent {
    val openAppIntent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      }
    return PendingIntent.getActivity(
      context,
      REQUEST_CODE_ACTIVITY,
      openAppIntent,
      PendingIntent.FLAG_IMMUTABLE,
    )
  }

  fun buildProgressNotification(
    modelName: String,
    progress: Int,
    taskId: String,
    modelId: String,
    bytesDownloaded: Long = 0L,
    totalBytes: Long = 0L,
  ): Notification {
    val resumeIntent = createActionIntent(ACTION_RESUME_DOWNLOAD, taskId, modelId)
    val pauseIntent = createActionIntent(ACTION_PAUSE_DOWNLOAD, taskId, modelId)
    val cancelIntent = createActionIntent(ACTION_CANCEL_DOWNLOAD, taskId, modelId)

    val resumePendingIntent =
      createPendingIntent(resumeIntent, taskId.hashCode() + REQUEST_CODE_RESUME_OFFSET)
    val pausePendingIntent =
      createPendingIntent(pauseIntent, taskId.hashCode() + REQUEST_CODE_PAUSE_OFFSET)
    val cancelPendingIntent =
      createPendingIntent(cancelIntent, taskId.hashCode() + REQUEST_CODE_CANCEL_OFFSET)
    val openAppPendingIntent = createActivityPendingIntent()

    // Build progress text with size info if available
    val progressText =
      if (totalBytes > 0L) {
        val downloadedMb = bytesDownloaded / BYTES_PER_MB
        val totalMb = totalBytes / BYTES_PER_MB
        context.getString(
          R.string.notification_download_progress_with_size,
          progress,
          downloadedMb,
          totalMb,
        )
      } else {
        context.getString(R.string.notification_download_progress, progress)
      }

    return NotificationCompat.Builder(context, CHANNEL_ID_BACKGROUND_TASKS)
      .setSmallIcon(android.R.drawable.stat_sys_download)
      .setContentTitle(context.getString(R.string.notification_download_title, modelName))
      .setContentText(progressText)
      .setProgress(PROGRESS_MAX_VALUE, progress, false)
      .setOngoing(true)
      .setContentIntent(openAppPendingIntent)
      .addAction(
        android.R.drawable.ic_media_play,
        context.getString(R.string.notification_action_resume),
        resumePendingIntent,
      )
      .addAction(
        android.R.drawable.ic_media_pause,
        context.getString(R.string.notification_action_pause),
        pausePendingIntent,
      )
      .addAction(
        android.R.drawable.ic_menu_close_clear_cancel,
        context.getString(R.string.notification_action_cancel),
        cancelPendingIntent,
      )
      .build()
  }

  fun buildCompletionNotification(modelName: String): Notification {
    val openAppPendingIntent = createActivityPendingIntent()

    return NotificationCompat.Builder(context, CHANNEL_ID_BACKGROUND_TASKS)
      .setSmallIcon(android.R.drawable.stat_sys_download_done)
      .setContentTitle(context.getString(R.string.notification_download_complete_title))
      .setContentText(context.getString(R.string.notification_download_complete_message, modelName))
      .setAutoCancel(true)
      .setContentIntent(openAppPendingIntent)
      .build()
  }

  fun buildFailureNotification(modelName: String, errorMessage: String): Notification {
    val openAppPendingIntent = createActivityPendingIntent()

    return NotificationCompat.Builder(context, CHANNEL_ID_BACKGROUND_TASKS)
      .setSmallIcon(android.R.drawable.stat_notify_error)
      .setContentTitle(context.getString(R.string.notification_download_failed_title))
      .setContentText(
        context.getString(R.string.notification_download_failed_message, modelName, errorMessage)
      )
      .setAutoCancel(true)
      .setContentIntent(openAppPendingIntent)
      .build()
  }

  fun notifyProgress(notification: Notification) {
    if (!canPostNotifications()) return
    notificationManager.notify(NOTIFICATION_ID_DOWNLOAD_PROGRESS, notification)
  }

  fun notifyCompletion(notification: Notification) {
    if (!canPostNotifications()) return
    notificationManager.notify(NOTIFICATION_ID_DOWNLOAD_COMPLETE, notification)
  }

  fun notifyFailure(notification: Notification) {
    if (!canPostNotifications()) return
    notificationManager.notify(NOTIFICATION_ID_DOWNLOAD_FAILED, notification)
  }

  fun cancelProgressNotification() {
    notificationManager.cancel(NOTIFICATION_ID_DOWNLOAD_PROGRESS)
  }

  private fun canPostNotifications(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      return true
    }
    val permissionState =
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
    return permissionState == PackageManager.PERMISSION_GRANTED
  }
}
