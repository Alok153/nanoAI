package com.vjaykrsna.nanoai.testing.di

import com.vjaykrsna.nanoai.core.data.repository.ConversationRepository
import com.vjaykrsna.nanoai.core.data.repository.PersonaRepository
import com.vjaykrsna.nanoai.core.di.RepositoryModule
import com.vjaykrsna.nanoai.feature.library.data.ModelCatalogRepository
import com.vjaykrsna.nanoai.testing.FakeConversationRepository
import com.vjaykrsna.nanoai.testing.FakeModelCatalogRepository
import com.vjaykrsna.nanoai.testing.FakePersonaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [RepositoryModule::class])
abstract class TestRepositoryModule {

  @Binds
  @Singleton
  abstract fun bindConversationRepository(impl: FakeConversationRepository): ConversationRepository

  @Binds
  @Singleton
  abstract fun bindPersonaRepository(impl: FakePersonaRepository): PersonaRepository

  @Binds
  @Singleton
  abstract fun bindModelCatalogRepository(impl: FakeModelCatalogRepository): ModelCatalogRepository
}
