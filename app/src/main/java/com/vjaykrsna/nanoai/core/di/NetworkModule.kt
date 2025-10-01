package com.vjaykrsna.nanoai.core.di

import com.vjaykrsna.nanoai.core.network.AndroidConnectivityStatusProvider
import com.vjaykrsna.nanoai.core.network.ConnectivityStatusProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json

/** Provides shared network-layer dependencies. */
@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    @Singleton
    abstract fun bindConnectivityStatusProvider(
        impl: AndroidConnectivityStatusProvider
    ): ConnectivityStatusProvider

    companion object {
        @Provides
        @Singleton
        fun provideJson(): Json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }
    }
}
