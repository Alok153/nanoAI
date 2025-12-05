package com.vjaykrsna.nanoai.core.di

import com.vjaykrsna.nanoai.core.common.NotificationHelper
import com.vjaykrsna.nanoai.core.data.library.workers.DownloadNotificationHelper
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {
  @Binds
  @Singleton
  abstract fun bindDownloadNotificationHelper(impl: NotificationHelper): DownloadNotificationHelper
}
