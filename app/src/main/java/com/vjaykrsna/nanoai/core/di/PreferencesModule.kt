package com.vjaykrsna.nanoai.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.vjaykrsna.nanoai.feature.uiux.domain.CommandPaletteActionProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides DataStore-backed UI preference dependencies. */
@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

  private val Context.modelCatalogDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "model_catalog")

  @Provides
  @Singleton
  fun provideModelCatalogDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
    context.modelCatalogDataStore

  @Provides
  @Singleton
  fun provideCommandPaletteActionProvider(): CommandPaletteActionProvider =
    CommandPaletteActionProvider()
}
