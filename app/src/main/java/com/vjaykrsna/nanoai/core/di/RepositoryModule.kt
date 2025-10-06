package com.vjaykrsna.nanoai.core.di

import com.vjaykrsna.nanoai.core.data.repository.ApiProviderConfigRepository
import com.vjaykrsna.nanoai.core.data.repository.ConversationRepository
import com.vjaykrsna.nanoai.core.data.repository.InferencePreferenceRepository
import com.vjaykrsna.nanoai.core.data.repository.PersonaRepository
import com.vjaykrsna.nanoai.core.data.repository.PersonaSwitchLogRepository
import com.vjaykrsna.nanoai.core.data.repository.UserProfileRepository
import com.vjaykrsna.nanoai.core.data.repository.impl.ApiProviderConfigRepositoryImpl
import com.vjaykrsna.nanoai.core.data.repository.impl.ConversationRepositoryImpl
import com.vjaykrsna.nanoai.core.data.repository.impl.InferencePreferenceRepositoryImpl
import com.vjaykrsna.nanoai.core.data.repository.impl.PersonaRepositoryImpl
import com.vjaykrsna.nanoai.core.data.repository.impl.PersonaSwitchLogRepositoryImpl
import com.vjaykrsna.nanoai.core.data.repository.impl.UserProfileRepositoryImpl
import com.vjaykrsna.nanoai.feature.library.data.DownloadManager
import com.vjaykrsna.nanoai.feature.library.data.ModelCatalogRepository
import com.vjaykrsna.nanoai.feature.library.data.export.ExportServiceImpl
import com.vjaykrsna.nanoai.feature.library.data.impl.DownloadManagerImpl
import com.vjaykrsna.nanoai.feature.library.data.impl.ModelCatalogRepositoryImpl
import com.vjaykrsna.nanoai.feature.library.domain.ExportService
import com.vjaykrsna.nanoai.feature.settings.data.backup.ImportServiceImpl
import com.vjaykrsna.nanoai.feature.settings.domain.ImportService
import com.vjaykrsna.nanoai.model.catalog.ModelManifestRepository
import com.vjaykrsna.nanoai.model.catalog.ModelManifestRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing repository implementations.
 *
 * Binds repository interfaces to their concrete implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
  @Binds
  @Singleton
  abstract fun bindConversationRepository(
    impl: ConversationRepositoryImpl,
  ): ConversationRepository

  @Binds
  @Singleton
  abstract fun bindPersonaRepository(
    impl: PersonaRepositoryImpl,
  ): PersonaRepository

  @Binds
  @Singleton
  abstract fun bindPersonaSwitchLogRepository(
    impl: PersonaSwitchLogRepositoryImpl,
  ): PersonaSwitchLogRepository

  @Binds
  @Singleton
  abstract fun bindInferencePreferenceRepository(
    impl: InferencePreferenceRepositoryImpl,
  ): InferencePreferenceRepository

  @Binds
  @Singleton
  abstract fun bindApiProviderConfigRepository(
    impl: ApiProviderConfigRepositoryImpl,
  ): ApiProviderConfigRepository

  @Binds
  @Singleton
  abstract fun bindModelCatalogRepository(
    impl: ModelCatalogRepositoryImpl,
  ): ModelCatalogRepository

  @Binds
  @Singleton
  abstract fun bindModelManifestRepository(
    impl: ModelManifestRepositoryImpl,
  ): ModelManifestRepository

  @Binds
  @Singleton
  abstract fun bindDownloadManager(
    impl: DownloadManagerImpl,
  ): DownloadManager

  @Binds
  @Singleton
  abstract fun bindExportService(
    impl: ExportServiceImpl,
  ): ExportService

  @Binds
  @Singleton
  abstract fun bindImportService(
    impl: ImportServiceImpl,
  ): ImportService

  @Binds
  @Singleton
  abstract fun bindUserProfileRepository(
    impl: UserProfileRepositoryImpl,
  ): UserProfileRepository
}
