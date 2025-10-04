package com.vjaykrsna.nanoai.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vjaykrsna.nanoai.core.data.db.daos.ApiProviderConfigDao
import com.vjaykrsna.nanoai.core.data.db.daos.ChatThreadDao
import com.vjaykrsna.nanoai.core.data.db.daos.LayoutSnapshotDao
import com.vjaykrsna.nanoai.core.data.db.daos.MessageDao
import com.vjaykrsna.nanoai.core.data.db.daos.PersonaProfileDao
import com.vjaykrsna.nanoai.core.data.db.daos.PersonaSwitchLogDao
import com.vjaykrsna.nanoai.core.data.db.daos.UIStateSnapshotDao
import com.vjaykrsna.nanoai.core.data.db.daos.UserProfileDao
import com.vjaykrsna.nanoai.core.data.db.entities.ApiProviderConfigEntity
import com.vjaykrsna.nanoai.core.data.db.entities.ChatThreadEntity
import com.vjaykrsna.nanoai.core.data.db.entities.LayoutSnapshotEntity
import com.vjaykrsna.nanoai.core.data.db.entities.MessageEntity
import com.vjaykrsna.nanoai.core.data.db.entities.PersonaProfileEntity
import com.vjaykrsna.nanoai.core.data.db.entities.PersonaSwitchLogEntity
import com.vjaykrsna.nanoai.core.data.db.entities.UIStateSnapshotEntity
import com.vjaykrsna.nanoai.core.data.db.entities.UserProfileEntity
import com.vjaykrsna.nanoai.core.maintenance.db.CodeQualityMetricDao
import com.vjaykrsna.nanoai.core.maintenance.db.CodeQualityMetricEntity
import com.vjaykrsna.nanoai.core.maintenance.db.RepoMaintenanceTaskDao
import com.vjaykrsna.nanoai.core.maintenance.db.RepoMaintenanceTaskEntity
import com.vjaykrsna.nanoai.feature.library.data.daos.DownloadTaskDao
import com.vjaykrsna.nanoai.feature.library.data.daos.ModelPackageDao
import com.vjaykrsna.nanoai.feature.library.data.entities.DownloadTaskEntity
import com.vjaykrsna.nanoai.model.catalog.DownloadManifestEntity
import com.vjaykrsna.nanoai.model.catalog.ModelPackageEntity

/**
 * Room database for nanoAI application.
 *
 * Contains all entities for chat threads, messages, personas, models, downloads, API
 * configurations, persona switch logging, and UI/UX state.
 *
 * Version 1: Initial schema with core entities. Version 2: Added UserProfile, LayoutSnapshot, and
 * UIStateSnapshot entities for UI/UX feature.
 *
 * Foreign keys are enabled to ensure referential integrity and cascade deletes. TypeConverters
 * handle UUID, Instant, Set<String>, List<String>, Map<String, Boolean>, and enum conversions.
 */
@Database(
  entities =
    [
      ChatThreadEntity::class,
      MessageEntity::class,
      PersonaProfileEntity::class,
      PersonaSwitchLogEntity::class,
      ModelPackageEntity::class,
      DownloadTaskEntity::class,
      ApiProviderConfigEntity::class,
      UserProfileEntity::class,
      LayoutSnapshotEntity::class,
      UIStateSnapshotEntity::class,
      RepoMaintenanceTaskEntity::class,
      CodeQualityMetricEntity::class,
      DownloadManifestEntity::class,
    ],
  version = 3,
  exportSchema = true,
)
@TypeConverters(com.vjaykrsna.nanoai.core.data.db.TypeConverters::class)
@Suppress("TooManyFunctions") // Database provides many DAO accessors
abstract class NanoAIDatabase : RoomDatabase() {
  // Core DAOs
  abstract fun chatThreadDao(): ChatThreadDao

  abstract fun messageDao(): MessageDao

  abstract fun personaProfileDao(): PersonaProfileDao

  abstract fun personaSwitchLogDao(): PersonaSwitchLogDao

  abstract fun apiProviderConfigDao(): ApiProviderConfigDao

  abstract fun repoMaintenanceTaskDao(): RepoMaintenanceTaskDao

  abstract fun codeQualityMetricDao(): CodeQualityMetricDao

  // Library feature DAOs
  abstract fun modelPackageDao(): ModelPackageDao

  abstract fun downloadTaskDao(): DownloadTaskDao

  // UI/UX feature DAOs
  abstract fun userProfileDao(): UserProfileDao

  abstract fun layoutSnapshotDao(): LayoutSnapshotDao

  abstract fun uiStateSnapshotDao(): UIStateSnapshotDao

  companion object {
    const val DATABASE_NAME = "nanoai_database"
  }
}
