package com.vjaykrsna.nanoai.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.vjaykrsna.nanoai.core.security.model.CredentialScope
import com.vjaykrsna.nanoai.core.security.model.SecretCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Keystore-backed credential store that persists provider secrets as encrypted JSON blobs. */
@Singleton
class EncryptedSecretStore
private constructor(private val persistence: SecretPersistence, private val suppliedClock: Clock?) {
  private val clock: Clock = suppliedClock ?: Clock.System
  private val lock = ReentrantReadWriteLock()

  @Inject
  constructor(
    @ApplicationContext context: Context,
    json: Json,
  ) : this(
    persistence =
      KeystoreSecretPersistence(
        file = File(context.filesDir, PREFS_FILE_NAME),
        json = json,
        crypto = KeystoreSecretCrypto(MASTER_KEY_ALIAS),
      ),
    suppliedClock = null,
  )

  /** Visible for unit tests that don't have access to AndroidKeyStore. */
  @VisibleForTesting
  constructor(json: Json, clock: Clock) : this(InMemorySecretPersistence(json), clock)

  /** Persist a credential value and associated metadata. */
  fun saveCredential(
    providerId: String,
    encryptedValue: String,
    scope: CredentialScope,
    rotatesAfter: Instant? = null,
    metadata: Map<String, String> = emptyMap(),
  ): SecretCredential {
    val payload =
      SecretPayload(
        encryptedValue = encryptedValue,
        storedAt = clock.now().toEpochMilliseconds(),
        rotatesAfter = rotatesAfter?.toEpochMilliseconds(),
        scope = scope.name,
        metadata = metadata,
      )

    writeStore { entries ->
      entries[providerId] = payload
      true
    }

    return payload.toCredential(providerId)
  }

  /** Retrieve a stored credential if present. */
  fun getCredential(providerId: String): SecretCredential? = readStore { entries ->
    entries[providerId]?.toCredential(providerId)
  }

  /** Remove a stored credential. */
  fun deleteCredential(providerId: String) {
    writeStore { entries -> entries.remove(providerId) != null }
  }

  /** Enumerate all stored credentials. */
  fun listCredentials(): List<SecretCredential> = readStore { entries ->
    entries.map { (id, payload) -> payload.toCredential(id) }
  }

  private inline fun <T> readStore(transform: (Map<String, SecretPayload>) -> T): T =
    lock.read { persistence.read().let(transform) }

  private inline fun writeStore(mutator: (MutableMap<String, SecretPayload>) -> Boolean) {
    lock.write {
      val entries = persistence.read().toMutableMap()
      val changed = mutator(entries)
      if (changed) {
        persistence.write(entries)
      }
    }
  }

  @Serializable
  private data class SecretPayload(
    val encryptedValue: String,
    val storedAt: Long,
    val rotatesAfter: Long? = null,
    val scope: String,
    val metadata: Map<String, String> = emptyMap(),
  ) {
    fun toCredential(providerId: String): SecretCredential =
      SecretCredential(
        providerId = providerId,
        encryptedValue = encryptedValue,
        keyAlias = MASTER_KEY_ALIAS,
        storedAt = Instant.fromEpochMilliseconds(storedAt),
        rotatesAfter = rotatesAfter?.let(Instant::fromEpochMilliseconds),
        scope =
          runCatching { CredentialScope.valueOf(scope) }
            .getOrDefault(CredentialScope.TEXT_INFERENCE),
        metadata = metadata,
      )
  }

  private interface SecretPersistence {
    fun read(): Map<String, SecretPayload>

    fun write(entries: Map<String, SecretPayload>)
  }

  private class KeystoreSecretPersistence(
    private val file: File,
    private val json: Json,
    private val crypto: SecretCrypto,
  ) : SecretPersistence {
    private val serializer = MapSerializer(String.serializer(), SecretPayload.serializer())

    override fun read(): Map<String, SecretPayload> {
      if (!file.exists() || file.length() == 0L) return emptyMap()
      return try {
        val encrypted = file.readBytes()
        val decrypted = crypto.decrypt(encrypted)
        json.decodeFromString(serializer, decrypted.decodeToString())
      } catch (exception: Exception) {
        Log.e(TAG, "Failed to read encrypted provider store", exception)
        throw IllegalStateException("Unable to read encrypted provider store", exception)
      }
    }

    override fun write(entries: Map<String, SecretPayload>) {
      try {
        if (entries.isEmpty()) {
          if (file.exists()) file.delete()
          return
        }
        file.parentFile?.mkdirs()
        val payload = json.encodeToString(serializer, entries)
        val encrypted = crypto.encrypt(payload.encodeToByteArray())
        file.outputStream().use { stream -> stream.write(encrypted) }
      } catch (exception: Exception) {
        Log.e(TAG, "Failed to write encrypted provider store", exception)
        throw IllegalStateException("Unable to write encrypted provider store", exception)
      }
    }
  }

  private class InMemorySecretPersistence(json: Json) : SecretPersistence {
    private var entries: MutableMap<String, SecretPayload> = mutableMapOf()
    private val serializer = MapSerializer(String.serializer(), SecretPayload.serializer())
    private val copyJson = json

    override fun read(): Map<String, SecretPayload> = entries.toMap()

    override fun write(entries: Map<String, SecretPayload>) {
      // Make a deep copy via JSON so tests can't mutate references across calls.
      val encoded = copyJson.encodeToString(serializer, entries)
      this.entries = copyJson.decodeFromString(serializer, encoded).toMutableMap()
    }
  }

  private interface SecretCrypto {
    fun encrypt(plaintext: ByteArray): ByteArray

    fun decrypt(ciphertext: ByteArray): ByteArray
  }

  private class KeystoreSecretCrypto(private val keyAlias: String) : SecretCrypto {
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    override fun encrypt(plaintext: ByteArray): ByteArray =
      try {
        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(plaintext)
        val iv = cipher.iv
        ByteBuffer.allocate(INT_BYTES + iv.size + ciphertext.size)
          .putInt(iv.size)
          .put(iv)
          .put(ciphertext)
          .array()
      } catch (exception: GeneralSecurityException) {
        throw IllegalStateException("Unable to encrypt credential", exception)
      }

    override fun decrypt(ciphertext: ByteArray): ByteArray =
      try {
        val buffer = ByteBuffer.wrap(ciphertext)
        val ivLength = buffer.int
        require(ivLength in MIN_IV_LENGTH..MAX_IV_LENGTH) { "Invalid IV length: $ivLength" }
        val iv = ByteArray(ivLength).also { buffer.get(it) }
        val encryptedPayload = ByteArray(buffer.remaining()).also { buffer.get(it) }
        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(
          Cipher.DECRYPT_MODE,
          getOrCreateKey(),
          GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv),
        )
        cipher.doFinal(encryptedPayload)
      } catch (invalidated: KeyPermanentlyInvalidatedException) {
        deleteKey()
        throw IllegalStateException("Encryption key permanently invalidated", invalidated)
      } catch (badTag: AEADBadTagException) {
        throw IllegalStateException("Stored credential cannot be decrypted (bad tag)", badTag)
      } catch (exception: GeneralSecurityException) {
        throw IllegalStateException("Unable to decrypt credential", exception)
      }

    private fun getOrCreateKey(): SecretKey {
      val existing = (keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.secretKey
      if (existing != null) return existing
      val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
      val spec =
        KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
          )
          .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
          .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
          .setKeySize(AES_KEY_SIZE_BITS)
          .setUserAuthenticationRequired(false)
          .build()
      keyGenerator.init(spec)
      return keyGenerator.generateKey()
    }

    private fun deleteKey() {
      runCatching { keyStore.deleteEntry(keyAlias) }
    }
  }

  companion object {
    private const val PREFS_FILE_NAME = "encrypted_provider_store.json"
    private const val MASTER_KEY_ALIAS = "nanoai.encrypted.master"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val AES_MODE = "AES/GCM/NoPadding"
    private const val AES_KEY_SIZE_BITS = 256
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val INT_BYTES = 4
    private const val MIN_IV_LENGTH = 12
    private const val MAX_IV_LENGTH = 16
    private const val TAG = "EncryptedSecretStore"

    private val defaultJson: Json = Json {
      ignoreUnknownKeys = true
      encodeDefaults = true
      explicitNulls = false
    }

    @VisibleForTesting
    fun createForTesting(
      json: Json = defaultJson,
      clock: Clock = Clock.System,
    ): EncryptedSecretStore = EncryptedSecretStore(json, clock)
  }
}
