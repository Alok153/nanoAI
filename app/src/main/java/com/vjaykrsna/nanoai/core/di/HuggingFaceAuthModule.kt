package com.vjaykrsna.nanoai.core.di

import com.vjaykrsna.nanoai.security.HuggingFaceCredentialRepository
import com.vjaykrsna.nanoai.security.HuggingFaceTokenProvider
import dagger.Binds
import dagger.Module
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
    repository: HuggingFaceCredentialRepository,
  ): HuggingFaceTokenProvider
}
