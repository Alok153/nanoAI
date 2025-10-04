package com.vjaykrsna.nanoai.security

import android.app.Application
import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.security.model.CredentialScope
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class SecretMigrationInitializerTest {
  private val context: Context = RuntimeEnvironment.getApplication()

  @BeforeTest
  fun resetState() {
    context.deleteSharedPreferences(MIGRATION_STATE_PREFS)
    LEGACY_PREF_NAMES.forEach { name -> context.deleteSharedPreferences(name) }
    LEGACY_FILES.forEach { fileName -> File(context.filesDir, fileName).delete() }
  }

  @Test
  fun `migrate copies secrets clears legacy stores and marks complete`() = runTest {
    val dispatcher = UnconfinedTestDispatcher(testScheduler)
    val secretStore = mockk<EncryptedSecretStore>(relaxed = true)
    val initializer = SecretMigrationInitializer(context, secretStore, dispatcher)

    context
      .getSharedPreferences("legacy_provider_config", Context.MODE_PRIVATE)
      .edit()
      .putString("provider.openai", "sk-test-123")
      .putString("provider.anthropic", "key-abc")
      .commit()

    every { secretStore.saveCredential(any(), any(), any(), any(), any()) } answers
      {
        val providerId = firstArg<String>()
        val rawValue = secondArg<String>()
        SecretCredentialFactory.create(providerId = providerId, encryptedValue = rawValue)
      }
    every { secretStore.deleteLegacyStore(any()) } answers { firstArg<File>().delete() }

    File(context.filesDir, "legacy-secrets.xml").writeText("<map></map>")

    initializer.runMigrationBlocking()

    val metadataSlotOne: CapturingSlot<Map<String, String>> = slot()
    val metadataSlotTwo: CapturingSlot<Map<String, String>> = slot()
    verify(exactly = 1) {
      secretStore.saveCredential(
        providerId = "provider.openai",
        encryptedValue = "sk-test-123",
        scope = CredentialScope.TEXT_INFERENCE,
        metadata = capture(metadataSlotOne),
      )
    }
    verify(exactly = 1) {
      secretStore.saveCredential(
        providerId = "provider.anthropic",
        encryptedValue = "key-abc",
        scope = CredentialScope.TEXT_INFERENCE,
        metadata = capture(metadataSlotTwo),
      )
    }
    val legacyFileSlot: CapturingSlot<File> = slot()
    verify { secretStore.deleteLegacyStore(capture(legacyFileSlot)) }

    assertThat(metadataSlotOne.isCaptured).isTrue()
    assertThat(metadataSlotOne.captured["source"]).isEqualTo("legacy_provider_config")
    assertThat(metadataSlotOne.captured["migratedAt"]).isNotNull()
    assertThat(metadataSlotTwo.isCaptured).isTrue()
    assertThat(metadataSlotTwo.captured["source"]).isEqualTo("legacy_provider_config")
    assertThat(metadataSlotTwo.captured["migratedAt"]).isNotNull()
    assertThat(legacyFileSlot.isCaptured).isTrue()
    assertThat(legacyFileSlot.captured.name).isEqualTo("legacy-secrets.xml")

    val legacyPrefs = context.getSharedPreferences("legacy_provider_config", Context.MODE_PRIVATE)
    assertThat(legacyPrefs.all).isEmpty()

    val statePrefs = context.getSharedPreferences(MIGRATION_STATE_PREFS, Context.MODE_PRIVATE)
    assertThat(statePrefs.getBoolean("completed", false)).isTrue()
    assertThat(statePrefs.getInt("migrated_count", 0)).isEqualTo(2)
  }

  @Test
  fun `migrate skips when already completed`() = runTest {
    val dispatcher = UnconfinedTestDispatcher(testScheduler)
    val secretStore = mockk<EncryptedSecretStore>(relaxed = true)
    val initializer = SecretMigrationInitializer(context, secretStore, dispatcher)

    context
      .getSharedPreferences(MIGRATION_STATE_PREFS, Context.MODE_PRIVATE)
      .edit()
      .putBoolean("completed", true)
      .apply()

    initializer.runMigrationBlocking()

    verify(exactly = 0) { secretStore.saveCredential(any(), any(), any(), any(), any()) }
    verify(exactly = 0) { secretStore.deleteLegacyStore(any()) }
  }

  @Test
  fun `migrate marks complete when no legacy data`() = runTest {
    val dispatcher = UnconfinedTestDispatcher(testScheduler)
    val secretStore = mockk<EncryptedSecretStore>(relaxed = true)
    val initializer = SecretMigrationInitializer(context, secretStore, dispatcher)

    initializer.runMigrationBlocking()

    verify(exactly = 0) { secretStore.saveCredential(any(), any(), any(), any(), any()) }

    val statePrefs = context.getSharedPreferences(MIGRATION_STATE_PREFS, Context.MODE_PRIVATE)
    assertThat(statePrefs.getBoolean("completed", false)).isTrue()
    assertThat(statePrefs.getInt("migrated_count", 0)).isEqualTo(0)
  }

  private object SecretCredentialFactory {
    fun create(providerId: String, encryptedValue: String) =
      com.vjaykrsna.nanoai.security.model.SecretCredential(
        providerId = providerId,
        encryptedValue = encryptedValue,
        keyAlias = "nanoai.encrypted.master",
        storedAt = Instant.fromEpochMilliseconds(0),
        rotatesAfter = null,
        scope = CredentialScope.TEXT_INFERENCE,
        metadata = emptyMap(),
      )
  }

  companion object {
    private const val MIGRATION_STATE_PREFS = "secret_migration_state"
    private val LEGACY_PREF_NAMES =
      listOf("legacy_provider_config", "provider_config", "api_provider_config")
    private val LEGACY_FILES =
      listOf("legacy-secrets.xml", "provider-secrets.xml", "provider_config.json")
  }
}
