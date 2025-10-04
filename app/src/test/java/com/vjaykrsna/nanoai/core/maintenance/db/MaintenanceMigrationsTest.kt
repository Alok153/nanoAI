package com.vjaykrsna.nanoai.core.maintenance.db

import android.content.Context
import androidx.room.RoomMasterTable
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.db.NanoAIDatabaseMigrations
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val TEST_DB = "maintenance-migration-test"
private const val VERSION_2_IDENTITY = "e2a4721f193ffd510a2671baeaabeb9d"
private const val VERSION_3_IDENTITY = "60b31829a0f49c3f9f070c9d12ef4d2e"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MaintenanceMigrationsTest {
  private val context: Context = ApplicationProvider.getApplicationContext()
  private val helperFactory = FrameworkSQLiteOpenHelperFactory()

  @Test
  fun migrate2To3_createsMaintenanceTablesAndPreservesModelPackages() {
    val configuration = legacyConfiguration()

    helperFactory.create(configuration).use { helper ->
      helper.writableDatabase.use { db ->
        db.execSQL("PRAGMA user_version = 2")
        applyMigrationToVersion3(db)

        db.query("SELECT * FROM repo_maintenance_tasks").use { cursor ->
          assertThat(cursor.columnNames.toList())
            .containsAtLeast(
              "task_id",
              "title",
              "description",
              "category",
              "priority",
              "status",
              "created_at",
              "updated_at",
            )
          assertThat(cursor.count).isEqualTo(0)
        }

        db.query("SELECT * FROM code_quality_metrics").use { cursor ->
          assertThat(cursor.columnNames.toList())
            .containsAtLeast("metric_id", "rule_id", "file_path", "severity", "occurrences")
        }

        db.query("SELECT * FROM download_manifests").use { cursor ->
          assertThat(cursor.columnNames.toList())
            .containsAtLeast("model_id", "version", "checksum_sha256", "download_url", "fetched_at")
        }

        db
          .query(
            "SELECT model_id, manifest_url, checksum_sha256, signature, min_app_version FROM model_packages"
          )
          .use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("persona-text-delta")
            assertThat(cursor.getString(1)).isEqualTo("")
            assertThat(cursor.getString(2))
              .isEqualTo("abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890")
            assertThat(cursor.isNull(3)).isTrue()
            assertThat(cursor.getInt(4)).isEqualTo(1)
          }
      }
    }

    context.deleteDatabase(TEST_DB)
  }

  private fun applyMigrationToVersion3(db: SupportSQLiteDatabase) {
    NanoAIDatabaseMigrations.MIGRATION_2_3.migrate(db)
    db.execSQL(RoomMasterTable.CREATE_QUERY)
    db.execSQL(
      "INSERT OR REPLACE INTO ${RoomMasterTable.TABLE_NAME} (id, identity_hash) VALUES(42, '$VERSION_3_IDENTITY')"
    )
    db.execSQL("PRAGMA user_version = 3")
  }

  private fun legacyConfiguration(): SupportSQLiteOpenHelper.Configuration {
    context.deleteDatabase(TEST_DB)
    return SupportSQLiteOpenHelper.Configuration.builder(context)
      .name(TEST_DB)
      .callback(
        object : SupportSQLiteOpenHelper.Callback(2) {
          override fun onCreate(db: SupportSQLiteDatabase) {
            db.execSQL(
              """
              CREATE TABLE IF NOT EXISTS model_packages (
                model_id TEXT NOT NULL PRIMARY KEY,
                display_name TEXT NOT NULL,
                version TEXT NOT NULL,
                provider_type TEXT NOT NULL,
                size_bytes INTEGER NOT NULL,
                capabilities TEXT NOT NULL,
                install_state TEXT NOT NULL,
                download_task_id TEXT,
                checksum TEXT,
                updated_at INTEGER NOT NULL
              )
              """
                .trimIndent(),
            )
            db.execSQL(
              """
              INSERT INTO model_packages (
                model_id,
                display_name,
                version,
                provider_type,
                size_bytes,
                capabilities,
                install_state,
                download_task_id,
                checksum,
                updated_at
              ) VALUES (
                'persona-text-delta',
                'Persona Text Delta',
                '1.0.0',
                'MEDIA_PIPE',
                524288000,
                '["text"]',
                'INSTALLED',
                NULL,
                'abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890',
                1700000000000
              )
              """
                .trimIndent(),
            )
            db.execSQL(RoomMasterTable.CREATE_QUERY)
            db.execSQL(
              "INSERT OR REPLACE INTO ${RoomMasterTable.TABLE_NAME} (id, identity_hash) VALUES(42, '$VERSION_2_IDENTITY')"
            )
          }

          override fun onUpgrade(
            db: SupportSQLiteDatabase,
            oldVersion: Int,
            newVersion: Int,
          ) = Unit
        },
      )
      .build()
  }
}
