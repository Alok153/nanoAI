package com.vjaykrsna.nanoai.core.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NanoAIDatabaseMigrationTest {
  private val context = InstrumentationRegistry.getInstrumentation().targetContext

  @get:Rule
  val helper =
    MigrationTestHelper(
      InstrumentationRegistry.getInstrumentation(),
      NanoAIDatabase::class.java,
      listOf(),
      FrameworkSQLiteOpenHelperFactory(),
    )

  @Test
  fun migrate7To8_addsColumnsAndTables() {
    helper.createDatabase(TEST_DB, 7).apply {
      execSQL(
        """
        INSERT INTO download_tasks (task_id, model_id, progress, status, bytes_downloaded, started_at, finished_at, error_message)
        VALUES ('t1','m1',0.1,'QUEUED',100,NULL,NULL,NULL)
        """
      )
      close()
    }

    helper.runMigrationsAndValidate(TEST_DB, 8, true, NanoAIDatabaseMigrations.MIGRATION_7_8).use {
      db ->
      db.query("PRAGMA table_info(download_tasks)").use { cursor ->
        val columnNames = buildList {
          while (cursor.moveToNext()) {
            add(cursor.getString(cursor.getColumnIndex("name")))
          }
        }
        assertThat(columnNames).contains("total_bytes")
      }

      db
        .query("SELECT name FROM sqlite_master WHERE type='table' AND name='generated_images'")
        .use { cursor -> assertThat(cursor.count).isEqualTo(1) }

      db
        .query("SELECT name FROM sqlite_master WHERE type='table' AND name='huggingface_models'")
        .use { cursor -> assertThat(cursor.count).isEqualTo(1) }
    }

    context.deleteDatabase(TEST_DB)
  }

  private companion object {
    const val TEST_DB = "nanoai-migration-test.db"
  }
}
