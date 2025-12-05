package com.vjaykrsna.nanoai.core.data.library.catalog

import com.vjaykrsna.nanoai.core.data.db.entities.DownloadManifestEntity
import com.vjaykrsna.nanoai.core.data.db.entities.ModelPackageEntity
import com.vjaykrsna.nanoai.core.data.db.entities.ModelPackageWithManifests
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/** Local persistence layer for model catalog metadata and manifests. */
@Singleton
class ModelCatalogLocalDataSource
@Inject
constructor(
  private val modelPackageWriteDao: ModelPackageWriteDao,
  private val relationsDao: ModelPackageRelationsDao,
  private val downloadManifestDao: DownloadManifestDao,
  private val clock: Clock,
) {

  fun observeModel(modelId: String): Flow<ModelPackageWithManifests?> =
    relationsDao.observeModelWithManifests(modelId)

  suspend fun getModel(modelId: String): ModelPackageWithManifests? =
    relationsDao.getModelWithManifests(modelId)

  suspend fun upsertPackages(packages: List<ModelPackageEntity>) {
    modelPackageWriteDao.insertAll(packages)
  }

  suspend fun upsertPackage(model: ModelPackageEntity) {
    modelPackageWriteDao.insert(model)
  }

  suspend fun cacheManifest(manifest: DownloadManifestEntity) {
    downloadManifestDao.upsertManifest(manifest.copy(fetchedAt = clock.now()))
  }

  suspend fun cacheManifests(manifests: List<DownloadManifestEntity>) {
    val now = clock.now()
    downloadManifestDao.upsertManifests(manifests.map { it.copy(fetchedAt = now) })
  }

  suspend fun pruneExpired(now: Instant = clock.now()) {
    downloadManifestDao.deleteExpiredManifests(now)
  }

  suspend fun updateIntegrityMetadata(modelId: String, checksum: String, signature: String?) {
    modelPackageWriteDao.updateIntegrityMetadata(modelId, checksum, signature, clock.now())
  }
}
