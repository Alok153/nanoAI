package com.vjaykrsna.nanoai.core.di

import com.vjaykrsna.nanoai.feature.uiux.presentation.WelcomeAnalytics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {
    @Provides
    @Singleton
    fun provideWelcomeAnalytics(): WelcomeAnalytics = WelcomeAnalytics.NoOp
}
