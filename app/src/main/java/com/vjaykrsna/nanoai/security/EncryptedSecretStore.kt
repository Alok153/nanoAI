package com.vjaykrsna.nanoai.security

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.vjaykrsna.nanoai.security.model.CredentialScope
import com.vjaykrsna.nanoai.security.model.SecretCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Jetpack Security-backed storage for provider credentials. Responsible for encrypting secrets,
 * exposing typed accessors, and migrating legacy plaintext stores.
 */
@Singleton
class EncryptedSecretStore
private constructor(
  private val appContext: Context?,
  private val json: Json,
  private val suppliedClock: Clock?,
) {
  private val clock: Clock = suppliedClock ?: Clock.System

  @Inject
  constructor(
    @ApplicationContext context: Context,
    json: Json,
  ) : this(context, json, null)

  @VisibleForTesting constructor(json: Json, clock: Clock) : this(null, json, clock)

  @VisibleForTesting constructor() : this(null, defaultJson, Clock.System)

  private val masterKey: MasterKey? by lazy {
    appContext?.let { context ->
      MasterKey.Builder(context, MASTER_KEY_ALIAS)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .setRequestStrongBoxBacked(false)
        .build()
    }
  }

  private val encryptedPrefs: SharedPreferences? by lazy {
    val context = appContext ?: return@lazy null
    val key = masterKey ?: return@lazy null

    EncryptedSharedPreferences.create(
      context,
      PREFS_NAME,
      key,
      EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
      EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
  }

  /** Persist a credential value and associated metadata. */
  fun saveCredential(
    providerId: String,
    encryptedValue: String,
    scope: CredentialScope,
    rotatesAfter: Instant? = null,
    metadata: Map<String, String> = emptyMap(),
  ): SecretCredential {
    val prefs = encryptedPrefs ?: error("EncryptedSharedPreferences not initialised")

    val payload =
      SecretPayload(
        encryptedValue = encryptedValue,
        storedAt = clock.now().toEpochMilliseconds(),
        rotatesAfter = rotatesAfter?.toEpochMilliseconds(),
        scope = scope.name,
        metadata = metadata,
      )
    prefs.edit().putString(entryKey(providerId), json.encodeToString(payload)).apply()

    return payload.toCredential(providerId, MASTER_KEY_ALIAS)
  }

  /** Retrieve a stored credential if present. */
  @Suppress("ReturnCount") // Multiple validation checks
  fun getCredential(providerId: String): SecretCredential? {
    val prefs = encryptedPrefs ?: return null
    val rawPayload = prefs.getString(entryKey(providerId), null) ?: return null
    val payload =
      runCatching { json.decodeFromString<SecretPayload>(rawPayload) }.getOrNull() ?: return null

    return payload.toCredential(providerId, MASTER_KEY_ALIAS)
  }

  /** Remove a stored credential. */
  fun deleteCredential(providerId: String) {
    encryptedPrefs?.edit()?.remove(entryKey(providerId))?.apply()
  }

  /** Enumerate all stored credentials. */
  fun listCredentials(): List<SecretCredential> {
    val prefs = encryptedPrefs ?: return emptyList()
    return prefs.all
      .filterKeys { it.startsWith(ENTRY_PREFIX) }
      .mapNotNull { (key, value) ->
        val providerId = key.removePrefix(ENTRY_PREFIX)
        val payload =
          (value as? String)?.let { json.decodeFromString<SecretPayload>(it) }
            ?: return@mapNotNull null
        payload.toCredential(providerId, MASTER_KEY_ALIAS)
      }
  }

  /**
   * Migrate plaintext entries represented as mutable maps (used in unit tests to simulate legacy
   * stores).
   */
  fun migrateLegacySecrets(
    legacyStore: MutableMap<String, String>,
    encryptedStore: MutableMap<String, String>,
  ) {
    if (legacyStore.isEmpty()) return

    legacyStore.forEach { (key, value) -> encryptedStore[key] = hashSecret(value) }
    legacyStore.clear()
  }

  /** Delete the legacy preference file once migration succeeds. */
  fun deleteLegacyStore(legacyFile: File): Boolean {
    return if (legacyFile.exists()) legacyFile.delete() else false
  }

  /**
   * Migrate plaintext shared preferences into encrypted storage.
   *
   * @param legacyPrefs SharedPreferences instance containing plaintext secrets.
   * @param legacyFile Optional file reference to remove after migration.
   */
  fun migrateLegacyPreferences(legacyPrefs: SharedPreferences, legacyFile: File? = null) {
    val snapshot =
      legacyPrefs.all.mapNotNull { (key, value) -> (value as? String)?.let { key to it } }.toMap()
    if (snapshot.isEmpty()) return

    snapshot.forEach { (key, value) ->
      saveCredential(
        providerId = key,
        encryptedValue = value,
        scope = CredentialScope.TEXT_INFERENCE,
      )
    }

    legacyPrefs.edit().clear().apply()
    legacyFile?.let { deleteLegacyStore(it) }
  }

  private fun entryKey(providerId: String): String = "$ENTRY_PREFIX$providerId"

  private fun hashSecret(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashed = digest.digest(value.toByteArray())
    return hashed.joinToString(separator = "") { byte -> String.format(Locale.US, "%02x", byte) }
  }

  @Serializable
  private data class SecretPayload(
    val encryptedValue: String,
    val storedAt: Long,
    val rotatesAfter: Long? = null,
    val scope: String,
    val metadata: Map<String, String> = emptyMap(),
  ) {
    fun toCredential(providerId: String, alias: String): SecretCredential =
      SecretCredential(
        providerId = providerId,
        encryptedValue = encryptedValue,
        keyAlias = alias,
        storedAt = Instant.fromEpochMilliseconds(storedAt),
        rotatesAfter = rotatesAfter?.let(Instant::fromEpochMilliseconds),
        scope =
          runCatching { CredentialScope.valueOf(scope) }
            .getOrDefault(CredentialScope.TEXT_INFERENCE),
        metadata = metadata,
      )
  }

  companion object {
    private const val PREFS_NAME = "encrypted_provider_prefs"
    private const val ENTRY_PREFIX = "credential:"
    private const val MASTER_KEY_ALIAS = "nanoai.encrypted.master"

    private val defaultJson: Json = Json {
      ignoreUnknownKeys = true
      encodeDefaults = true
      explicitNulls = false
    }

    @VisibleForTesting
    fun createForTesting(
      json: Json = defaultJson,
      clock: Clock = Clock.System,
    ): EncryptedSecretStore = EncryptedSecretStore(null, json, clock)
  }
}
