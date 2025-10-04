package com.vjaykrsna.nanoai.core.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vjaykrsna.nanoai.core.model.APIType
import com.vjaykrsna.nanoai.core.model.ProviderStatus
import kotlinx.datetime.Instant

/**
 * Room entity representing a cloud API provider configuration.
 *
 * Stores connection details, API keys (encrypted), and health status for cloud fallback when local
 * models are unavailable.
 *
 * @property providerId Unique provider identifier (e.g., "openai", "gemini-main")
 * @property providerName Display name for the provider
 * @property baseUrl Base API endpoint URL
 * @property apiKey API key (should be encrypted at rest; encryption integration tracked separately)
 * @property apiType API protocol type
 * @property isEnabled Whether this provider is currently enabled for use
 * @property quotaResetAt Timestamp when rate limit quota resets (nullable)
 * @property lastStatus Last known health/connectivity status
 */
@Entity(tableName = "api_provider_configs")
data class ApiProviderConfigEntity(
  @PrimaryKey @ColumnInfo(name = "provider_id") val providerId: String,
  @ColumnInfo(name = "provider_name") val providerName: String,
  @ColumnInfo(name = "base_url") val baseUrl: String,
  // Encryption integration: use encrypted storage (e.g., Jetpack Security) before production
  // rollout
  @ColumnInfo(name = "api_key") val apiKey: String,
  @ColumnInfo(name = "api_type") val apiType: APIType,
  @ColumnInfo(name = "is_enabled") val isEnabled: Boolean = true,
  @ColumnInfo(name = "quota_reset_at") val quotaResetAt: Instant? = null,
  @ColumnInfo(name = "last_status") val lastStatus: ProviderStatus = ProviderStatus.UNKNOWN,
)
