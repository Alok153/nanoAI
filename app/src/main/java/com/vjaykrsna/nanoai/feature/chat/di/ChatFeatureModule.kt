package com.vjaykrsna.nanoai.feature.chat.di

import com.vjaykrsna.nanoai.feature.chat.domain.ChatFeatureCoordinator
import com.vjaykrsna.nanoai.feature.chat.domain.DefaultChatFeatureCoordinator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ChatFeatureModule {
  @Binds
  @Singleton
  abstract fun bindChatFeatureCoordinator(
    impl: DefaultChatFeatureCoordinator
  ): ChatFeatureCoordinator
}
