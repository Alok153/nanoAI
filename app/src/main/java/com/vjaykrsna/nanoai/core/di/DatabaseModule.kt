package com.vjaykrsna.nanoai.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.vjaykrsna.nanoai.core.data.db.NanoAIDatabase
import com.vjaykrsna.nanoai.core.data.db.NanoAIDatabaseMigrations
import com.vjaykrsna.nanoai.core.data.db.daos.ApiProviderConfigDao
import com.vjaykrsna.nanoai.core.data.db.daos.ChatThreadDao
import com.vjaykrsna.nanoai.core.data.db.daos.LayoutSnapshotDao
import com.vjaykrsna.nanoai.core.data.db.daos.MessageDao
import com.vjaykrsna.nanoai.core.data.db.daos.PersonaProfileDao
import com.vjaykrsna.nanoai.core.data.db.daos.PersonaSwitchLogDao
import com.vjaykrsna.nanoai.core.data.db.daos.UIStateSnapshotDao
import com.vjaykrsna.nanoai.core.data.db.daos.UserProfileDao
import com.vjaykrsna.nanoai.core.maintenance.db.CodeQualityMetricDao
import com.vjaykrsna.nanoai.core.maintenance.db.RepoMaintenanceTaskDao
import com.vjaykrsna.nanoai.feature.library.data.daos.DownloadTaskDao
import com.vjaykrsna.nanoai.feature.library.data.daos.ModelPackageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Hilt module providing database and DAO instances. */
@Module
@InstallIn(SingletonComponent::class)
@Suppress("TooManyFunctions") // DI module provides many DAO instances
object DatabaseModule {
  @Provides
  @Singleton
  fun provideNanoAIDatabase(@ApplicationContext context: Context): NanoAIDatabase =
    Room.databaseBuilder(context, NanoAIDatabase::class.java, NanoAIDatabase.DATABASE_NAME)
      .addMigrations(
        NanoAIDatabaseMigrations.MIGRATION_1_2,
        NanoAIDatabaseMigrations.MIGRATION_2_3,
        NanoAIDatabaseMigrations.MIGRATION_3_4,
      )
      .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
      .build()

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

  @Provides
  @Singleton
  fun provideModelPackageDao(database: NanoAIDatabase): ModelPackageDao = database.modelPackageDao()

  @Provides
  @Singleton
  fun provideDownloadTaskDao(database: NanoAIDatabase): DownloadTaskDao = database.downloadTaskDao()

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

  @Provides
  @Singleton
  fun provideRepoMaintenanceTaskDao(database: NanoAIDatabase): RepoMaintenanceTaskDao =
    database.repoMaintenanceTaskDao()

  @Provides
  @Singleton
  fun provideCodeQualityMetricDao(database: NanoAIDatabase): CodeQualityMetricDao =
    database.codeQualityMetricDao()
}
