package com.vjaykrsna.nanoai.security

import com.google.common.truth.Truth.assertThat
import kotlin.io.path.createTempFile
import org.junit.Test

/**
 * Migration contract test for EncryptedSecretStore.
 *
 * TDD note: This test intentionally fails until the production migration copies
 * legacy plaintext entries into the encrypted store and deletes the legacy file.
 */
class EncryptedSecretStoreMigrationTest {
    @Test
    fun `migration should copy plaintext secrets into encrypted store`() {
        val legacyStore = mutableMapOf("provider.openai" to "plaintext-key")
        val encryptedStore = mutableMapOf<String, String>()

        val migrationResult =
            runCatching {
                val storeClass = Class.forName("com.vjaykrsna.nanoai.security.EncryptedSecretStore")
                val migrateMethod =
                    storeClass.getDeclaredMethod(
                        "migrateLegacySecrets",
                        MutableMap::class.java,
                        MutableMap::class.java,
                    )
                val instance = storeClass.getDeclaredConstructor().newInstance()
                migrateMethod.invoke(instance, legacyStore, encryptedStore)
            }

        assertThat(migrationResult.isSuccess).isTrue()
        assertThat(legacyStore).isEmpty()
        val encryptedValue = encryptedStore["provider.openai"]
        assertThat(encryptedValue).isNotNull()
        assertThat(encryptedValue).isNotEqualTo("plaintext-key")
    }

    @Test
    fun `migration should delete legacy preference file`() {
        val migrationResult =
            runCatching {
                val storeClass = Class.forName("com.vjaykrsna.nanoai.security.EncryptedSecretStore")
                val deleteLegacyMethod =
                    storeClass.getDeclaredMethod(
                        "deleteLegacyStore",
                        java.io.File::class.java,
                    )

                val legacyFile = createTempFile(prefix = "legacy-secrets", suffix = ".xml").toFile()
                legacyFile.writeText("<map><string name=\"provider.openai\">plaintext</string></map>")

                deleteLegacyMethod.invoke(storeClass.getDeclaredConstructor().newInstance(), legacyFile)
                legacyFile.exists()
            }

        assertThat(migrationResult.isSuccess).isTrue()
        assertThat(migrationResult.getOrThrow()).isFalse()
    }
}
