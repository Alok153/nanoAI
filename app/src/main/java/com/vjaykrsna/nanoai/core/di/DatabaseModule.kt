package com.vjaykrsna.nanoai.core.di

import android.content.Context
import androidx.room.Room
import com.vjaykrsna.nanoai.core.data.db.NanoAIDatabase
import com.vjaykrsna.nanoai.core.data.db.daos.ApiProviderConfigDao
import com.vjaykrsna.nanoai.core.data.db.daos.ChatThreadDao
import com.vjaykrsna.nanoai.core.data.db.daos.MessageDao
import com.vjaykrsna.nanoai.core.data.db.daos.PersonaProfileDao
import com.vjaykrsna.nanoai.core.data.db.daos.PersonaSwitchLogDao
import com.vjaykrsna.nanoai.feature.library.data.daos.DownloadTaskDao
import com.vjaykrsna.nanoai.feature.library.data.daos.ModelPackageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Hilt module providing Room database and DAOs. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideNanoAIDatabase(@ApplicationContext context: Context): NanoAIDatabase {
        return Room.databaseBuilder(context, NanoAIDatabase::class.java, "nanoai_database")
                .fallbackToDestructiveMigration() // TODO: Add proper migrations for production
                .build()
    }

    @Provides
    @Singleton
    fun provideChatThreadDao(database: NanoAIDatabase): ChatThreadDao {
        return database.chatThreadDao()
    }

    @Provides
    @Singleton
    fun provideMessageDao(database: NanoAIDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    @Singleton
    fun providePersonaProfileDao(database: NanoAIDatabase): PersonaProfileDao {
        return database.personaProfileDao()
    }

    @Provides
    @Singleton
    fun providePersonaSwitchLogDao(database: NanoAIDatabase): PersonaSwitchLogDao {
        return database.personaSwitchLogDao()
    }

    @Provides
    @Singleton
    fun provideApiProviderConfigDao(database: NanoAIDatabase): ApiProviderConfigDao {
        return database.apiProviderConfigDao()
    }

    @Provides
    @Singleton
    fun provideModelPackageDao(database: NanoAIDatabase): ModelPackageDao {
        return database.modelPackageDao()
    }

    @Provides
    @Singleton
    fun provideDownloadTaskDao(database: NanoAIDatabase): DownloadTaskDao {
        return database.downloadTaskDao()
    }
}
