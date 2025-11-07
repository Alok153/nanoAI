package com.vjaykrsna.nanoai.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.vjaykrsna.nanoai.core.data.db.NanoAIDatabase
import com.vjaykrsna.nanoai.core.data.db.daos.ApiProviderConfigDao
import com.vjaykrsna.nanoai.core.data.db.daos.ChatThreadDao
import com.vjaykrsna.nanoai.core.data.db.daos.LayoutSnapshotDao
import com.vjaykrsna.nanoai.core.data.db.daos.MessageDao
import com.vjaykrsna.nanoai.core.data.db.daos.PersonaProfileDao
import com.vjaykrsna.nanoai.core.data.db.daos.PersonaSwitchLogDao
import com.vjaykrsna.nanoai.core.data.db.daos.UIStateSnapshotDao
import com.vjaykrsna.nanoai.core.data.db.daos.UserProfileDao
import com.vjaykrsna.nanoai.core.data.image.db.GeneratedImageDao
import com.vjaykrsna.nanoai.core.data.library.catalog.DownloadManifestDao
import com.vjaykrsna.nanoai.core.data.library.daos.DownloadTaskDao
import com.vjaykrsna.nanoai.core.data.library.daos.ModelPackageReadDao
import com.vjaykrsna.nanoai.core.data.library.daos.ModelPackageRelationsDao
import com.vjaykrsna.nanoai.core.data.library.daos.ModelPackageWriteDao
import com.vjaykrsna.nanoai.core.data.library.huggingface.dao.HuggingFaceModelCacheDao
import com.vjaykrsna.nanoai.core.maintenance.db.CodeQualityMetricDao
import com.vjaykrsna.nanoai.core.maintenance.db.RepoMaintenanceTaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides the Room database instance. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
  @Provides
  @Singleton
  fun provideNanoAIDatabase(@ApplicationContext context: Context): NanoAIDatabase =
    Room.databaseBuilder(context, NanoAIDatabase::class.java, NanoAIDatabase.DATABASE_NAME)
      .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
      .fallbackToDestructiveMigration(true)
      .build()
}

/** Provides DAOs for core chat and persona features. */
@Module
@InstallIn(SingletonComponent::class)
object CoreDaoModule {
  @Provides
  @Singleton
  fun provideChatThreadDao(database: NanoAIDatabase): ChatThreadDao = database.chatThreadDao()

  @Provides
  @Singleton
  fun provideMessageDao(database: NanoAIDatabase): MessageDao = database.messageDao()

  @Provides
  @Singleton
  fun providePersonaProfileDao(database: NanoAIDatabase): PersonaProfileDao =
    database.personaProfileDao()

  @Provides
  @Singleton
  fun providePersonaSwitchLogDao(database: NanoAIDatabase): PersonaSwitchLogDao =
    database.personaSwitchLogDao()

  @Provides
  @Singleton
  fun provideApiProviderConfigDao(database: NanoAIDatabase): ApiProviderConfigDao =
    database.apiProviderConfigDao()
}

/** Provides DAOs associated with model catalog and downloads. */
@Module
@InstallIn(SingletonComponent::class)
object LibraryDaoModule {
  @Provides
  @Singleton
  fun provideModelPackageReadDao(database: NanoAIDatabase): ModelPackageReadDao =
    database.modelPackageReadDao()

  @Provides
  @Singleton
  fun provideModelPackageWriteDao(database: NanoAIDatabase): ModelPackageWriteDao =
    database.modelPackageWriteDao()

  @Provides
  @Singleton
  fun provideModelPackageRelationsDao(database: NanoAIDatabase): ModelPackageRelationsDao =
    database.modelPackageRelationsDao()

  @Provides
  @Singleton
  fun provideDownloadManifestDao(database: NanoAIDatabase): DownloadManifestDao =
    database.downloadManifestDao()

  @Provides
  @Singleton
  fun provideDownloadTaskDao(database: NanoAIDatabase): DownloadTaskDao = database.downloadTaskDao()

  @Provides
  @Singleton
  fun provideHuggingFaceModelCacheDao(database: NanoAIDatabase): HuggingFaceModelCacheDao =
    database.huggingFaceModelCacheDao()
}

/** Provides DAOs supporting UI/UX specific persistence. */
@Module
@InstallIn(SingletonComponent::class)
object UiUxDaoModule {
  @Provides
  @Singleton
  fun provideUserProfileDao(database: NanoAIDatabase): UserProfileDao = database.userProfileDao()

  @Provides
  @Singleton
  fun provideLayoutSnapshotDao(database: NanoAIDatabase): LayoutSnapshotDao =
    database.layoutSnapshotDao()

  @Provides
  @Singleton
  fun provideUIStateSnapshotDao(database: NanoAIDatabase): UIStateSnapshotDao =
    database.uiStateSnapshotDao()
}

/** Provides DAOs for maintenance telemetry and generated content. */
@Module
@InstallIn(SingletonComponent::class)
object MaintenanceDaoModule {
  @Provides
  @Singleton
  fun provideRepoMaintenanceTaskDao(database: NanoAIDatabase): RepoMaintenanceTaskDao =
    database.repoMaintenanceTaskDao()

  @Provides
  @Singleton
  fun provideCodeQualityMetricDao(database: NanoAIDatabase): CodeQualityMetricDao =
    database.codeQualityMetricDao()
}

/** Provides DAOs for the image generation feature set. */
@Module
@InstallIn(SingletonComponent::class)
object ImageDaoModule {
  @Provides
  @Singleton
  fun provideGeneratedImageDao(database: NanoAIDatabase): GeneratedImageDao =
    database.generatedImageDao()
}
