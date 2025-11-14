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
interface ApiProviderConfigDao :
  ApiProviderConfigWriteDao,
  ApiProviderConfigReadDao,
  ApiProviderConfigStatusDao,
  ApiProviderConfigMaintenanceDao

/** Write helpers for API provider configurations. */
interface ApiProviderConfigWriteDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(config: ApiProviderConfigEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(configs: List<ApiProviderConfigEntity>)

  @Update suspend fun update(config: ApiProviderConfigEntity)

  @Delete suspend fun delete(config: ApiProviderConfigEntity)
}

/** Read helpers for API provider configurations. */
interface ApiProviderConfigReadDao {
  @Query("SELECT * FROM api_provider_configs WHERE provider_id = :providerId")
  suspend fun getById(providerId: String): ApiProviderConfigEntity?

  @Query("SELECT * FROM api_provider_configs WHERE provider_id = :providerId")
  fun observeById(providerId: String): Flow<ApiProviderConfigEntity?>

  @Query("SELECT * FROM api_provider_configs ORDER BY provider_name ASC")
  suspend fun getAll(): List<ApiProviderConfigEntity>

  @Query("SELECT * FROM api_provider_configs ORDER BY provider_name ASC")
  fun observeAll(): Flow<List<ApiProviderConfigEntity>>

  @Query("SELECT * FROM api_provider_configs WHERE is_enabled = 1 ORDER BY provider_name ASC")
  suspend fun getEnabled(): List<ApiProviderConfigEntity>

  @Query("SELECT * FROM api_provider_configs WHERE is_enabled = 1 ORDER BY provider_name ASC")
  fun observeEnabled(): Flow<List<ApiProviderConfigEntity>>

  @Query("SELECT * FROM api_provider_configs WHERE last_status = :status")
  suspend fun getByStatus(status: ProviderStatus): List<ApiProviderConfigEntity>
}

/** Status update helpers for API provider configurations. */
interface ApiProviderConfigStatusDao {
  @Query("UPDATE api_provider_configs SET is_enabled = :enabled WHERE provider_id = :providerId")
  suspend fun setEnabled(providerId: String, enabled: Boolean)

  @Query("UPDATE api_provider_configs SET last_status = :status WHERE provider_id = :providerId")
  suspend fun updateStatus(providerId: String, status: ProviderStatus)

  @Query(
    "UPDATE api_provider_configs SET quota_reset_at = :resetAt WHERE provider_id = :providerId"
  )
  suspend fun updateQuotaReset(providerId: String, resetAt: Instant?)
}

/** Maintenance helpers for provider configuration state. */
interface ApiProviderConfigMaintenanceDao {
  @Query("DELETE FROM api_provider_configs") suspend fun deleteAll()

  @Query("SELECT COUNT(*) FROM api_provider_configs WHERE is_enabled = 1")
  suspend fun countEnabled(): Int

  @Query("SELECT COUNT(*) FROM api_provider_configs WHERE is_enabled = 1 AND last_status = 'OK'")
  suspend fun countHealthy(): Int
}
