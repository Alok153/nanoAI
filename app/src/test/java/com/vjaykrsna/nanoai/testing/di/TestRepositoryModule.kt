package com.vjaykrsna.nanoai.testing.di

import com.vjaykrsna.nanoai.core.data.di.ConversationRepositoryModule
import com.vjaykrsna.nanoai.core.data.di.LibraryRepositoryModule
import com.vjaykrsna.nanoai.core.data.library.catalog.ModelManifestRepositoryImpl
import com.vjaykrsna.nanoai.core.data.library.huggingface.HuggingFaceCatalogRepositoryImpl
import com.vjaykrsna.nanoai.core.data.library.impl.DownloadManagerImpl
import com.vjaykrsna.nanoai.core.data.repository.impl.PersonaSwitchLogRepositoryImpl
import com.vjaykrsna.nanoai.core.domain.library.DownloadManager
import com.vjaykrsna.nanoai.core.domain.library.HuggingFaceCatalogRepository
import com.vjaykrsna.nanoai.core.domain.library.ModelCatalogRepository
import com.vjaykrsna.nanoai.core.domain.library.ModelManifestRepository
import com.vjaykrsna.nanoai.core.domain.repository.ConversationRepository
import com.vjaykrsna.nanoai.core.domain.repository.PersonaRepository
import com.vjaykrsna.nanoai.core.domain.repository.PersonaSwitchLogRepository
import com.vjaykrsna.nanoai.testing.FakeConversationRepository
import com.vjaykrsna.nanoai.testing.FakeModelCatalogRepository
import com.vjaykrsna.nanoai.testing.FakePersonaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
  components = [SingletonComponent::class],
  replaces = [ConversationRepositoryModule::class, LibraryRepositoryModule::class],
)
abstract class TestRepositoryModule {

  @Binds
  @Singleton
  abstract fun bindConversationRepository(impl: FakeConversationRepository): ConversationRepository

  @Binds
  @Singleton
  abstract fun bindPersonaRepository(impl: FakePersonaRepository): PersonaRepository

  @Binds
  @Singleton
  abstract fun bindPersonaSwitchLogRepository(
    impl: PersonaSwitchLogRepositoryImpl
  ): PersonaSwitchLogRepository

  @Binds
  @Singleton
  abstract fun bindModelCatalogRepository(impl: FakeModelCatalogRepository): ModelCatalogRepository

  @Binds
  @Singleton
  abstract fun bindModelManifestRepository(
    impl: ModelManifestRepositoryImpl
  ): ModelManifestRepository

  @Binds @Singleton abstract fun bindDownloadManager(impl: DownloadManagerImpl): DownloadManager

  @Binds
  @Singleton
  abstract fun bindHuggingFaceCatalogRepository(
    impl: HuggingFaceCatalogRepositoryImpl
  ): HuggingFaceCatalogRepository
}
