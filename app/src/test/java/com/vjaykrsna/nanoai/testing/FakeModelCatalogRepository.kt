package com.vjaykrsna.nanoai.testing

import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.data.ModelCatalogRefreshStatus
import com.vjaykrsna.nanoai.feature.library.data.ModelCatalogRepository
import com.vjaykrsna.nanoai.feature.library.domain.InstallState
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Fake implementation of [ModelCatalogRepository] for testing. */
@Singleton
class FakeModelCatalogRepository @Inject constructor() : ModelCatalogRepository {
  private val _models =
    MutableStateFlow<List<com.vjaykrsna.nanoai.core.domain.model.ModelPackage>>(emptyList())
  private val _refreshStatus = MutableStateFlow(ModelCatalogRefreshStatus())

  var shouldFailOnReplaceCatalog = false
  var lastRefreshSource: String? = null
  var lastOfflineFallbackReason: String? = null

  fun setModels(models: List<ModelPackage>) {
    _models.value = models
  }

  fun addModel(model: ModelPackage) {
    _models.value += model
  }

  fun clearAll() {
    _models.value = emptyList()
    shouldFailOnReplaceCatalog = false
    lastRefreshSource = null
    lastOfflineFallbackReason = null
  }

  override suspend fun getAllModels(): List<ModelPackage> = _models.value

  override suspend fun getModel(modelId: String): ModelPackage? =
    _models.value.firstOrNull { it.modelId == modelId }

  override fun getModelById(modelId: String): Flow<ModelPackage?> =
    _models.map { models -> models.firstOrNull { it.modelId == modelId } }

  override suspend fun getInstalledModels(): List<ModelPackage> =
    _models.value.filter { it.installState == InstallState.INSTALLED }

  override suspend fun getModelsByState(state: InstallState): List<ModelPackage> =
    _models.value.filter { it.installState == state }

  override suspend fun updateModelState(modelId: String, state: InstallState) {
    _models.value =
      _models.value.map { if (it.modelId == modelId) it.copy(installState = state) else it }
  }

  override suspend fun updateDownloadTaskId(modelId: String, taskId: UUID?) {
    _models.value =
      _models.value.map { if (it.modelId == modelId) it.copy(downloadTaskId = taskId) else it }
  }

  override suspend fun updateChecksum(modelId: String, checksum: String) {
    _models.value =
      _models.value.map { if (it.modelId == modelId) it.copy(checksumSha256 = checksum) else it }
  }

  override suspend fun upsertModel(model: ModelPackage) {
    val existing = _models.value.firstOrNull { it.modelId == model.modelId }
    _models.value =
      if (existing != null) {
        _models.value.map { if (it.modelId == model.modelId) model else it }
      } else {
        _models.value + model
      }
  }

  override suspend fun replaceCatalog(models: List<ModelPackage>) {
    if (shouldFailOnReplaceCatalog) {
      error("Failed to replace catalog")
    }
    _models.value = models
  }

  override fun observeAllModels(): Flow<List<ModelPackage>> = _models

  override fun observeInstalledModels(): Flow<List<ModelPackage>> =
    _models.map { models -> models.filter { it.installState == InstallState.INSTALLED } }

  override suspend fun isModelActiveInSession(modelId: String): Boolean = false

  override suspend fun deleteModelFiles(modelId: String) {
    // No-op for fake
  }

  override fun observeRefreshStatus(): Flow<ModelCatalogRefreshStatus> = _refreshStatus

  override suspend fun recordRefreshSuccess(source: String, modelCount: Int) {
    lastRefreshSource = source
    _refreshStatus.value =
      _refreshStatus.value.copy(
        lastSuccessAt = kotlinx.datetime.Clock.System.now(),
        lastSuccessSource = source,
        lastSuccessCount = modelCount,
        lastFallbackAt = null,
        lastFallbackReason = null,
        lastFallbackCachedCount = 0,
        lastFallbackMessage = null,
      )
  }

  override suspend fun recordOfflineFallback(reason: String, cachedCount: Int, message: String?) {
    lastOfflineFallbackReason = reason
    _refreshStatus.value =
      _refreshStatus.value.copy(
        lastFallbackAt = kotlinx.datetime.Clock.System.now(),
        lastFallbackReason = reason,
        lastFallbackCachedCount = cachedCount,
        lastFallbackMessage = message,
      )
  }
}
