package com.vjaykrsna.nanoai.core.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Central registry for Room database migrations. */
object NanoAIDatabaseMigrations {
  private const val VERSION_7 = 7
  private const val VERSION_8 = 8

  val MIGRATION_7_8 =
    object : Migration(VERSION_7, VERSION_8) {
      override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
          """
          ALTER TABLE download_tasks ADD COLUMN total_bytes INTEGER NOT NULL DEFAULT 0
          """
        )

        database.execSQL(
          """
          CREATE TABLE IF NOT EXISTS generated_images (
            id TEXT NOT NULL,
            prompt TEXT NOT NULL,
            negative_prompt TEXT NOT NULL,
            width INTEGER NOT NULL,
            height INTEGER NOT NULL,
            steps INTEGER NOT NULL,
            guidance_scale REAL NOT NULL,
            file_path TEXT NOT NULL,
            thumbnail_path TEXT,
            created_at INTEGER NOT NULL,
            PRIMARY KEY(id)
          )
          """
        )

        database.execSQL(
          """
          CREATE TABLE IF NOT EXISTS huggingface_models (
            model_id TEXT NOT NULL,
            display_name TEXT NOT NULL,
            author TEXT,
            pipeline_tag TEXT,
            library_name TEXT,
            tags TEXT NOT NULL,
            likes INTEGER NOT NULL,
            downloads INTEGER NOT NULL,
            license TEXT,
            languages TEXT NOT NULL,
            base_model TEXT,
            datasets TEXT NOT NULL,
            architectures TEXT NOT NULL,
            model_type TEXT,
            base_relations TEXT NOT NULL,
            gated INTEGER NOT NULL,
            disabled INTEGER NOT NULL,
            total_size_bytes INTEGER,
            summary TEXT,
            description TEXT,
            trending_score INTEGER,
            created_at INTEGER,
            last_modified INTEGER,
            private INTEGER NOT NULL,
            fetched_at INTEGER NOT NULL,
            PRIMARY KEY(model_id)
          )
          """
        )
      }
    }

  val ALL = arrayOf(MIGRATION_7_8)
}
