package com.vjaykrsna.nanoai.core.data.db.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vjaykrsna.nanoai.core.data.db.entities.ApiProviderConfigEntity
import com.vjaykrsna.nanoai.core.model.ProviderStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Data Access Object for ApiProviderConfig entities.
 *
 * Provides methods to manage cloud API provider configurations and status.
 */
@Dao
interface ApiProviderConfigDao {
  /** Insert or update an API provider configuration. */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(config: ApiProviderConfigEntity)

  /** Insert multiple provider configs (for seeding defaults). */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(configs: List<ApiProviderConfigEntity>)

  /** Update an existing provider configuration. */
  @Update suspend fun update(config: ApiProviderConfigEntity)

  /** Delete a provider configuration. */
  @Delete suspend fun delete(config: ApiProviderConfigEntity)

  /** Get a specific provider by ID. */
  @Query("SELECT * FROM api_provider_configs WHERE provider_id = :providerId")
  suspend fun getById(providerId: String): ApiProviderConfigEntity?

  /** Observe a specific provider by ID (reactive). */
  @Query("SELECT * FROM api_provider_configs WHERE provider_id = :providerId")
  fun observeById(providerId: String): Flow<ApiProviderConfigEntity?>

  /** Get all providers ordered by name. */
  @Query("SELECT * FROM api_provider_configs ORDER BY provider_name ASC")
  suspend fun getAll(): List<ApiProviderConfigEntity>

  /** Observe all providers (reactive). */
  @Query("SELECT * FROM api_provider_configs ORDER BY provider_name ASC")
  fun observeAll(): Flow<List<ApiProviderConfigEntity>>

  /** Get enabled providers only. */
  @Query("SELECT * FROM api_provider_configs WHERE is_enabled = 1 ORDER BY provider_name ASC")
  suspend fun getEnabled(): List<ApiProviderConfigEntity>

  /** Observe enabled providers (reactive). */
  @Query("SELECT * FROM api_provider_configs WHERE is_enabled = 1 ORDER BY provider_name ASC")
  fun observeEnabled(): Flow<List<ApiProviderConfigEntity>>

  /** Get providers by status (e.g., find all RATE_LIMITED providers). */
  @Query("SELECT * FROM api_provider_configs WHERE last_status = :status")
  suspend fun getByStatus(status: ProviderStatus): List<ApiProviderConfigEntity>

  /** Enable or disable a provider. */
  @Query("UPDATE api_provider_configs SET is_enabled = :enabled WHERE provider_id = :providerId")
  suspend fun setEnabled(providerId: String, enabled: Boolean)

  /** Update provider status. */
  @Query("UPDATE api_provider_configs SET last_status = :status WHERE provider_id = :providerId")
  suspend fun updateStatus(providerId: String, status: ProviderStatus)

  /** Update quota reset time. */
  @Query(
    "UPDATE api_provider_configs SET quota_reset_at = :resetAt WHERE provider_id = :providerId"
  )
  suspend fun updateQuotaReset(providerId: String, resetAt: Instant?)

  /** Update API key (for key rotation). */
  @Query("UPDATE api_provider_configs SET api_key = :apiKey WHERE provider_id = :providerId")
  suspend fun updateApiKey(providerId: String, apiKey: String)

  /** Delete all providers (for testing/debugging). */
  @Query("DELETE FROM api_provider_configs") suspend fun deleteAll()

  /** Count enabled providers. */
  @Query("SELECT COUNT(*) FROM api_provider_configs WHERE is_enabled = 1")
  suspend fun countEnabled(): Int

  /** Check if any providers are enabled and operational. */
  @Query("SELECT COUNT(*) FROM api_provider_configs WHERE is_enabled = 1 AND last_status = 'OK'")
  suspend fun countHealthy(): Int
}
