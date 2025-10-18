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
import com.vjaykrsna.nanoai.feature.image.data.db.GeneratedImageDao
import com.vjaykrsna.nanoai.feature.image.data.db.GeneratedImageEntity
import com.vjaykrsna.nanoai.feature.library.data.daos.DownloadTaskDao
import com.vjaykrsna.nanoai.feature.library.data.entities.DownloadTaskEntity
import com.vjaykrsna.nanoai.feature.library.data.huggingface.dao.HuggingFaceModelCacheDao
import com.vjaykrsna.nanoai.feature.library.data.huggingface.entities.HuggingFaceModelCacheEntity
import com.vjaykrsna.nanoai.model.catalog.DownloadManifestDao
import com.vjaykrsna.nanoai.model.catalog.DownloadManifestEntity
import com.vjaykrsna.nanoai.model.catalog.ModelPackageEntity
import com.vjaykrsna.nanoai.model.catalog.ModelPackageReadDao
import com.vjaykrsna.nanoai.model.catalog.ModelPackageRelationsDao
import com.vjaykrsna.nanoai.model.catalog.ModelPackageWriteDao

/**
 * Room database for nanoAI application.
 *
 * Contains all entities for chat threads, messages, personas, models, downloads, API
 * configurations, persona switch logging, and UI/UX state.
 *
 * Version 1: Initial schema with core entities. Version 2: Added UserProfile, LayoutSnapshot, and
 * UIStateSnapshot entities for UI/UX feature. Version 3: Added maintenance tracking tables and
 * download manifests. Version 4: Added public key metadata to download manifests. Version 5:
 * Extended ui_state_snapshots persistence for unified shell drawers and palette state. Version 6:
 * Schema update for test coverage improvements. Version 7: Added GeneratedImage entity for image
 * feature gallery.
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
      GeneratedImageEntity::class,
      HuggingFaceModelCacheEntity::class,
    ],
  version = 8,
  exportSchema = true,
)
@TypeConverters(
  TemporalTypeConverters::class,
  CollectionTypeConverters::class,
  UiPreferenceTypeConverters::class,
  MaintenanceTypeConverters::class,
  DeliveryTypeConverters::class,
)
@Suppress("TooManyFunctions")
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
  abstract fun modelPackageReadDao(): ModelPackageReadDao

  abstract fun modelPackageWriteDao(): ModelPackageWriteDao

  abstract fun modelPackageRelationsDao(): ModelPackageRelationsDao

  abstract fun downloadManifestDao(): DownloadManifestDao

  abstract fun downloadTaskDao(): DownloadTaskDao

  abstract fun huggingFaceModelCacheDao(): HuggingFaceModelCacheDao

  // UI/UX feature DAOs
  abstract fun userProfileDao(): UserProfileDao

  abstract fun layoutSnapshotDao(): LayoutSnapshotDao

  abstract fun uiStateSnapshotDao(): UIStateSnapshotDao

  // Image feature DAOs
  abstract fun generatedImageDao(): GeneratedImageDao

  companion object {
    const val DATABASE_NAME = "nanoai_database"
  }
}
