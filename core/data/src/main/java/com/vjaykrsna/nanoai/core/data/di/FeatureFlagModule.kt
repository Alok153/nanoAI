package com.vjaykrsna.nanoai.core.data.di

import com.vjaykrsna.nanoai.core.data.usecase.BuildConfigFeatureFlagEnvironment
import com.vjaykrsna.nanoai.core.domain.usecase.FeatureFlagEnvironment
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class FeatureFlagModule {
  @Binds
  abstract fun bindFeatureFlagEnvironment(
    impl: BuildConfigFeatureFlagEnvironment
  ): FeatureFlagEnvironment
}
