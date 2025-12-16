package com.vjaykrsna.nanoai.core.di

import com.vjaykrsna.nanoai.core.device.ConnectivityObserver
import com.vjaykrsna.nanoai.core.device.SharedFlowConnectivityObserver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DeviceModule {
  @Binds
  @Singleton
  fun bindConnectivityObserver(impl: SharedFlowConnectivityObserver): ConnectivityObserver
}
