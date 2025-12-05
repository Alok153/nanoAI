package com.vjaykrsna.nanoai.core.maintenance.db

import android.content.Context
import androidx.room.RoomMasterTable
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val TEST_DB = "maintenance-migration-test"
private const val VERSION_2_IDENTITY = "e2a4721f193ffd510a2671baeaabeb9d"
private const val VERSION_3_IDENTITY = "60b31829a0f49c3f9f070c9d12ef4d2e"
private const val VERSION_4_IDENTITY = "042c7d4e5d78c94b26aa7c37d4a385fb"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@Ignore("Migrations removed during development; re-enable when migrations are added before release")
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
            """
              SELECT model_id, manifest_url, checksum_sha256, signature, min_app_version 
              FROM model_packages
            """
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

  @Test
  fun migrate3To4_addsPublicKeyUrlColumn() {
    val configuration = legacyConfiguration()

    helperFactory.create(configuration).use { helper ->
      helper.writableDatabase.use { db ->
        db.execSQL("PRAGMA user_version = 2")
        applyMigrationToVersion3(db)
        seedLegacyManifest(db)
        applyMigrationToVersion4(db)
        assertHasPublicKeyColumn(db)
        assertLegacyManifestHasNullPublicKey(db)
      }
    }

    context.deleteDatabase(TEST_DB)
  }

  private fun applyMigrationToVersion3(db: SupportSQLiteDatabase) {
    // MIGRATION_2_3 removed during development. When migrations are reintroduced this
    // should call NanoAIDatabaseMigrations.MIGRATION_2_3.migrate(db).
    // For now, apply the expected Room master table identity and bump the user_version
    // so tests that assert schema structure can proceed when enabled.
    db.execSQL(RoomMasterTable.CREATE_QUERY)
    db.execSQL(
      """
        INSERT OR REPLACE INTO ${RoomMasterTable.TABLE_NAME} (id, identity_hash) 
        VALUES(42, '$VERSION_3_IDENTITY')
      """
    )
    db.execSQL("PRAGMA user_version = 3")
  }

  private fun seedLegacyManifest(db: SupportSQLiteDatabase) {
    db.execSQL(
      """
        INSERT INTO download_manifests (
          model_id,
          version,
          checksum_sha256,
          size_bytes,
          download_url,
          signature,
          expires_at,
          fetched_at,
          release_notes
        ) VALUES (
          'persona-text-delta',
          '1.0.0',
          'abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890',
          524288000,
          'https://example.com/model.zip',
          NULL,
          NULL,
          1700000000000,
          NULL
        )
      """
        .trimIndent()
    )
  }

  private fun assertHasPublicKeyColumn(db: SupportSQLiteDatabase) {
    db.query("PRAGMA table_info(download_manifests)").use { cursor ->
      val columns = mutableListOf<String>()
      while (cursor.moveToNext()) {
        columns += cursor.getString(cursor.getColumnIndexOrThrow("name"))
      }

      assertThat(columns)
        .containsAtLeast(
          "model_id",
          "version",
          "checksum_sha256",
          "size_bytes",
          "download_url",
          "public_key_url",
        )
    }
  }

  private fun assertLegacyManifestHasNullPublicKey(db: SupportSQLiteDatabase) {
    db
      .query(
        "SELECT public_key_url FROM download_manifests WHERE model_id = ?",
        arrayOf("persona-text-delta"),
      )
      .use { cursor ->
        assertThat(cursor.moveToFirst()).isTrue()
        assertThat(cursor.isNull(0)).isTrue()
      }
  }

  private fun applyMigrationToVersion4(db: SupportSQLiteDatabase) {
    // MIGRATION_3_4 removed during development. When migrations are reintroduced this
    // should call NanoAIDatabaseMigrations.MIGRATION_3_4.migrate(db).
    // For now, apply the expected Room master table identity and bump the user_version
    // so tests that assert schema structure can proceed when enabled.
    db.execSQL(RoomMasterTable.CREATE_QUERY)
    db.execSQL(
      """
        INSERT OR REPLACE INTO ${RoomMasterTable.TABLE_NAME} (id, identity_hash) 
        VALUES(42, '$VERSION_4_IDENTITY')
      """
    )
    db.execSQL("PRAGMA user_version = 4")
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
                .trimIndent()
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
                .trimIndent()
            )
            db.execSQL(RoomMasterTable.CREATE_QUERY)
            db.execSQL(
              """
                INSERT OR REPLACE INTO ${RoomMasterTable.TABLE_NAME} (id, identity_hash) 
                VALUES(42, '$VERSION_2_IDENTITY')
              """
            )
          }

          override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
      )
      .build()
  }
}
