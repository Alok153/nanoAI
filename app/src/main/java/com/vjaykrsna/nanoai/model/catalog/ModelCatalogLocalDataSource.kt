package com.vjaykrsna.nanoai.model.catalog

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
  private val dao: ModelCatalogDao,
) {
  private val clock: Clock = Clock.System

  fun observeModel(modelId: String): Flow<ModelPackageWithManifests?> =
    dao.observeModelWithManifests(modelId)

  suspend fun getModel(modelId: String): ModelPackageWithManifests? =
    dao.getModelWithManifests(modelId)

  suspend fun upsertPackages(packages: List<ModelPackageEntity>) {
    dao.insertAll(packages)
  }

  suspend fun upsertPackage(model: ModelPackageEntity) {
    dao.insert(model)
  }

  suspend fun cacheManifest(manifest: DownloadManifestEntity) {
    dao.upsertManifest(manifest.copy(fetchedAt = clock.now()))
  }

  suspend fun cacheManifests(manifests: List<DownloadManifestEntity>) {
    val now = clock.now()
    dao.upsertManifests(manifests.map { it.copy(fetchedAt = now) })
  }

  suspend fun pruneExpired(now: Instant = clock.now()) {
    dao.deleteExpiredManifests(now)
  }

  suspend fun updateIntegrityMetadata(modelId: String, checksum: String, signature: String?) {
    dao.updateIntegrityMetadata(modelId, checksum, signature, clock.now())
  }
}
