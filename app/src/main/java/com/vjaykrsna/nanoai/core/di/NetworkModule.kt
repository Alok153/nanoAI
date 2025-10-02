package com.vjaykrsna.nanoai.core.di

import com.vjaykrsna.nanoai.core.network.AndroidConnectivityStatusProvider
import com.vjaykrsna.nanoai.core.network.ConnectivityStatusProvider
import com.vjaykrsna.nanoai.core.network.UserProfileService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Named
import javax.inject.Singleton

/** Provides shared network-layer dependencies. */
@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {
    @Binds
    @Singleton
    abstract fun bindConnectivityStatusProvider(impl: AndroidConnectivityStatusProvider): ConnectivityStatusProvider

    companion object {
        private const val USER_PROFILE_BASE_URL = "https://api.nanoai.dev/"
        private val jsonMediaType = "application/json".toMediaType()

        @Provides
        @Singleton
        fun provideJson(): Json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                explicitNulls = false
            }

        @Provides
        @Singleton
        @Named("UserProfile")
        fun provideUserProfileRetrofit(
            json: Json,
            okHttpClient: OkHttpClient,
        ): Retrofit =
            Retrofit
                .Builder()
                .baseUrl(USER_PROFILE_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(json.asConverterFactory(jsonMediaType))
                .build()

        @Provides
        @Singleton
        fun provideUserProfileService(
            @Named("UserProfile") retrofit: Retrofit,
        ): UserProfileService = retrofit.create(UserProfileService::class.java)
    }
}
