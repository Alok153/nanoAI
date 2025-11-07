package com.vjaykrsna.nanoai.core.di

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.data.audio.AudioRepositoryImpl
import com.vjaykrsna.nanoai.core.data.library.catalog.ModelManifestRepository
import com.vjaykrsna.nanoai.core.data.library.catalog.ModelManifestRepositoryImpl
import com.vjaykrsna.nanoai.core.data.library.export.ExportServiceImpl
import com.vjaykrsna.nanoai.core.data.library.huggingface.HuggingFaceCatalogRepository
import com.vjaykrsna.nanoai.core.data.library.huggingface.HuggingFaceCatalogRepositoryImpl
import com.vjaykrsna.nanoai.core.data.library.impl.DownloadManagerImpl
import com.vjaykrsna.nanoai.core.data.library.impl.ModelCatalogRepositoryImpl
import com.vjaykrsna.nanoai.core.data.repository.impl.ApiProviderConfigRepositoryImpl
import com.vjaykrsna.nanoai.core.data.repository.impl.ConversationRepositoryImpl
import com.vjaykrsna.nanoai.core.data.repository.impl.InferencePreferenceRepositoryImpl
import com.vjaykrsna.nanoai.core.data.repository.impl.PersonaRepositoryImpl
import com.vjaykrsna.nanoai.core.data.repository.impl.PersonaSwitchLogRepositoryImpl
import com.vjaykrsna.nanoai.core.data.repository.impl.UserProfileRepositoryImpl
import com.vjaykrsna.nanoai.core.data.settings.backup.ImportServiceImpl
import com.vjaykrsna.nanoai.core.data.uiux.ConnectivityRepositoryImpl
import com.vjaykrsna.nanoai.core.data.uiux.NavigationRepositoryImpl
import com.vjaykrsna.nanoai.core.data.uiux.ProgressRepositoryImpl
import com.vjaykrsna.nanoai.core.data.uiux.ThemeRepositoryImpl
import com.vjaykrsna.nanoai.core.domain.audio.AudioRepository
import com.vjaykrsna.nanoai.core.domain.library.DownloadManager
import com.vjaykrsna.nanoai.core.domain.library.ExportService
import com.vjaykrsna.nanoai.core.domain.library.ModelCatalogRepository
import com.vjaykrsna.nanoai.core.domain.repository.ApiProviderConfigRepository
import com.vjaykrsna.nanoai.core.domain.repository.ConversationRepository
import com.vjaykrsna.nanoai.core.domain.repository.InferencePreferenceRepository
import com.vjaykrsna.nanoai.core.domain.repository.PersonaRepository
import com.vjaykrsna.nanoai.core.domain.repository.PersonaSwitchLogRepository
import com.vjaykrsna.nanoai.core.domain.repository.UserProfileRepository
import com.vjaykrsna.nanoai.core.domain.settings.ImportService
import com.vjaykrsna.nanoai.core.domain.uiux.ProgressCenterCoordinator
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

/** Binds repositories associated with conversations and personas. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ConversationRepositoryModule {
  @Binds
  @Singleton
  abstract fun bindConversationRepository(impl: ConversationRepositoryImpl): ConversationRepository

  @Binds
  @Singleton
  abstract fun bindPersonaRepository(impl: PersonaRepositoryImpl): PersonaRepository

  @Binds
  @Singleton
  abstract fun bindPersonaSwitchLogRepository(
    impl: PersonaSwitchLogRepositoryImpl
  ): PersonaSwitchLogRepository
}

/** Binds repositories responsible for inference and provider preferences. */
@Module
@InstallIn(SingletonComponent::class)
abstract class PreferenceRepositoryModule {
  @Binds
  @Singleton
  abstract fun bindInferencePreferenceRepository(
    impl: InferencePreferenceRepositoryImpl
  ): InferencePreferenceRepository

  @Binds
  @Singleton
  abstract fun bindApiProviderConfigRepository(
    impl: ApiProviderConfigRepositoryImpl
  ): ApiProviderConfigRepository
}

/** Binds repositories serving the model catalog and download flows. */
@Module
@InstallIn(SingletonComponent::class)
abstract class LibraryRepositoryModule {
  @Binds
  @Singleton
  abstract fun bindModelCatalogRepository(impl: ModelCatalogRepositoryImpl): ModelCatalogRepository

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

/** Binds import/export services used for library maintenance. */
@Module
@InstallIn(SingletonComponent::class)
abstract class LibraryMaintenanceModule {
  @Binds @Singleton abstract fun bindExportService(impl: ExportServiceImpl): ExportService

  @Binds @Singleton abstract fun bindImportService(impl: ImportServiceImpl): ImportService
}

/** Binds repositories powering shell UI/UX coordination. */
@Module
@InstallIn(SingletonComponent::class)
abstract class UiUxRepositoryModule {
  @Binds
  @Singleton
  abstract fun bindUserProfileRepository(impl: UserProfileRepositoryImpl): UserProfileRepository

  @Binds
  @Singleton
  abstract fun bindNavigationRepository(
    impl: NavigationRepositoryImpl
  ): com.vjaykrsna.nanoai.core.domain.repository.NavigationRepository

  @Binds
  @Singleton
  abstract fun bindConnectivityRepository(
    impl: ConnectivityRepositoryImpl
  ): com.vjaykrsna.nanoai.core.domain.repository.ConnectivityRepository

  @Binds
  @Singleton
  abstract fun bindThemeRepository(
    impl: ThemeRepositoryImpl
  ): com.vjaykrsna.nanoai.core.domain.repository.ThemeRepository

  @Binds
  @Singleton
  abstract fun bindProgressRepository(
    impl: ProgressRepositoryImpl
  ): com.vjaykrsna.nanoai.core.domain.repository.ProgressRepository
}

/** Binds cross-cutting repositories such as audio capture. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AudioRepositoryModule {
  @Binds @Singleton abstract fun bindAudioRepository(impl: AudioRepositoryImpl): AudioRepository
}

/** Provides handcrafted coordinators that stitch multiple repositories together. */
@Module
@InstallIn(SingletonComponent::class)
object ProgressCoordinatorModule {
  @Provides
  @Singleton
  fun provideProgressCenterCoordinator(
    downloadManager: DownloadManager,
    progressRepository: com.vjaykrsna.nanoai.core.domain.repository.ProgressRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
  ): ProgressCenterCoordinator =
    ProgressCenterCoordinator(downloadManager, progressRepository, ioDispatcher)
}

/** Legacy aggregation module retained for tests that replace RepositoryModule wholesale. */
@Module(
  includes =
    [
      ConversationRepositoryModule::class,
      PreferenceRepositoryModule::class,
      LibraryRepositoryModule::class,
      LibraryMaintenanceModule::class,
      UiUxRepositoryModule::class,
      AudioRepositoryModule::class,
      ProgressCoordinatorModule::class,
    ]
)
@InstallIn(SingletonComponent::class)
object RepositoryModule
