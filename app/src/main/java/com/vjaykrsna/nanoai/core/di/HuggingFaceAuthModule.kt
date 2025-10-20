package com.vjaykrsna.nanoai.core.di

import com.vjaykrsna.nanoai.BuildConfig
import com.vjaykrsna.nanoai.feature.settings.domain.huggingface.HuggingFaceOAuthConfig
import com.vjaykrsna.nanoai.security.HuggingFaceCredentialRepository
import com.vjaykrsna.nanoai.security.HuggingFaceTokenProvider
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
    repository: HuggingFaceCredentialRepository
  ): HuggingFaceTokenProvider

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
