package com.vjaykrsna.nanoai

import android.app.Application
import android.content.IntentFilter
import com.vjaykrsna.nanoai.core.common.DownloadNotificationReceiver
import com.vjaykrsna.nanoai.core.common.NotificationHelper

/**
 * Base application class for nanoAI.
 *
 * Contains common application setup without Hilt annotations.
 */
abstract class BaseApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    registerDownloadNotificationReceiver()
  }

  protected fun registerDownloadNotificationReceiver() {
    val receiver = DownloadNotificationReceiver()
    val filter =
      IntentFilter().apply {
        addAction(NotificationHelper.ACTION_RESUME_DOWNLOAD)
        addAction(NotificationHelper.ACTION_PAUSE_DOWNLOAD)
        addAction(NotificationHelper.ACTION_CANCEL_DOWNLOAD)
      }
    registerReceiver(receiver, filter, RECEIVER_EXPORTED)
  }
}
