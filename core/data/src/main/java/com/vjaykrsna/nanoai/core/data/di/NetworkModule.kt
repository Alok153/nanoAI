package com.vjaykrsna.nanoai.core.data.di

import com.vjaykrsna.nanoai.core.data.BuildConfig
import com.vjaykrsna.nanoai.core.data.library.catalog.network.ModelCatalogService
import com.vjaykrsna.nanoai.core.data.library.huggingface.network.HuggingFaceAccountService
import com.vjaykrsna.nanoai.core.data.library.huggingface.network.HuggingFaceOAuthService
import com.vjaykrsna.nanoai.core.data.library.huggingface.network.HuggingFaceService
import com.vjaykrsna.nanoai.core.network.AndroidConnectivityStatusProvider
import com.vjaykrsna.nanoai.core.network.ConnectivityStatusProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

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
    private val jsonMediaType = "application/json".toMediaType()

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
      ignoreUnknownKeys = true
      encodeDefaults = true
      explicitNulls = false
    }

    @Provides
    @Singleton
    @Named("ModelCatalog")
    fun provideModelCatalogRetrofit(json: Json, okHttpClient: OkHttpClient): Retrofit =
      Retrofit.Builder()
        .baseUrl(BuildConfig.MODEL_CATALOG_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory(jsonMediaType))
        .build()

    @Provides
    @Singleton
    fun provideModelCatalogService(@Named("ModelCatalog") retrofit: Retrofit): ModelCatalogService =
      retrofit.create(ModelCatalogService::class.java)

    @Provides
    @Singleton
    @Named("HuggingFace")
    fun provideHuggingFaceRetrofit(json: Json, okHttpClient: OkHttpClient): Retrofit =
      Retrofit.Builder()
        .baseUrl(BuildConfig.HUGGING_FACE_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory(jsonMediaType))
        .build()

    @Provides
    @Singleton
    fun provideHuggingFaceService(@Named("HuggingFace") retrofit: Retrofit): HuggingFaceService =
      retrofit.create(HuggingFaceService::class.java)

    @Provides
    @Singleton
    fun provideHuggingFaceAccountService(
      @Named("HuggingFace") retrofit: Retrofit
    ): HuggingFaceAccountService = retrofit.create(HuggingFaceAccountService::class.java)

    @Provides
    @Singleton
    fun provideHuggingFaceOAuthService(
      @Named("HuggingFace") retrofit: Retrofit
    ): HuggingFaceOAuthService = retrofit.create(HuggingFaceOAuthService::class.java)
  }
}
