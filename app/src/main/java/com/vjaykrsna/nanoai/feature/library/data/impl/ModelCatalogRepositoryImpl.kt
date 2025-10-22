package com.vjaykrsna.nanoai.feature.library.data.impl

import android.content.Context
import com.vjaykrsna.nanoai.core.data.db.daos.ChatThreadDao
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.toDomain
import com.vjaykrsna.nanoai.core.domain.model.toEntity
import com.vjaykrsna.nanoai.feature.library.data.ModelCatalogRefreshStatus
import com.vjaykrsna.nanoai.feature.library.data.ModelCatalogRepository
import com.vjaykrsna.nanoai.feature.library.data.daos.ModelPackageReadDao
import com.vjaykrsna.nanoai.feature.library.data.daos.ModelPackageWriteDao
import com.vjaykrsna.nanoai.feature.library.model.InstallState
import com.vjaykrsna.nanoai.model.catalog.ModelPackageEntity
import com.vjaykrsna.nanoai.model.leap.LeapModelRemoteDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock

/** Wraps ModelPackageDao, converting between entities and domain models. */
@Singleton
class ModelCatalogRepositoryImpl
@Inject
constructor(
  private val modelPackageReadDao: ModelPackageReadDao,
  private val modelPackageWriteDao: ModelPackageWriteDao,
  private val chatThreadDao: ChatThreadDao,
  private val leapModelRemoteDataSource: LeapModelRemoteDataSource,
  @ApplicationContext private val context: Context,
  private val clock: Clock,
) : ModelCatalogRepository {
  private val modelsDirectoryProvider: () -> File = { File(context.filesDir, "models") }
  private val refreshStatus = MutableStateFlow(ModelCatalogRefreshStatus())
  private val allModelsFlow: Flow<List<ModelPackage>> =
    modelPackageReadDao
      .observeAll()
      .map { models -> models.map { it.toDomain() } }
      .distinctUntilChanged()

  private val installedModelsFlow: Flow<List<ModelPackage>> =
    modelPackageReadDao
      .observeInstalled()
      .map { models -> models.map { it.toDomain() } }
      .distinctUntilChanged()

  override fun observeAllModels(): Flow<List<ModelPackage>> =
    allModelsFlow.map { it + leapModelRemoteDataSource.getModels() }

  override fun observeInstalledModels(): Flow<List<ModelPackage>> = installedModelsFlow

  override suspend fun getAllModels(): List<ModelPackage> =
    modelPackageReadDao.getAll().map { it.toDomain() } + leapModelRemoteDataSource.getModels()

  override suspend fun getModel(modelId: String): ModelPackage? =
    modelPackageReadDao.getById(modelId)?.toDomain()

  override fun getModelById(modelId: String): Flow<ModelPackage?> =
    modelPackageReadDao.observeById(modelId).map { it?.toDomain() }.distinctUntilChanged()

  override suspend fun getInstalledModels(): List<ModelPackage> =
    modelPackageReadDao.getByInstallState(InstallState.INSTALLED).map { it.toDomain() }

  override suspend fun getModelsByState(state: InstallState): List<ModelPackage> =
    modelPackageReadDao.getByInstallState(state).map { it.toDomain() }

  override suspend fun updateModelState(modelId: String, state: InstallState) {
    modelPackageWriteDao.updateInstallState(modelId, state, clock.now())
  }

  override suspend fun upsertModel(model: ModelPackage) {
    modelPackageWriteDao.insert(model.toEntity())
  }

  override suspend fun replaceCatalog(models: List<ModelPackage>) {
    val existing = modelPackageReadDao.getAll()
    val existingById = existing.associateBy { it.modelId }
    val mergedEntities =
      models.map { model ->
        val incoming = model.toEntity()
        val persisted = existingById[incoming.modelId]
        if (persisted != null) {
          incoming.copy(
            installState = persisted.installState,
            downloadTaskId = persisted.downloadTaskId,
            checksumSha256 =
              incoming.checksumSha256?.takeUnless { it.isBlank() } ?: persisted.checksumSha256,
            signature = incoming.signature?.takeUnless { it.isBlank() } ?: persisted.signature,
            createdAt = persisted.createdAt,
            updatedAt = maxOf(incoming.updatedAt, persisted.updatedAt),
          )
        } else {
          incoming
        }
      }
    val incomingIds = mergedEntities.mapTo(mutableSetOf(), ModelPackageEntity::modelId)
    val preserved = existing.filter { it.modelId !in incomingIds }
    modelPackageWriteDao.replaceCatalog(mergedEntities + preserved)
  }

  override suspend fun updateDownloadTaskId(modelId: String, taskId: UUID?) {
    modelPackageWriteDao.updateDownloadTaskId(modelId, taskId?.toString(), clock.now())
  }

  override suspend fun updateChecksum(modelId: String, checksum: String) {
    modelPackageWriteDao.updateIntegrityMetadata(modelId, checksum, null, clock.now())
  }

  override suspend fun isModelActiveInSession(modelId: String): Boolean =
    chatThreadDao.countActiveByModel(modelId) > 0

  override suspend fun deleteModelFiles(modelId: String) {
    val modelsDirectory = resolveModelsDirectory()
    val cleanupTargets =
      listOf(
        File(modelsDirectory, "$modelId.bin"),
        File(modelsDirectory, "$modelId.tmp"),
        File(modelsDirectory, "$modelId.metadata"),
        File(modelsDirectory, modelId),
      )
    cleanupTargets.forEach { file ->
      if (!file.exists()) return@forEach
      if (file.isDirectory) {
        file.deleteRecursively()
      } else {
        file.delete()
      }
    }
  }

  override fun observeRefreshStatus(): Flow<ModelCatalogRefreshStatus> = refreshStatus.asStateFlow()

  override suspend fun recordRefreshSuccess(source: String, modelCount: Int) {
    val now = clock.now()
    refreshStatus.update { status ->
      status.copy(
        lastSuccessAt = now,
        lastSuccessSource = source.takeIf { it.isNotBlank() },
        lastSuccessCount = modelCount,
        lastFallbackAt = null,
        lastFallbackReason = null,
        lastFallbackCachedCount = 0,
        lastFallbackMessage = null,
      )
    }
  }

  override suspend fun recordOfflineFallback(reason: String, cachedCount: Int, message: String?) {
    val now = clock.now()
    refreshStatus.update { status ->
      status.copy(
        lastFallbackAt = now,
        lastFallbackReason = reason.takeIf { it.isNotBlank() },
        lastFallbackCachedCount = cachedCount,
        lastFallbackMessage = message?.takeIf { it.isNotBlank() },
      )
    }
  }

  private fun resolveModelsDirectory(): File = modelsDirectoryProvider()
}
