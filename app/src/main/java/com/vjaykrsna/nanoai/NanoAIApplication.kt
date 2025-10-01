package com.vjaykrsna.nanoai

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class for nanoAI.
 *
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection throughout the app, including
 * WorkManager workers.
 */
@HiltAndroidApp
class NanoAIApplication :
    Application(),
    Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}
