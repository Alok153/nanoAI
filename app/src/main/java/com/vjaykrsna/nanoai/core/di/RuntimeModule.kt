package com.vjaykrsna.nanoai.core.di

import com.vjaykrsna.nanoai.core.device.AndroidDeviceIdentityProvider
import com.vjaykrsna.nanoai.core.device.DeviceIdentityProvider
import com.vjaykrsna.nanoai.core.runtime.DefaultLocalRuntimeGateway
import com.vjaykrsna.nanoai.core.runtime.LocalModelRuntime
import com.vjaykrsna.nanoai.core.runtime.LocalModelRuntimeImpl
import com.vjaykrsna.nanoai.core.runtime.LocalRuntimeGateway
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.datetime.Clock

/** Provides on-device runtime bindings. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RuntimeModule {
  @Binds
  @Singleton
  abstract fun bindLocalModelRuntime(impl: LocalModelRuntimeImpl): LocalModelRuntime

  @Binds
  @Singleton
  abstract fun bindLocalRuntimeGateway(impl: DefaultLocalRuntimeGateway): LocalRuntimeGateway

  @Binds
  @Singleton
  abstract fun bindDeviceIdentityProvider(
    impl: AndroidDeviceIdentityProvider
  ): DeviceIdentityProvider

  companion object {
    @Provides @Singleton fun provideClock(): Clock = Clock.System
  }
}
