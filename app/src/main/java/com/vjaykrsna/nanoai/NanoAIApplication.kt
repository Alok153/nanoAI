package com.vjaykrsna.nanoai

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class for nanoAI.
 *
 * Extends [BaseApplication] to enable notification receiver registration for download actions
 * (Resume/Pause/Cancel). Annotated with @HiltAndroidApp to enable Hilt dependency injection
 * throughout the app, including WorkManager workers.
 */
@HiltAndroidApp
class NanoAIApplication : BaseApplication(), Configuration.Provider {
  @Inject lateinit var workerFactory: HiltWorkerFactory

  override val workManagerConfiguration: Configuration
    get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

  override fun onCreate() {
    super.onCreate()
    // Notification receiver registration is handled by BaseApplication.onCreate()
  }
}
