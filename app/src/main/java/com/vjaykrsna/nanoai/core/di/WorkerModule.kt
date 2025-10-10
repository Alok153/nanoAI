package com.vjaykrsna.nanoai.core.di

import android.content.Context
import androidx.work.WorkManager
import com.vjaykrsna.nanoai.core.network.huggingface.HuggingFaceAuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
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
  fun provideOkHttpClient(
    authInterceptor: com.vjaykrsna.nanoai.core.network.huggingface.HuggingFaceAuthInterceptor,
  ): OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
      .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
      .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
      .addInterceptor(authInterceptor)
      .build()
}
