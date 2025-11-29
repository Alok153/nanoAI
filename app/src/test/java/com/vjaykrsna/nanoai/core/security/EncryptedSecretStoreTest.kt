package com.vjaykrsna.nanoai.core.security

import com.vjaykrsna.nanoai.core.security.model.CredentialScope
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.plus
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EncryptedSecretStoreTest {
  private lateinit var store: EncryptedSecretStore
  private lateinit var clock: FakeClock

  @BeforeEach
  fun setUp() {
    clock = FakeClock(Instant.fromEpochMilliseconds(1_000L))
    store =
      EncryptedSecretStore.createForTesting(
        json =
          Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
            explicitNulls = false
          },
        clock = clock,
      )
  }

  @Test
  fun `saveCredential stores data that can be read back`() {
    val rotatesAfter = clock.now().plus(1_000L, kotlinx.datetime.DateTimeUnit.MILLISECOND)
    val metadata = mapOf("region" to "us", "tier" to "pro")

    val saved =
      store.saveCredential(
        providerId = "media-pipe",
        encryptedValue = "secret-value",
        scope = CredentialScope.TEXT_INFERENCE,
        rotatesAfter = rotatesAfter,
        metadata = metadata,
      )

    val loaded = store.getCredential("media-pipe")

    assertEquals(saved, loaded)
    requireNotNull(loaded)
    assertEquals(rotatesAfter, loaded.rotatesAfter)
    assertEquals(metadata, loaded.metadata)
    assertEquals("nanoai.encrypted.master", loaded.keyAlias)
  }

  @Test
  fun `listCredentials returns all saved providers`() {
    store.saveCredential("p1", "token-1", CredentialScope.TEXT_INFERENCE)
    clock.advanceBy(5_000)
    store.saveCredential("p2", "token-2", CredentialScope.AUDIO)

    val credentials = store.listCredentials()

    assertEquals(2, credentials.size)
    val providerIds = credentials.map { it.providerId }.toSet()
    assertTrue(providerIds.containsAll(listOf("p1", "p2")))
  }

  @Test
  fun `deleteCredential removes provider from store`() {
    store.saveCredential("p1", "token-1", CredentialScope.TEXT_INFERENCE)

    store.deleteCredential("p1")

    assertNull(store.getCredential("p1"))
  }

  @Test
  fun `saveCredential overwrites existing provider and updates timestamp`() {
    store.saveCredential("p1", "token-1", CredentialScope.TEXT_INFERENCE)
    clock.advanceBy(10_000)

    val updated = store.saveCredential("p1", "token-new", CredentialScope.TEXT_INFERENCE)

    assertEquals("token-new", store.getCredential("p1")?.encryptedValue)
    assertEquals(clock.now(), updated.storedAt)
  }

  @Test
  fun `deleteCredential ignores missing provider`() {
    store.saveCredential("p1", "token-1", CredentialScope.TEXT_INFERENCE)

    store.deleteCredential("missing")

    assertEquals("token-1", store.getCredential("p1")?.encryptedValue)
  }

  private class FakeClock(var instant: Instant) : Clock {
    override fun now(): Instant = instant

    fun advanceBy(millis: Long) {
      instant = Instant.fromEpochMilliseconds(instant.toEpochMilliseconds() + millis)
    }
  }
}
