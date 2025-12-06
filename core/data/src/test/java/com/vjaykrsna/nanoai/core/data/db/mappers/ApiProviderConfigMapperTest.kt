package com.vjaykrsna.nanoai.core.data.db.mappers

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.db.entities.ApiProviderConfigEntity
import com.vjaykrsna.nanoai.core.domain.model.ApiProviderConfig
import com.vjaykrsna.nanoai.core.model.APIType
import com.vjaykrsna.nanoai.core.model.ProviderStatus
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test

class ApiProviderConfigMapperTest {

  @Test
  fun `toDomain maps all fields correctly`() {
    val quotaResetAt = Instant.parse("2024-01-15T12:00:00Z")

    val entity =
      ApiProviderConfigEntity(
        providerId = "openai-main",
        providerName = "OpenAI",
        baseUrl = "https://api.openai.com/v1",
        credentialId = "cred-123",
        apiType = APIType.OPENAI_COMPATIBLE,
        isEnabled = true,
        quotaResetAt = quotaResetAt,
        lastStatus = ProviderStatus.OK,
      )

    val domain = ApiProviderConfigMapper.toDomain(entity)

    assertThat(domain.providerId).isEqualTo("openai-main")
    assertThat(domain.providerName).isEqualTo("OpenAI")
    assertThat(domain.baseUrl).isEqualTo("https://api.openai.com/v1")
    assertThat(domain.credentialId).isEqualTo("cred-123")
    assertThat(domain.apiType).isEqualTo(APIType.OPENAI_COMPATIBLE)
    assertThat(domain.isEnabled).isTrue()
    assertThat(domain.quotaResetAt).isEqualTo(quotaResetAt)
    assertThat(domain.lastStatus).isEqualTo(ProviderStatus.OK)
  }

  @Test
  fun `toEntity maps all fields correctly`() {
    val quotaResetAt = Instant.parse("2024-01-15T12:00:00Z")

    val domain =
      ApiProviderConfig(
        providerId = "gemini-primary",
        providerName = "Google Gemini",
        baseUrl = "https://generativelanguage.googleapis.com/v1",
        credentialId = "cred-456",
        apiType = APIType.GEMINI,
        isEnabled = false,
        quotaResetAt = quotaResetAt,
        lastStatus = ProviderStatus.RATE_LIMITED,
      )

    val entity = ApiProviderConfigMapper.toEntity(domain)

    assertThat(entity.providerId).isEqualTo("gemini-primary")
    assertThat(entity.providerName).isEqualTo("Google Gemini")
    assertThat(entity.baseUrl).isEqualTo("https://generativelanguage.googleapis.com/v1")
    assertThat(entity.credentialId).isEqualTo("cred-456")
    assertThat(entity.apiType).isEqualTo(APIType.GEMINI)
    assertThat(entity.isEnabled).isFalse()
    assertThat(entity.quotaResetAt).isEqualTo(quotaResetAt)
    assertThat(entity.lastStatus).isEqualTo(ProviderStatus.RATE_LIMITED)
  }

  @Test
  fun `round trip conversion preserves all data`() {
    val original =
      ApiProviderConfig(
        providerId = "custom-local",
        providerName = "Local Server",
        baseUrl = "http://localhost:8080",
        credentialId = null,
        apiType = APIType.CUSTOM,
        isEnabled = true,
        quotaResetAt = null,
        lastStatus = ProviderStatus.UNKNOWN,
      )

    val entity = ApiProviderConfigMapper.toEntity(original)
    val roundTrip = ApiProviderConfigMapper.toDomain(entity)

    assertThat(roundTrip).isEqualTo(original)
  }

  @Test
  fun `handles null credentialId correctly`() {
    val entity =
      ApiProviderConfigEntity(
        providerId = "test-provider",
        providerName = "Test",
        baseUrl = "https://example.com",
        credentialId = null,
        apiType = APIType.OPENAI_COMPATIBLE,
      )

    val domain = ApiProviderConfigMapper.toDomain(entity)

    assertThat(domain.credentialId).isNull()
    assertThat(domain.hasCredential).isFalse()
  }

  @Test
  fun `handles null quotaResetAt correctly`() {
    val domain =
      ApiProviderConfig(
        providerId = "no-quota",
        providerName = "No Quota Provider",
        baseUrl = "https://api.example.com",
        apiType = APIType.GEMINI,
        quotaResetAt = null,
      )

    val entity = ApiProviderConfigMapper.toEntity(domain)

    assertThat(entity.quotaResetAt).isNull()
  }

  @Test
  fun `all APIType values can be mapped`() {
    APIType.values().forEach { apiType ->
      val entity =
        ApiProviderConfigEntity(
          providerId = "test-$apiType",
          providerName = "Test",
          baseUrl = "https://example.com",
          apiType = apiType,
        )

      val domain = ApiProviderConfigMapper.toDomain(entity)
      val roundTrip = ApiProviderConfigMapper.toEntity(domain)

      assertThat(roundTrip.apiType).isEqualTo(apiType)
    }
  }

  @Test
  fun `all ProviderStatus values can be mapped`() {
    ProviderStatus.values().forEach { status ->
      val entity =
        ApiProviderConfigEntity(
          providerId = "test-$status",
          providerName = "Test",
          baseUrl = "https://example.com",
          apiType = APIType.OPENAI_COMPATIBLE,
          lastStatus = status,
        )

      val domain = ApiProviderConfigMapper.toDomain(entity)
      val roundTrip = ApiProviderConfigMapper.toEntity(domain)

      assertThat(roundTrip.lastStatus).isEqualTo(status)
    }
  }

  @Test
  fun `extension function toDomain works correctly`() {
    val entity =
      ApiProviderConfigEntity(
        providerId = "ext-test",
        providerName = "Extension Test",
        baseUrl = "https://api.test.com",
        apiType = APIType.CUSTOM,
      )

    val domain = entity.toDomain()

    assertThat(domain.providerName).isEqualTo("Extension Test")
  }

  @Test
  fun `extension function toEntity works correctly`() {
    val domain =
      ApiProviderConfig(
        providerId = "ext-entity-test",
        providerName = "Extension Entity Test",
        baseUrl = "https://api.entity.com",
        apiType = APIType.GEMINI,
      )

    val entity = domain.toEntity()

    assertThat(entity.providerName).isEqualTo("Extension Entity Test")
  }

  @Test
  fun `hasCredential returns true when credentialId is present`() {
    val domain =
      ApiProviderConfig(
        providerId = "has-cred",
        providerName = "With Credential",
        baseUrl = "https://api.example.com",
        apiType = APIType.OPENAI_COMPATIBLE,
        credentialId = "my-credential",
      )

    assertThat(domain.hasCredential).isTrue()
  }

  @Test
  fun `hasCredential returns false when credentialId is blank`() {
    val domain =
      ApiProviderConfig(
        providerId = "blank-cred",
        providerName = "Blank Credential",
        baseUrl = "https://api.example.com",
        apiType = APIType.OPENAI_COMPATIBLE,
        credentialId = "   ",
      )

    assertThat(domain.hasCredential).isFalse()
  }

  @Test
  fun `default values are preserved`() {
    val entity =
      ApiProviderConfigEntity(
        providerId = "defaults-test",
        providerName = "Defaults",
        baseUrl = "https://example.com",
        apiType = APIType.OPENAI_COMPATIBLE,
      )

    val domain = ApiProviderConfigMapper.toDomain(entity)

    assertThat(domain.isEnabled).isTrue()
    assertThat(domain.lastStatus).isEqualTo(ProviderStatus.UNKNOWN)
  }
}
