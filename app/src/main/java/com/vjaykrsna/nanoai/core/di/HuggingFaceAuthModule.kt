package com.vjaykrsna.nanoai.core.di

import com.vjaykrsna.nanoai.BuildConfig
import com.vjaykrsna.nanoai.core.data.settings.huggingface.HuggingFaceAuthCoordinatorImpl
import com.vjaykrsna.nanoai.core.domain.settings.huggingface.HuggingFaceAuthCoordinator
import com.vjaykrsna.nanoai.core.domain.settings.huggingface.HuggingFaceOAuthConfig
import com.vjaykrsna.nanoai.core.security.HuggingFaceCredentialRepository
import com.vjaykrsna.nanoai.core.security.HuggingFaceCredentialRepositoryImpl
import com.vjaykrsna.nanoai.core.security.HuggingFaceTokenProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides Hugging Face authentication bindings. */
@Module
@InstallIn(SingletonComponent::class)
abstract class HuggingFaceAuthModule {
  @Binds
  @Singleton
  abstract fun bindHuggingFaceTokenProvider(
    repository: HuggingFaceCredentialRepositoryImpl
  ): HuggingFaceTokenProvider

  @Binds
  @Singleton
  abstract fun bindHuggingFaceCredentialRepository(
    impl: HuggingFaceCredentialRepositoryImpl
  ): HuggingFaceCredentialRepository

  @Binds
  @Singleton
  abstract fun bindHuggingFaceAuthCoordinator(
    impl: HuggingFaceAuthCoordinatorImpl
  ): HuggingFaceAuthCoordinator

  companion object {
    private const val DEFAULT_SCOPE = "all offline_access"

    @Provides
    @Singleton
    fun provideHuggingFaceOAuthConfig(): HuggingFaceOAuthConfig =
      HuggingFaceOAuthConfig(
        clientId = BuildConfig.HF_OAUTH_CLIENT_ID,
        scope = BuildConfig.HF_OAUTH_SCOPE.ifBlank { DEFAULT_SCOPE },
      )
  }
}
