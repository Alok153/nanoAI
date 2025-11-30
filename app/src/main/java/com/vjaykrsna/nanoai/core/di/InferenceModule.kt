package com.vjaykrsna.nanoai.core.di

import com.vjaykrsna.nanoai.core.data.chat.InferenceOrchestrator
import com.vjaykrsna.nanoai.core.domain.chat.PromptInferenceGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InferenceModule {
  @Binds
  @Singleton
  abstract fun bindPromptInferenceGateway(impl: InferenceOrchestrator): PromptInferenceGateway
}
