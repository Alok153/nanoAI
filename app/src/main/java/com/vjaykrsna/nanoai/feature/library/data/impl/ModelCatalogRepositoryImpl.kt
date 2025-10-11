package com.vjaykrsna.nanoai.feature.library.data.impl

import android.content.Context
import com.vjaykrsna.nanoai.core.data.db.daos.ChatThreadDao
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.toDomain
import com.vjaykrsna.nanoai.core.domain.model.toEntity
import com.vjaykrsna.nanoai.feature.library.data.ModelCatalogRepository
import com.vjaykrsna.nanoai.feature.library.data.daos.ModelPackageReadDao
import com.vjaykrsna.nanoai.feature.library.data.daos.ModelPackageWriteDao
import com.vjaykrsna.nanoai.feature.library.model.InstallState
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

/** Wraps ModelPackageDao, converting between entities and domain models. */
@Singleton
class ModelCatalogRepositoryImpl
@Inject
constructor(
  private val modelPackageReadDao: ModelPackageReadDao,
  private val modelPackageWriteDao: ModelPackageWriteDao,
  private val chatThreadDao: ChatThreadDao,
  @ApplicationContext private val context: Context,
) : ModelCatalogRepository {
  private val clock = Clock.System

  override suspend fun getAllModels(): List<ModelPackage> =
    modelPackageReadDao.getAll().map { it.toDomain() }

  override suspend fun getModel(modelId: String): ModelPackage? =
    modelPackageReadDao.getById(modelId)?.toDomain()

  override suspend fun getModelById(modelId: String): Flow<ModelPackage?> =
    modelPackageReadDao.observeById(modelId).map { it?.toDomain() }

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
    val existingById = modelPackageReadDao.getAll().associateBy { it.modelId }
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
    modelPackageWriteDao.replaceCatalog(mergedEntities)
  }

  override suspend fun updateDownloadTaskId(modelId: String, taskId: UUID?) {
    modelPackageWriteDao.updateDownloadTaskId(modelId, taskId?.toString(), clock.now())
  }

  override suspend fun updateChecksum(modelId: String, checksum: String) {
    modelPackageWriteDao.updateIntegrityMetadata(modelId, checksum, null, clock.now())
  }

  override fun observeAllModels(): Flow<List<ModelPackage>> =
    modelPackageReadDao.observeAll().map { models -> models.map { it.toDomain() } }

  override fun observeInstalledModels(): Flow<List<ModelPackage>> =
    modelPackageReadDao.observeInstalled().map { models -> models.map { it.toDomain() } }

  override suspend fun isModelActiveInSession(modelId: String): Boolean =
    chatThreadDao.countActiveByModel(modelId) > 0

  override suspend fun deleteModelFiles(modelId: String) {
    val modelsDir = File(context.filesDir, "models")
    val cleanupTargets =
      listOf(
        File(modelsDir, "$modelId.bin"),
        File(modelsDir, "$modelId.tmp"),
        File(modelsDir, "$modelId.metadata"),
        File(modelsDir, modelId),
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
}
