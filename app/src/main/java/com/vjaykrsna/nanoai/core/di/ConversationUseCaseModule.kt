package com.vjaykrsna.nanoai.core.di

import com.vjaykrsna.nanoai.core.domain.chat.ConversationUseCase
import com.vjaykrsna.nanoai.core.domain.chat.ConversationUseCaseInterface
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface ConversationUseCaseModule {
  @Binds fun bindConversationUseCase(impl: ConversationUseCase): ConversationUseCaseInterface
}
