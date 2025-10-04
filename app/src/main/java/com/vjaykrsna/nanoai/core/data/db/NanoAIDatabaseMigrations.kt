package com.vjaykrsna.nanoai.core.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Central registry for Room database migrations. */
object NanoAIDatabaseMigrations {
  /**
   * Migration from schema version 1 to 2 introducing UI/UX tables.
   *
   * Adds user profile, layout snapshot, and UI state snapshot tables required by the polished UI/UX
   * feature while preserving existing chat history data.
   */
  val MIGRATION_1_2: Migration =
    object : Migration(1, 2) {
      override fun migrate(database: SupportSQLiteDatabase) {
        createUserProfilesTable(database)
        createLayoutSnapshotsTable(database)
        createUiStateSnapshotsTable(database)
      }

      private fun createUserProfilesTable(database: SupportSQLiteDatabase) {
        database.execSQL(
          """
                    CREATE TABLE IF NOT EXISTS user_profiles (
                        user_id TEXT NOT NULL PRIMARY KEY,
                        display_name TEXT,
                        theme_preference TEXT NOT NULL,
                        visual_density TEXT NOT NULL,
                        onboarding_completed INTEGER NOT NULL DEFAULT 0,
                        dismissed_tips TEXT NOT NULL DEFAULT '{}',
                        last_opened_screen TEXT NOT NULL,
                        compact_mode INTEGER NOT NULL DEFAULT 0,
                        pinned_tools TEXT NOT NULL DEFAULT '[]'
                    )
                    """
            .trimIndent(),
        )
      }

      private fun createLayoutSnapshotsTable(database: SupportSQLiteDatabase) {
        database.execSQL(
          """
                    CREATE TABLE IF NOT EXISTS layout_snapshots (
                        layout_id TEXT NOT NULL PRIMARY KEY,
                        user_id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        last_opened_screen TEXT NOT NULL,
                        pinned_tools TEXT NOT NULL DEFAULT '[]',
                        is_compact INTEGER NOT NULL DEFAULT 0,
                        position INTEGER NOT NULL,
                        FOREIGN KEY(user_id) REFERENCES user_profiles(user_id) ON UPDATE CASCADE ON DELETE CASCADE
                    )
                    """
            .trimIndent(),
        )
        database.execSQL(
          """
                    CREATE INDEX IF NOT EXISTS index_layout_snapshots_user_id ON layout_snapshots(user_id)
                    """
            .trimIndent(),
        )
        database.execSQL(
          """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_layout_snapshots_user_id_position ON layout_snapshots(user_id, position)
                    """
            .trimIndent(),
        )
        database.execSQL(
          """
                    CREATE INDEX IF NOT EXISTS index_layout_snapshots_last_opened_screen ON layout_snapshots(last_opened_screen)
                    """
            .trimIndent(),
        )
      }

      private fun createUiStateSnapshotsTable(database: SupportSQLiteDatabase) {
        database.execSQL(
          """
                    CREATE TABLE IF NOT EXISTS ui_state_snapshots (
                        user_id TEXT NOT NULL PRIMARY KEY,
                        expanded_panels TEXT NOT NULL DEFAULT '[]',
                        recent_actions TEXT NOT NULL DEFAULT '[]',
                        sidebar_collapsed INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(user_id) REFERENCES user_profiles(user_id) ON UPDATE CASCADE ON DELETE CASCADE
                    )
                    """
            .trimIndent(),
        )
      }
    }

  /** Migration from schema version 2 to 3 introducing maintenance tracking tables. */
  val MIGRATION_2_3: Migration =
    object : Migration(2, 3) {
      override fun migrate(database: SupportSQLiteDatabase) {
        upgradeModelPackagesTable(database)
        createRepoMaintenanceTasksTable(database)
        createCodeQualityMetricsTable(database)
        createDownloadManifestsTable(database)
      }

      private fun upgradeModelPackagesTable(database: SupportSQLiteDatabase) {
        database.execSQL(
          """
                    CREATE TABLE IF NOT EXISTS model_packages_new (
                        model_id TEXT NOT NULL PRIMARY KEY,
                        display_name TEXT NOT NULL,
                        version TEXT NOT NULL,
                        provider_type TEXT NOT NULL,
                        delivery_type TEXT NOT NULL DEFAULT 'LOCAL_ARCHIVE',
                        min_app_version INTEGER NOT NULL DEFAULT 1,
                        size_bytes INTEGER NOT NULL,
                        capabilities TEXT NOT NULL DEFAULT '[]',
                        install_state TEXT NOT NULL,
                        download_task_id TEXT,
                        manifest_url TEXT NOT NULL DEFAULT '',
                        checksum_sha256 TEXT,
                        signature TEXT,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """
            .trimIndent(),
        )
        database.execSQL(
          """
                    INSERT INTO model_packages_new (
                        model_id,
                        display_name,
                        version,
                        provider_type,
                        delivery_type,
                        min_app_version,
                        size_bytes,
                        capabilities,
                        install_state,
                        download_task_id,
                        manifest_url,
                        checksum_sha256,
                        signature,
                        created_at,
                        updated_at
                    )
                    SELECT
                        model_id,
                        display_name,
                        version,
                        provider_type,
                        'LOCAL_ARCHIVE',
                        1,
                        size_bytes,
                        COALESCE(capabilities, '[]'),
                        install_state,
                        download_task_id,
                        '' AS manifest_url,
                        checksum,
                        NULL,
                        COALESCE(updated_at, strftime('%s','now') * 1000),
                        COALESCE(updated_at, strftime('%s','now') * 1000)
                    FROM model_packages
                    """
            .trimIndent(),
        )
        database.execSQL("DROP TABLE IF EXISTS model_packages")
        database.execSQL("ALTER TABLE model_packages_new RENAME TO model_packages")
        database.execSQL(
          """
                    CREATE INDEX IF NOT EXISTS index_model_packages_provider_type
                        ON model_packages(provider_type)
                    """
            .trimIndent(),
        )
        database.execSQL(
          """
                    CREATE INDEX IF NOT EXISTS index_model_packages_delivery_type
                        ON model_packages(delivery_type)
                    """
            .trimIndent(),
        )
        database.execSQL(
          """
                    CREATE INDEX IF NOT EXISTS index_model_packages_install_state
                        ON model_packages(install_state)
                    """
            .trimIndent(),
        )
      }

      private fun createRepoMaintenanceTasksTable(database: SupportSQLiteDatabase) {
        database.execSQL(
          """
                    CREATE TABLE IF NOT EXISTS repo_maintenance_tasks (
                        task_id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        category TEXT NOT NULL,
                        priority TEXT NOT NULL,
                        status TEXT NOT NULL,
                        owner TEXT,
                        blocking_rules TEXT NOT NULL DEFAULT '[]',
                        linked_artifacts TEXT NOT NULL DEFAULT '[]',
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """
            .trimIndent(),
        )
        database.execSQL(
          """
                    CREATE INDEX IF NOT EXISTS index_repo_maintenance_tasks_status
                        ON repo_maintenance_tasks(status)
                    """
            .trimIndent(),
        )
        database.execSQL(
          """
                    CREATE INDEX IF NOT EXISTS index_repo_maintenance_tasks_priority
                        ON repo_maintenance_tasks(priority)
                    """
            .trimIndent(),
        )
      }

      private fun createCodeQualityMetricsTable(database: SupportSQLiteDatabase) {
        database.execSQL(
          """
                    CREATE TABLE IF NOT EXISTS code_quality_metrics (
                        metric_id TEXT NOT NULL PRIMARY KEY,
                        task_id TEXT,
                        rule_id TEXT NOT NULL,
                        file_path TEXT NOT NULL,
                        severity TEXT NOT NULL,
                        occurrences INTEGER NOT NULL,
                        threshold INTEGER NOT NULL,
                        first_detected_at INTEGER NOT NULL,
                        resolved_at INTEGER,
                        notes TEXT,
                        FOREIGN KEY(task_id) REFERENCES repo_maintenance_tasks(task_id)
                            ON UPDATE CASCADE ON DELETE SET NULL
                    )
                    """
            .trimIndent(),
        )
        database.execSQL(
          """
                    CREATE INDEX IF NOT EXISTS index_code_quality_metrics_task_id
                        ON code_quality_metrics(task_id)
                    """
            .trimIndent(),
        )
        database.execSQL(
          """
                    CREATE INDEX IF NOT EXISTS index_code_quality_metrics_rule_id
                        ON code_quality_metrics(rule_id)
                    """
            .trimIndent(),
        )
        database.execSQL(
          """
                    CREATE INDEX IF NOT EXISTS index_code_quality_metrics_file_path
                        ON code_quality_metrics(file_path)
                    """
            .trimIndent(),
        )
      }

      private fun createDownloadManifestsTable(database: SupportSQLiteDatabase) {
        database.execSQL(
          """
                    CREATE TABLE IF NOT EXISTS download_manifests (
                        model_id TEXT NOT NULL,
                        version TEXT NOT NULL,
                        checksum_sha256 TEXT NOT NULL,
                        size_bytes INTEGER NOT NULL,
                        download_url TEXT NOT NULL,
                        signature TEXT,
                        expires_at INTEGER,
                        fetched_at INTEGER NOT NULL,
                        release_notes TEXT,
                        PRIMARY KEY(model_id, version),
                        FOREIGN KEY(model_id) REFERENCES model_packages(model_id)
                            ON UPDATE CASCADE ON DELETE CASCADE
                    )
                    """
            .trimIndent(),
        )
        database.execSQL(
          """
                    CREATE INDEX IF NOT EXISTS index_download_manifests_model_id
                        ON download_manifests(model_id)
                    """
            .trimIndent(),
        )
        database.execSQL(
          """
                    CREATE INDEX IF NOT EXISTS index_download_manifests_expires_at
                        ON download_manifests(expires_at)
                    """
            .trimIndent(),
        )
        database.execSQL(
          """
                    CREATE INDEX IF NOT EXISTS index_download_manifests_checksum
                        ON download_manifests(checksum_sha256)
                    """
            .trimIndent(),
        )
      }
    }
}
