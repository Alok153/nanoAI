package com.vjaykrsna.nanoai.feature.library.data

import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.model.InstallState
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/**
 * Repository for model catalog management.
 *
 * Provides access to available AI models, their installation status, and metadata.
 */
@Suppress("TooManyFunctions") // Repository pattern requires comprehensive API
interface ModelCatalogRepository {
  /** Get all available models in the catalog. */
  suspend fun getAllModels(): List<ModelPackage>

  /** Get a specific model by ID. */
  suspend fun getModel(modelId: String): ModelPackage?

  /** Observe a specific model by ID. */
  suspend fun getModelById(modelId: String): kotlinx.coroutines.flow.Flow<ModelPackage?>

  /** Get all installed models. */
  suspend fun getInstalledModels(): List<ModelPackage>

  /** Get models by installation state. */
  suspend fun getModelsByState(state: InstallState): List<ModelPackage>

  /** Update model installation state. */
  suspend fun updateModelState(modelId: String, state: InstallState)

  /** Update model installation state (alias for updateModelState). */
  suspend fun updateInstallState(modelId: String, state: InstallState) =
    updateModelState(modelId, state)

  /** Associate a download task with a model. */
  suspend fun updateDownloadTaskId(modelId: String, taskId: UUID?)

  /** Persist checksum for a model package. */
  suspend fun updateChecksum(modelId: String, checksum: String)

  /** Insert or update a model in the catalog. */
  suspend fun upsertModel(model: ModelPackage)

  /** Observe all models (reactive updates). */
  fun observeAllModels(): Flow<List<ModelPackage>>

  /** Observe installed models (reactive updates). */
  fun observeInstalledModels(): Flow<List<ModelPackage>>

  /** Check whether a model is currently active in any non-archived chat session. */
  suspend fun isModelActiveInSession(modelId: String): Boolean

  /** Delete downloaded artifacts for the provided model. */
  suspend fun deleteModelFiles(modelId: String)
}
