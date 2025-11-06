package com.vjaykrsna.nanoai.core.data.library.catalog

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides bindings for model catalog data sources. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class ModelCatalogModule {
  @Binds
  @Singleton
  abstract fun bindModelCatalogSource(impl: AssetModelCatalogSource): ModelCatalogSource
}
