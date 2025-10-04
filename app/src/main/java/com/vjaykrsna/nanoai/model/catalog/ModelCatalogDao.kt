package com.vjaykrsna.nanoai.model.catalog

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.vjaykrsna.nanoai.feature.library.model.InstallState
import com.vjaykrsna.nanoai.feature.library.model.ProviderType
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/** DAO managing model packages and cached download manifests. */
@Dao
interface ModelCatalogDao {
  // region Model packages
  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(model: ModelPackageEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(models: List<ModelPackageEntity>)

  @Update suspend fun update(model: ModelPackageEntity)

  @Delete suspend fun delete(model: ModelPackageEntity)

  @Query("SELECT * FROM model_packages WHERE model_id = :modelId")
  suspend fun getById(modelId: String): ModelPackageEntity?

  @Query("SELECT * FROM model_packages WHERE model_id = :modelId")
  fun observeById(modelId: String): Flow<ModelPackageEntity?>

  @Query("SELECT * FROM model_packages ORDER BY display_name ASC")
  suspend fun getAll(): List<ModelPackageEntity>

  @Query("SELECT * FROM model_packages ORDER BY display_name ASC")
  fun observeAll(): Flow<List<ModelPackageEntity>>

  @Query("SELECT * FROM model_packages WHERE install_state = :state ORDER BY display_name ASC")
  suspend fun getByInstallState(state: InstallState): List<ModelPackageEntity>

  @Query("SELECT * FROM model_packages WHERE install_state = 'INSTALLED' ORDER BY display_name ASC")
  fun observeInstalled(): Flow<List<ModelPackageEntity>>

  @Query(
    "SELECT * FROM model_packages WHERE provider_type = :providerType ORDER BY display_name ASC",
  )
  suspend fun getByProviderType(providerType: ProviderType): List<ModelPackageEntity>

  @Query("SELECT * FROM model_packages WHERE capabilities LIKE '%' || :capability || '%'")
  suspend fun getByCapability(capability: String): List<ModelPackageEntity>

  @Query(
    "SELECT * FROM model_packages WHERE install_state = 'DOWNLOADING' ORDER BY display_name ASC",
  )
  suspend fun getDownloading(): List<ModelPackageEntity>

  @Query(
    "UPDATE model_packages SET install_state = :state, updated_at = :updatedAt WHERE model_id = :modelId",
  )
  suspend fun updateInstallState(modelId: String, state: InstallState, updatedAt: Instant)

  @Query(
    "UPDATE model_packages SET download_task_id = :taskId, updated_at = :updatedAt WHERE model_id = :modelId",
  )
  suspend fun updateDownloadTaskId(modelId: String, taskId: String?, updatedAt: Instant)

  @Query(
    "UPDATE model_packages SET checksum_sha256 = :checksum, signature = :signature, updated_at = :updatedAt WHERE model_id = :modelId",
  )
  suspend fun updateIntegrityMetadata(
    modelId: String,
    checksum: String,
    signature: String?,
    updatedAt: Instant,
  )

  @Query("DELETE FROM model_packages") suspend fun deleteAll()

  @Query("SELECT SUM(size_bytes) FROM model_packages WHERE install_state = 'INSTALLED'")
  suspend fun getTotalInstalledSize(): Long?

  @Query("SELECT COUNT(*) FROM model_packages WHERE install_state = 'INSTALLED'")
  suspend fun countInstalled(): Int
  // endregion

  // region Manifests
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertManifest(manifest: DownloadManifestEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertManifests(manifests: List<DownloadManifestEntity>)

  @Query(
    "SELECT * FROM download_manifests WHERE model_id = :modelId AND version = :version LIMIT 1",
  )
  suspend fun getManifest(modelId: String, version: String): DownloadManifestEntity?

  @Query(
    "SELECT * FROM download_manifests WHERE model_id = :modelId ORDER BY fetched_at DESC LIMIT 1",
  )
  suspend fun getLatestManifest(modelId: String): DownloadManifestEntity?

  @Query("DELETE FROM download_manifests WHERE model_id = :modelId AND version = :version")
  suspend fun deleteManifest(modelId: String, version: String)

  @Query("DELETE FROM download_manifests WHERE expires_at IS NOT NULL AND expires_at < :now")
  suspend fun deleteExpiredManifests(now: Instant)

  @Query("DELETE FROM download_manifests") suspend fun clearManifests()

  @Transaction
  @Query("SELECT * FROM model_packages WHERE model_id = :modelId LIMIT 1")
  suspend fun getModelWithManifests(modelId: String): ModelPackageWithManifests?

  @Transaction
  @Query("SELECT * FROM model_packages WHERE model_id = :modelId LIMIT 1")
  fun observeModelWithManifests(modelId: String): Flow<ModelPackageWithManifests?>
  // endregion
}
