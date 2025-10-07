package com.vjaykrsna.nanoai.core.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {
  // Previously provided WelcomeAnalytics; onboarding removed so provide no-op analytics via other
  // modules as needed.
}
