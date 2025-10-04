package com.vjaykrsna.nanoai.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.security.model.CredentialScope
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

/**
 * Coordinates migration of legacy plaintext provider secrets into [EncryptedSecretStore].
 *
 * The migration runs once during application start. It copies any entries found in legacy
 * SharedPreferences files or flat files into the encrypted store and then removes the plaintext
 * sources. Progress is tracked in a lightweight SharedPreferences marker to avoid redundant work.
 */
@Singleton
class SecretMigrationInitializer
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val encryptedSecretStore: EncryptedSecretStore,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
  private val startupScope = CoroutineScope(SupervisorJob() + ioDispatcher)
  private val hasStarted = AtomicBoolean(false)

  /** Kick off the migration asynchronously. Safe to call multiple times; only first call runs. */
  fun ensureMigration() {
    if (hasStarted.compareAndSet(false, true)) {
      startupScope.launch { runMigrationInternal() }
    }
  }

  /** Runs the migration synchronously for tests. */
  @VisibleForTesting
  suspend fun runMigrationBlocking() {
    withContext(ioDispatcher) { runMigrationInternal() }
  }

  private suspend fun runMigrationInternal() {
    val statePrefs = migrationStatePrefs()
    if (statePrefs.getBoolean(KEY_COMPLETED, false)) {
      Log.d(TAG, "Secret migration already completed; skipping.")
      return
    }

    val snapshots = snapshotLegacyPreferences()
    if (snapshots.isEmpty() && !hasLegacyFiles()) {
      markMigrationComplete(statePrefs, migratedEntries = 0)
      Log.i(TAG, "Secret migration found no legacy credentials.")
      return
    }

    var migratedCount = 0
    snapshots.forEach { snapshot ->
      snapshot.entries.forEach { (providerId, secretValue) ->
        runCatching {
            encryptedSecretStore.saveCredential(
              providerId = providerId,
              encryptedValue = secretValue,
              scope = CredentialScope.TEXT_INFERENCE,
              metadata =
                mapOf(
                  METADATA_SOURCE to snapshot.name,
                  METADATA_MIGRATED_AT to Clock.System.now().toString(),
                ),
            )
            migratedCount += 1
          }
          .onFailure { error ->
            Log.e(TAG, "Failed to migrate credential for $providerId from ${snapshot.name}", error)
          }
      }
      snapshot.prefs.edit().clear().apply()
      Log.i(TAG, "Cleared legacy credential store ${snapshot.name} after migration.")
    }

    deleteLegacyFilesIfPresent()

    markMigrationComplete(statePrefs, migratedEntries = migratedCount)
    Log.i(TAG, "Secret migration completed; migrated $migratedCount credential(s).")
  }

  private fun migrationStatePrefs(): SharedPreferences =
    context.getSharedPreferences(MIGRATION_STATE_PREFS, Context.MODE_PRIVATE)

  private fun markMigrationComplete(prefs: SharedPreferences, migratedEntries: Int) {
    prefs
      .edit()
      .putBoolean(KEY_COMPLETED, true)
      .putLong(KEY_COMPLETED_AT, Clock.System.now().toEpochMilliseconds())
      .putInt(KEY_MIGRATED_COUNT, migratedEntries)
      .apply()
  }

  private fun snapshotLegacyPreferences(): List<LegacyPreferenceSnapshot> {
    val snapshots = mutableListOf<LegacyPreferenceSnapshot>()
    LEGACY_PREF_NAMES.forEach { prefName ->
      val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
      val entries =
        prefs.all.mapNotNull { (key, value) ->
          val secret = value as? String
          if (secret.isNullOrBlank()) {
            null
          } else {
            key to secret
          }
        }
      if (entries.isNotEmpty()) {
        snapshots += LegacyPreferenceSnapshot(name = prefName, prefs = prefs, entries = entries)
      }
    }
    return snapshots
  }

  private fun hasLegacyFiles(): Boolean =
    LEGACY_FILES.any { legacyFileName -> File(context.filesDir, legacyFileName).exists() }

  private fun deleteLegacyFilesIfPresent() {
    LEGACY_FILES.forEach { legacyFileName ->
      val file = File(context.filesDir, legacyFileName)
      if (file.exists()) {
        val deleted = encryptedSecretStore.deleteLegacyStore(file)
        if (deleted) {
          Log.i(TAG, "Deleted legacy credential file ${file.name}.")
        } else {
          Log.w(TAG, "Unable to delete legacy credential file ${file.name}.")
        }
      }
    }
  }

  private data class LegacyPreferenceSnapshot(
    val name: String,
    val prefs: SharedPreferences,
    val entries: List<Pair<String, String>>,
  )

  companion object {
    private const val TAG = "SecretMigration"
    private const val MIGRATION_STATE_PREFS = "secret_migration_state"
    private const val KEY_COMPLETED = "completed"
    private const val KEY_COMPLETED_AT = "completed_at"
    private const val KEY_MIGRATED_COUNT = "migrated_count"
    private const val METADATA_SOURCE = "source"
    private const val METADATA_MIGRATED_AT = "migratedAt"
    private val LEGACY_PREF_NAMES =
      listOf(
        "legacy_provider_config",
        "provider_config",
        "api_provider_config",
      )
    private val LEGACY_FILES =
      listOf(
        "legacy-secrets.xml",
        "provider-secrets.xml",
        "provider_config.json",
      )
  }
}
