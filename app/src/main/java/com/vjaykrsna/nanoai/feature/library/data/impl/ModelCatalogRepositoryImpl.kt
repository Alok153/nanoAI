package com.vjaykrsna.nanoai.feature.library.data.impl

import android.content.Context
import com.vjaykrsna.nanoai.core.data.db.daos.ChatThreadDao
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.toDomain
import com.vjaykrsna.nanoai.core.domain.model.toEntity
import com.vjaykrsna.nanoai.feature.library.data.ModelCatalogRepository
import com.vjaykrsna.nanoai.feature.library.data.daos.ModelPackageDao
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
  private val modelPackageDao: ModelPackageDao,
  private val chatThreadDao: ChatThreadDao,
  @ApplicationContext private val context: Context,
) : ModelCatalogRepository {
  private val clock = Clock.System

  override suspend fun getAllModels(): List<ModelPackage> =
    modelPackageDao.getAll().map { it.toDomain() }

  override suspend fun getModel(modelId: String): ModelPackage? =
    modelPackageDao.getById(modelId)?.toDomain()

  override suspend fun getModelById(modelId: String): Flow<ModelPackage?> =
    modelPackageDao.observeById(modelId).map { it?.toDomain() }

  override suspend fun getInstalledModels(): List<ModelPackage> =
    modelPackageDao.getByInstallState(InstallState.INSTALLED).map { it.toDomain() }

  override suspend fun getModelsByState(state: InstallState): List<ModelPackage> =
    modelPackageDao.getByInstallState(state).map { it.toDomain() }

  override suspend fun updateModelState(modelId: String, state: InstallState) {
    modelPackageDao.updateInstallState(modelId, state, clock.now())
  }

  override suspend fun upsertModel(model: ModelPackage) {
    modelPackageDao.insert(model.toEntity())
  }

  override suspend fun updateDownloadTaskId(modelId: String, taskId: UUID?) {
    modelPackageDao.updateDownloadTaskId(modelId, taskId?.toString(), clock.now())
  }

  override suspend fun updateChecksum(modelId: String, checksum: String) {
    modelPackageDao.updateIntegrityMetadata(modelId, checksum, null, clock.now())
  }

  override fun observeAllModels(): Flow<List<ModelPackage>> =
    modelPackageDao.observeAll().map { models -> models.map { it.toDomain() } }

  override fun observeInstalledModels(): Flow<List<ModelPackage>> =
    modelPackageDao.observeInstalled().map { models -> models.map { it.toDomain() } }

  override suspend fun isModelActiveInSession(modelId: String): Boolean =
    chatThreadDao.countActiveByModel(modelId) > 0

  override suspend fun deleteModelFiles(modelId: String) {
    val modelsDir = File(context.filesDir, "models")
    listOf(
        File(modelsDir, "$modelId.bin"),
        File(modelsDir, "$modelId.tmp"),
        File(modelsDir, "$modelId.metadata"),
      )
      .forEach { file ->
        if (file.exists()) {
          file.delete()
        }
      }
  }
}
