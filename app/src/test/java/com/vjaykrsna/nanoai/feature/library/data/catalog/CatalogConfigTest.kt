package com.vjaykrsna.nanoai.feature.library.data.catalog

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.feature.library.model.InstallState
import com.vjaykrsna.nanoai.feature.library.model.ProviderType
import com.vjaykrsna.nanoai.model.catalog.DeliveryType
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

  private object FixedClock : Clock {
    override fun now(): Instant = Instant.parse("2025-01-01T00:00:00Z")
  }
}
