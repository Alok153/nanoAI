package com.vjaykrsna.nanoai.feature.library.data.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vjaykrsna.nanoai.feature.library.data.entities.ModelPackageEntity
import com.vjaykrsna.nanoai.feature.library.model.InstallState
import com.vjaykrsna.nanoai.feature.library.model.ProviderType
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for ModelPackage entities.
 *
 * Provides methods to manage AI model catalog and installation states.
 */
@Dao
interface ModelPackageDao {
    /**
     * Insert or update a model package.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(model: ModelPackageEntity)

    /**
     * Insert multiple models (for catalog sync).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(models: List<ModelPackageEntity>)

    /**
     * Update an existing model package.
     */
    @Update
    suspend fun update(model: ModelPackageEntity)

    /**
     * Delete a model package.
     */
    @Delete
    suspend fun delete(model: ModelPackageEntity)

    /**
     * Get a specific model by ID.
     */
    @Query("SELECT * FROM model_packages WHERE model_id = :modelId")
    suspend fun getById(modelId: String): ModelPackageEntity?

    /**
     * Observe a specific model by ID (reactive).
     */
    @Query("SELECT * FROM model_packages WHERE model_id = :modelId")
    fun observeById(modelId: String): Flow<ModelPackageEntity?>

    /**
     * Get all models ordered by name.
     */
    @Query("SELECT * FROM model_packages ORDER BY display_name ASC")
    suspend fun getAll(): List<ModelPackageEntity>

    /**
     * Observe all models (reactive).
     */
    @Query("SELECT * FROM model_packages ORDER BY display_name ASC")
    fun observeAll(): Flow<List<ModelPackageEntity>>

    /**
     * Get models by installation state (e.g., all installed models).
     */
    @Query("SELECT * FROM model_packages WHERE install_state = :state ORDER BY display_name ASC")
    suspend fun getByInstallState(state: InstallState): List<ModelPackageEntity>

    /**
     * Observe installed models (reactive).
     */
    @Query("SELECT * FROM model_packages WHERE install_state = 'INSTALLED' ORDER BY display_name ASC")
    fun observeInstalled(): Flow<List<ModelPackageEntity>>

    /**
     * Get models by provider type.
     */
    @Query("SELECT * FROM model_packages WHERE provider_type = :providerType ORDER BY display_name ASC")
    suspend fun getByProviderType(providerType: ProviderType): List<ModelPackageEntity>

    /**
     * Get models with specific capability.
     * Uses LIKE for set membership check (capabilities stored as comma-separated).
     */
    @Query("SELECT * FROM model_packages WHERE capabilities LIKE '%' || :capability || '%'")
    suspend fun getByCapability(capability: String): List<ModelPackageEntity>

    /**
     * Get models currently downloading.
     */
    @Query("SELECT * FROM model_packages WHERE install_state = 'DOWNLOADING' ORDER BY display_name ASC")
    suspend fun getDownloading(): List<ModelPackageEntity>

    /**
     * Update install state for a model.
     */
    @Query("UPDATE model_packages SET install_state = :state WHERE model_id = :modelId")
    suspend fun updateInstallState(
        modelId: String,
        state: InstallState,
    )

    /**
     * Update download task ID for a model.
     */
    @Query("UPDATE model_packages SET download_task_id = :taskId WHERE model_id = :modelId")
    suspend fun updateDownloadTaskId(
        modelId: String,
        taskId: String?,
    )

    /**
     * Update checksum after successful download.
     */
    @Query("UPDATE model_packages SET checksum = :checksum WHERE model_id = :modelId")
    suspend fun updateChecksum(
        modelId: String,
        checksum: String,
    )

    /**
     * Get total size of all installed models.
     */
    @Query("SELECT SUM(size_bytes) FROM model_packages WHERE install_state = 'INSTALLED'")
    suspend fun getTotalInstalledSize(): Long?

    /**
     * Delete all models (for testing/debugging).
     */
    @Query("DELETE FROM model_packages")
    suspend fun deleteAll()

    /**
     * Count installed models.
     */
    @Query("SELECT COUNT(*) FROM model_packages WHERE install_state = 'INSTALLED'")
    suspend fun countInstalled(): Int
}
