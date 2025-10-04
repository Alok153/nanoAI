package com.vjaykrsna.nanoai.core.di

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.vjaykrsna.nanoai.feature.uiux.data.SyncUiStateWorker
import com.vjaykrsna.nanoai.feature.uiux.data.UiStateSyncScheduler
import com.vjaykrsna.nanoai.feature.uiux.domain.UIUX_DEFAULT_USER_ID
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
import okhttp3.OkHttpClient

private const val CONNECT_TIMEOUT_SECONDS = 30L
private const val READ_TIMEOUT_SECONDS = 60L
private const val WRITE_TIMEOUT_SECONDS = 60L

/** Hilt module providing WorkManager and related dependencies. */
@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {
  @Provides
  @Singleton
  fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
    WorkManager.getInstance(context)

  @Provides
  @Singleton
  fun provideOkHttpClient(): OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
      .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
      .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
      .build()

  @Provides
  @Singleton
  @Named("UiStateSyncConstraints")
  fun provideUiStateSyncConstraints(): Constraints =
    Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .setRequiresBatteryNotLow(true)
      .build()

  @Provides
  @Singleton
  @Named("UiStateSyncRequest")
  fun provideUiStateSyncPeriodicRequest(
    @Named("UiStateSyncConstraints") constraints: Constraints,
  ): PeriodicWorkRequest {
    return PeriodicWorkRequestBuilder<SyncUiStateWorker>(
        SyncUiStateWorker.REPEAT_INTERVAL_HOURS,
        TimeUnit.HOURS,
      )
      .setConstraints(constraints)
      .setInputData(
        workDataOf(SyncUiStateWorker.KEY_USER_ID to UIUX_DEFAULT_USER_ID),
      )
      .build()
  }

  @Provides
  @Singleton
  fun provideUiStateSyncScheduler(
    workManager: WorkManager,
    @Named("UiStateSyncRequest") syncRequest: PeriodicWorkRequest,
  ): UiStateSyncScheduler {
    return UiStateSyncScheduler(workManager).apply { ensurePeriodicSync(syncRequest) }
  }
}
