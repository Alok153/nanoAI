package com.vjaykrsna.nanoai.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vjaykrsna.nanoai.core.data.db.daos.ApiProviderConfigDao
import com.vjaykrsna.nanoai.core.data.db.daos.ChatThreadDao
import com.vjaykrsna.nanoai.core.data.db.daos.MessageDao
import com.vjaykrsna.nanoai.core.data.db.daos.PersonaProfileDao
import com.vjaykrsna.nanoai.core.data.db.daos.PersonaSwitchLogDao
import com.vjaykrsna.nanoai.core.data.db.entities.ApiProviderConfigEntity
import com.vjaykrsna.nanoai.core.data.db.entities.ChatThreadEntity
import com.vjaykrsna.nanoai.core.data.db.entities.MessageEntity
import com.vjaykrsna.nanoai.core.data.db.entities.PersonaProfileEntity
import com.vjaykrsna.nanoai.core.data.db.entities.PersonaSwitchLogEntity
import com.vjaykrsna.nanoai.feature.library.data.daos.DownloadTaskDao
import com.vjaykrsna.nanoai.feature.library.data.daos.ModelPackageDao
import com.vjaykrsna.nanoai.feature.library.data.entities.DownloadTaskEntity
import com.vjaykrsna.nanoai.feature.library.data.entities.ModelPackageEntity

/**
 * Room database for nanoAI application.
 * 
 * Contains all entities for chat threads, messages, personas, models, downloads,
 * API configurations, and persona switch logging.
 * 
 * Version 1: Initial schema with all core entities.
 * 
 * Foreign keys are enabled to ensure referential integrity and cascade deletes.
 * TypeConverters handle UUID, Instant, Set<String>, and enum conversions.
 */
@Database(
    entities = [
        ChatThreadEntity::class,
        MessageEntity::class,
        PersonaProfileEntity::class,
        PersonaSwitchLogEntity::class,
        ModelPackageEntity::class,
        DownloadTaskEntity::class,
        ApiProviderConfigEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(com.vjaykrsna.nanoai.core.data.db.TypeConverters::class)
abstract class NanoAIDatabase : RoomDatabase() {

    // Core DAOs
    abstract fun chatThreadDao(): ChatThreadDao
    abstract fun messageDao(): MessageDao
    abstract fun personaProfileDao(): PersonaProfileDao
    abstract fun personaSwitchLogDao(): PersonaSwitchLogDao
    abstract fun apiProviderConfigDao(): ApiProviderConfigDao

    // Library feature DAOs
    abstract fun modelPackageDao(): ModelPackageDao
    abstract fun downloadTaskDao(): DownloadTaskDao

    companion object {
        const val DATABASE_NAME = "nanoai_database"
    }
}
