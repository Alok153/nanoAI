package com.vjaykrsna.nanoai.core.di

import android.content.Context
import com.vjaykrsna.nanoai.core.data.preferences.UiPreferencesConverters
import com.vjaykrsna.nanoai.core.data.preferences.UiPreferencesStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/** Provides DataStore-backed UI preference dependencies. */
@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {
    @Provides
    @Singleton
    fun provideUiPreferencesConverters(json: Json): UiPreferencesConverters = UiPreferencesConverters(json)

    @Provides
    @Singleton
    fun provideUiPreferencesStore(
        @ApplicationContext context: Context,
        converters: UiPreferencesConverters,
    ): UiPreferencesStore = UiPreferencesStore(context, converters)
}
