package com.vjaykrsna.nanoai.core.di

import com.vjaykrsna.nanoai.core.network.AndroidConnectivityStatusProvider
import com.vjaykrsna.nanoai.core.network.ConnectivityStatusProvider
import com.vjaykrsna.nanoai.model.catalog.network.ModelCatalogService
import com.vjaykrsna.nanoai.model.huggingface.network.HuggingFaceAccountService
import com.vjaykrsna.nanoai.model.huggingface.network.HuggingFaceOAuthService
import com.vjaykrsna.nanoai.model.huggingface.network.HuggingFaceService
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
    private const val MODEL_CATALOG_BASE_URL = "https://api.nanoai.app/"
    private const val HUGGING_FACE_BASE_URL = "https://huggingface.co/"
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
        .baseUrl(MODEL_CATALOG_BASE_URL)
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
        .baseUrl(HUGGING_FACE_BASE_URL)
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
