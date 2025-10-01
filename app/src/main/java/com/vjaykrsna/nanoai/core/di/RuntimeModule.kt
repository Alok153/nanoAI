package com.vjaykrsna.nanoai.core.di

import com.vjaykrsna.nanoai.core.runtime.LocalModelRuntime
import com.vjaykrsna.nanoai.core.runtime.MediaPipeLocalModelRuntime
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides on-device runtime bindings. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RuntimeModule {
    @Binds
    @Singleton
    abstract fun bindLocalModelRuntime(impl: MediaPipeLocalModelRuntime): LocalModelRuntime
}
