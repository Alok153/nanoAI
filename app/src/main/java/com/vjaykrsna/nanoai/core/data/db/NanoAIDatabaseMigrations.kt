package com.vjaykrsna.nanoai.core.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Central registry for Room database migrations.
 */
object NanoAIDatabaseMigrations {
    /**
     * Migration from schema version 1 to 2 introducing UI/UX tables.
     *
     * Adds user profile, layout snapshot, and UI state snapshot tables required by
     * the polished UI/UX feature while preserving existing chat history data.
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
                    """.trimIndent(),
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
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_layout_snapshots_user_id ON layout_snapshots(user_id)
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_layout_snapshots_user_id_position ON layout_snapshots(user_id, position)
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_layout_snapshots_last_opened_screen ON layout_snapshots(last_opened_screen)
                    """.trimIndent(),
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
                    """.trimIndent(),
                )
            }
        }
}
