package com.vjaykrsna.nanoai.core.data.db.mappers

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.db.entities.ModelPackageEntity
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.library.DeliveryType
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import java.util.UUID
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test

class ModelPackageMapperTest {

  @Test
  fun `toDomain maps all fields correctly`() {
    val downloadTaskId = UUID.randomUUID().toString()
    val createdAt = Instant.parse("2024-01-15T10:30:00Z")
    val updatedAt = Instant.parse("2024-01-15T11:00:00Z")

    val entity =
      ModelPackageEntity(
        modelId = "gemma-2b-it",
        displayName = "Gemma 2B IT",
        version = "1.0.0",
        providerType = ProviderType.MEDIA_PIPE,
        deliveryType = DeliveryType.LOCAL_ARCHIVE,
        minAppVersion = 1,
        sizeBytes = 2_000_000_000L,
        capabilities = setOf("text-generation", "chat"),
        installState = InstallState.INSTALLED,
        downloadTaskId = downloadTaskId,
        manifestUrl = "https://example.com/gemma-2b-it.json",
        checksumSha256 = "abc123",
        signature = "sig456",
        createdAt = createdAt,
        updatedAt = updatedAt,
      )

    val domain = ModelPackageMapper.toDomain(entity)

    assertThat(domain.modelId).isEqualTo("gemma-2b-it")
    assertThat(domain.displayName).isEqualTo("Gemma 2B IT")
    assertThat(domain.version).isEqualTo("1.0.0")
    assertThat(domain.providerType).isEqualTo(ProviderType.MEDIA_PIPE)
    assertThat(domain.deliveryType).isEqualTo(DeliveryType.LOCAL_ARCHIVE)
    assertThat(domain.minAppVersion).isEqualTo(1)
    assertThat(domain.sizeBytes).isEqualTo(2_000_000_000L)
    assertThat(domain.capabilities).containsExactly("text-generation", "chat")
    assertThat(domain.installState).isEqualTo(InstallState.INSTALLED)
    assertThat(domain.downloadTaskId).isEqualTo(UUID.fromString(downloadTaskId))
    assertThat(domain.manifestUrl).isEqualTo("https://example.com/gemma-2b-it.json")
    assertThat(domain.checksumSha256).isEqualTo("abc123")
    assertThat(domain.signature).isEqualTo("sig456")
    assertThat(domain.createdAt).isEqualTo(createdAt)
    assertThat(domain.updatedAt).isEqualTo(updatedAt)
  }

  @Test
  fun `toEntity maps all fields correctly`() {
    val downloadTaskId = UUID.randomUUID()
    val createdAt = Instant.parse("2024-01-15T10:30:00Z")
    val updatedAt = Instant.parse("2024-01-15T11:00:00Z")

    val domain =
      ModelPackage(
        modelId = "phi-2",
        displayName = "Phi-2",
        version = "2.0.0",
        providerType = ProviderType.TFLITE,
        deliveryType = DeliveryType.PLAY_ASSET,
        minAppVersion = 2,
        sizeBytes = 3_500_000_000L,
        capabilities = setOf("code-generation"),
        installState = InstallState.DOWNLOADING,
        downloadTaskId = downloadTaskId,
        manifestUrl = "https://example.com/phi-2.json",
        checksumSha256 = "def789",
        signature = "sig789",
        createdAt = createdAt,
        updatedAt = updatedAt,
      )

    val entity = ModelPackageMapper.toEntity(domain)

    assertThat(entity.modelId).isEqualTo("phi-2")
    assertThat(entity.displayName).isEqualTo("Phi-2")
    assertThat(entity.version).isEqualTo("2.0.0")
    assertThat(entity.providerType).isEqualTo(ProviderType.TFLITE)
    assertThat(entity.deliveryType).isEqualTo(DeliveryType.PLAY_ASSET)
    assertThat(entity.minAppVersion).isEqualTo(2)
    assertThat(entity.sizeBytes).isEqualTo(3_500_000_000L)
    assertThat(entity.capabilities).containsExactly("code-generation")
    assertThat(entity.installState).isEqualTo(InstallState.DOWNLOADING)
    assertThat(entity.downloadTaskId).isEqualTo(downloadTaskId.toString())
    assertThat(entity.manifestUrl).isEqualTo("https://example.com/phi-2.json")
    assertThat(entity.checksumSha256).isEqualTo("def789")
    assertThat(entity.signature).isEqualTo("sig789")
    assertThat(entity.createdAt).isEqualTo(createdAt)
    assertThat(entity.updatedAt).isEqualTo(updatedAt)
  }

  @Test
  fun `round trip conversion preserves core data`() {
    val original =
      ModelPackage(
        modelId = "llama-3b",
        displayName = "LLaMA 3B",
        version = "1.5.0",
        providerType = ProviderType.MLC_LLM,
        deliveryType = DeliveryType.LOCAL_ARCHIVE,
        minAppVersion = 3,
        sizeBytes = 5_000_000_000L,
        capabilities = setOf("text-generation", "summarization"),
        installState = InstallState.NOT_INSTALLED,
        downloadTaskId = null,
        manifestUrl = "https://example.com/llama-3b.json",
        checksumSha256 = null,
        signature = null,
        createdAt = Instant.parse("2024-02-01T08:00:00Z"),
        updatedAt = Instant.parse("2024-02-01T09:00:00Z"),
      )

    val entity = ModelPackageMapper.toEntity(original)
    val roundTrip = ModelPackageMapper.toDomain(entity)

    // Core fields should match
    assertThat(roundTrip.modelId).isEqualTo(original.modelId)
    assertThat(roundTrip.displayName).isEqualTo(original.displayName)
    assertThat(roundTrip.version).isEqualTo(original.version)
    assertThat(roundTrip.providerType).isEqualTo(original.providerType)
    assertThat(roundTrip.deliveryType).isEqualTo(original.deliveryType)
    assertThat(roundTrip.installState).isEqualTo(original.installState)
    assertThat(roundTrip.manifestUrl).isEqualTo(original.manifestUrl)
  }

  @Test
  fun `handles null downloadTaskId correctly`() {
    val entity =
      ModelPackageEntity(
        modelId = "test-model",
        displayName = "Test Model",
        version = "1.0.0",
        providerType = ProviderType.MEDIA_PIPE,
        deliveryType = DeliveryType.LOCAL_ARCHIVE,
        minAppVersion = 1,
        sizeBytes = 1000L,
        capabilities = emptySet(),
        installState = InstallState.NOT_INSTALLED,
        downloadTaskId = null,
        manifestUrl = "https://example.com/test.json",
        checksumSha256 = null,
        signature = null,
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
      )

    val domain = ModelPackageMapper.toDomain(entity)

    assertThat(domain.downloadTaskId).isNull()
  }

  @Test
  fun `handles null checksum and signature correctly`() {
    val domain =
      ModelPackage(
        modelId = "no-integrity",
        displayName = "No Integrity Model",
        version = "1.0.0",
        providerType = ProviderType.CLOUD_API,
        deliveryType = DeliveryType.CLOUD_FALLBACK,
        minAppVersion = 1,
        sizeBytes = 0L,
        capabilities = emptySet(),
        installState = InstallState.INSTALLED,
        downloadTaskId = null,
        manifestUrl = "https://api.example.com",
        checksumSha256 = null,
        signature = null,
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
      )

    val entity = ModelPackageMapper.toEntity(domain)

    assertThat(entity.checksumSha256).isNull()
    assertThat(entity.signature).isNull()
  }

  @Test
  fun `handles empty capabilities set`() {
    val entity =
      ModelPackageEntity(
        modelId = "empty-caps",
        displayName = "Empty Caps Model",
        version = "1.0.0",
        providerType = ProviderType.ONNX_RUNTIME,
        deliveryType = DeliveryType.LOCAL_ARCHIVE,
        minAppVersion = 1,
        sizeBytes = 1000L,
        capabilities = emptySet(),
        installState = InstallState.NOT_INSTALLED,
        downloadTaskId = null,
        manifestUrl = "https://example.com",
        checksumSha256 = null,
        signature = null,
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
      )

    val domain = ModelPackageMapper.toDomain(entity)

    assertThat(domain.capabilities).isEmpty()
  }

  @Test
  fun `all ProviderType values can be mapped`() {
    ProviderType.values().forEach { providerType ->
      val entity =
        ModelPackageEntity(
          modelId = "test-$providerType",
          displayName = "Test",
          version = "1.0.0",
          providerType = providerType,
          deliveryType = DeliveryType.LOCAL_ARCHIVE,
          minAppVersion = 1,
          sizeBytes = 1000L,
          capabilities = emptySet(),
          installState = InstallState.NOT_INSTALLED,
          downloadTaskId = null,
          manifestUrl = "https://example.com",
          checksumSha256 = null,
          signature = null,
          createdAt = Instant.parse("2024-01-01T00:00:00Z"),
          updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
        )

      val domain = ModelPackageMapper.toDomain(entity)
      val roundTrip = ModelPackageMapper.toEntity(domain)

      assertThat(roundTrip.providerType).isEqualTo(providerType)
    }
  }

  @Test
  fun `all DeliveryType values can be mapped`() {
    DeliveryType.values().forEach { deliveryType ->
      val entity =
        ModelPackageEntity(
          modelId = "test-$deliveryType",
          displayName = "Test",
          version = "1.0.0",
          providerType = ProviderType.MEDIA_PIPE,
          deliveryType = deliveryType,
          minAppVersion = 1,
          sizeBytes = 1000L,
          capabilities = emptySet(),
          installState = InstallState.NOT_INSTALLED,
          downloadTaskId = null,
          manifestUrl = "https://example.com",
          checksumSha256 = null,
          signature = null,
          createdAt = Instant.parse("2024-01-01T00:00:00Z"),
          updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
        )

      val domain = ModelPackageMapper.toDomain(entity)
      val roundTrip = ModelPackageMapper.toEntity(domain)

      assertThat(roundTrip.deliveryType).isEqualTo(deliveryType)
    }
  }

  @Test
  fun `all InstallState values can be mapped`() {
    InstallState.values().forEach { installState ->
      val entity =
        ModelPackageEntity(
          modelId = "test-$installState",
          displayName = "Test",
          version = "1.0.0",
          providerType = ProviderType.MEDIA_PIPE,
          deliveryType = DeliveryType.LOCAL_ARCHIVE,
          minAppVersion = 1,
          sizeBytes = 1000L,
          capabilities = emptySet(),
          installState = installState,
          downloadTaskId = null,
          manifestUrl = "https://example.com",
          checksumSha256 = null,
          signature = null,
          createdAt = Instant.parse("2024-01-01T00:00:00Z"),
          updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
        )

      val domain = ModelPackageMapper.toDomain(entity)
      val roundTrip = ModelPackageMapper.toEntity(domain)

      assertThat(roundTrip.installState).isEqualTo(installState)
    }
  }

  @Test
  fun `extension function toDomain works correctly`() {
    val entity =
      ModelPackageEntity(
        modelId = "ext-test",
        displayName = "Extension Test",
        version = "1.0.0",
        providerType = ProviderType.LEAP,
        deliveryType = DeliveryType.LOCAL_ARCHIVE,
        minAppVersion = 1,
        sizeBytes = 1000L,
        capabilities = emptySet(),
        installState = InstallState.NOT_INSTALLED,
        downloadTaskId = null,
        manifestUrl = "https://example.com",
        checksumSha256 = null,
        signature = null,
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
      )

    val domain = entity.toDomain()

    assertThat(domain.displayName).isEqualTo("Extension Test")
  }

  @Test
  fun `extension function toEntity works correctly`() {
    val domain =
      ModelPackage(
        modelId = "ext-entity-test",
        displayName = "Extension Entity Test",
        version = "1.0.0",
        providerType = ProviderType.MEDIA_PIPE,
        deliveryType = DeliveryType.LOCAL_ARCHIVE,
        minAppVersion = 1,
        sizeBytes = 1000L,
        capabilities = emptySet(),
        installState = InstallState.NOT_INSTALLED,
        manifestUrl = "https://example.com",
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
      )

    val entity = domain.toEntity()

    assertThat(entity.displayName).isEqualTo("Extension Entity Test")
  }

  @Test
  fun `handles large size bytes values`() {
    val largeSize = 10_000_000_000L // 10GB

    val domain =
      ModelPackage(
        modelId = "large-model",
        displayName = "Large Model",
        version = "1.0.0",
        providerType = ProviderType.MLC_LLM,
        deliveryType = DeliveryType.LOCAL_ARCHIVE,
        minAppVersion = 1,
        sizeBytes = largeSize,
        capabilities = emptySet(),
        installState = InstallState.NOT_INSTALLED,
        manifestUrl = "https://example.com",
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
      )

    val entity = ModelPackageMapper.toEntity(domain)
    val roundTrip = ModelPackageMapper.toDomain(entity)

    assertThat(roundTrip.sizeBytes).isEqualTo(largeSize)
  }
}
