package com.vjaykrsna.nanoai.core.data.library.catalog

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.library.DeliveryType
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.junit.Test

class CatalogConfigTest {
  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun `toModelPackage maps configuration to domain model`() {
    val payload =
      """
      {
        "id": "gemma-test",
        "display_name": "Gemma Test",
        "version": "1.2.3",
        "provider": "media_pipe",
        "delivery": "cloud_fallback",
        "min_app_version": 42,
        "size_bytes": 123456789,
        "capabilities": ["chat", "text-generation", "multimodal"],
        "manifest_url": "hf://example/model",
        "checksum_sha256": "abc123",
        "signature": "sig",
        "created_at": "2024-01-01T00:00:00Z",
        "updated_at": "2024-02-02T00:00:00Z"
      }
      """
        .trimIndent()

    val config = json.decodeFromString(ModelConfig.serializer(), payload)
    val model = config.toModelPackage(FixedClock)

    assertThat(model.modelId).isEqualTo("gemma-test")
    assertThat(model.displayName).isEqualTo("Gemma Test")
    assertThat(model.version).isEqualTo("1.2.3")
    assertThat(model.providerType).isEqualTo(ProviderType.MEDIA_PIPE)
    assertThat(model.deliveryType).isEqualTo(DeliveryType.CLOUD_FALLBACK)
    assertThat(model.minAppVersion).isEqualTo(42)
    assertThat(model.sizeBytes).isEqualTo(123456789)
    assertThat(model.capabilities).containsExactly("chat", "text-generation", "multimodal")
    assertThat(model.installState).isEqualTo(InstallState.NOT_INSTALLED)
    assertThat(model.manifestUrl).isEqualTo("hf://example/model")
    assertThat(model.checksumSha256).isEqualTo("abc123")
    assertThat(model.signature).isEqualTo("sig")
    assertThat(model.createdAt).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"))
    assertThat(model.updatedAt).isEqualTo(Instant.parse("2024-02-02T00:00:00Z"))
  }

  @Test(expected = IllegalArgumentException::class)
  fun `invalid provider type throws descriptive exception`() {
    val payload =
      """
      {
        "id": "invalid",
        "display_name": "Invalid",
        "version": "1.0",
        "provider": "unknown",
        "delivery": "cloud_fallback",
        "min_app_version": 1,
        "size_bytes": 1,
        "capabilities": [],
        "manifest_url": "hf://example/invalid"
      }
      """
        .trimIndent()

    val config = json.decodeFromString(ModelConfig.serializer(), payload)
    config.toModelPackage(FixedClock)
  }

  @Test
  fun `toModelPackage trims blank capabilities`() {
    val payload =
      """
      {
        "id": "capability-test",
        "display_name": "Capability Test",
        "version": "1.0",
        "provider": "cloud_api",
        "delivery": "cloud_fallback",
        "min_app_version": 5,
        "size_bytes": 2048,
        "capabilities": [" text ", " ", "vision"],
        "manifest_url": "hf://example/capability"
      }
      """
        .trimIndent()

    val config = json.decodeFromString(ModelConfig.serializer(), payload)

    val model = config.toModelPackage(FixedClock)

    assertThat(model.capabilities).containsExactly("text", "vision")
  }

  @Test
  fun `toModelPackage falls back to clock when timestamps missing`() {
    val payload =
      """
      {
        "id": "timestamp-test",
        "display_name": "Timestamp Test",
        "version": "1.0",
        "provider": "cloud_api",
        "delivery": "cloud_fallback",
        "min_app_version": 1,
        "size_bytes": 1,
        "capabilities": [],
        "manifest_url": "hf://example/timestamp"
      }
      """
        .trimIndent()

    val config = json.decodeFromString(ModelConfig.serializer(), payload)

    val model = config.toModelPackage(FixedClock)

    assertThat(model.createdAt).isEqualTo(FixedClock.now())
    assertThat(model.updatedAt).isEqualTo(FixedClock.now())
  }

  @Test(expected = IllegalArgumentException::class)
  fun `invalid delivery type throws descriptive exception`() {
    val payload =
      """
      {
        "id": "invalid-delivery",
        "display_name": "Invalid Delivery",
        "version": "1.0",
        "provider": "cloud_api",
        "delivery": "unknown",
        "min_app_version": 1,
        "size_bytes": 1,
        "capabilities": [],
        "manifest_url": "hf://example/invalid"
      }
      """
        .trimIndent()

    val config = json.decodeFromString(ModelConfig.serializer(), payload)
    config.toModelPackage(FixedClock)
  }

  @Test
  fun `ModelCatalogConfig defaults to version one`() {
    val payload = """{"models": []}"""

    val config = json.decodeFromString(ModelCatalogConfig.serializer(), payload)

    assertThat(config.version).isEqualTo(1)
    assertThat(config.models).isEmpty()
  }

  private object FixedClock : Clock {
    override fun now(): Instant = Instant.parse("2025-01-01T00:00:00Z")
  }
}
