package com.vjaykrsna.nanoai.core.domain.library

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.annotations.OneShot
import com.vjaykrsna.nanoai.core.common.annotations.ReactiveStream
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

/** Use case for model catalog operations. */
@Singleton
class ModelCatalogUseCase
@Inject
constructor(private val modelCatalogRepository: ModelCatalogRepository) {
  /** Observe all models in the catalog. */
  @ReactiveStream("All catalog models")
  fun observeAllModels(): Flow<List<ModelPackage>> = modelCatalogRepository.observeAllModels()

  /** Observe installed models in the catalog. */
  @ReactiveStream("Installed models")
  fun observeInstalledModels(): Flow<List<ModelPackage>> =
    modelCatalogRepository.observeInstalledModels()

  /** Get all models in the catalog. */
  @OneShot("Fetch catalog snapshot")
  suspend fun getAllModels(): NanoAIResult<List<ModelPackage>> =
    guardRepositoryCall(message = "Failed to get all models") {
      val models = modelCatalogRepository.getAllModels()
      NanoAIResult.success(models)
    }

  /** Get a specific model by ID. */
  @OneShot("Fetch model by identifier")
  suspend fun getModel(modelId: String): NanoAIResult<ModelPackage?> =
    guardRepositoryCall(
      message = "Failed to get model $modelId",
      context = mapOf("modelId" to modelId),
    ) {
      val model = modelCatalogRepository.getModel(modelId)
      NanoAIResult.success(model)
    }

  /** Insert or update a model in the catalog. */
  @OneShot("Insert or update model metadata")
  suspend fun upsertModel(model: ModelPackage): NanoAIResult<Unit> =
    guardRepositoryCall(
      message = "Failed to upsert model ${model.modelId}",
      context = mapOf("modelId" to model.modelId),
    ) {
      modelCatalogRepository.upsertModel(model)
      NanoAIResult.success(Unit)
    }

  /** Record an offline fallback scenario. */
  @OneShot("Record offline catalog fallback")
  suspend fun recordOfflineFallback(
    reason: String,
    cachedCount: Int,
    message: String? = null,
  ): NanoAIResult<Unit> =
    guardRepositoryCall(
      message = "Failed to record offline fallback",
      context =
        mapOf("reason" to reason, "cachedCount" to cachedCount.toString()) +
          (message?.let { mapOf("message" to it) } ?: emptyMap()),
    ) {
      modelCatalogRepository.recordOfflineFallback(reason, cachedCount, message)
      NanoAIResult.success(Unit)
    }

  private inline fun <T> guardRepositoryCall(
    message: String,
    context: Map<String, String> = emptyMap(),
    block: () -> NanoAIResult<T>,
  ): NanoAIResult<T> {
    return try {
      block()
    } catch (cancellation: CancellationException) {
      throw cancellation
    } catch (ioException: IOException) {
      NanoAIResult.recoverable(message = message, cause = ioException, context = context)
    } catch (illegalStateException: IllegalStateException) {
      NanoAIResult.recoverable(message = message, cause = illegalStateException, context = context)
    } catch (illegalArgumentException: IllegalArgumentException) {
      NanoAIResult.recoverable(
        message = message,
        cause = illegalArgumentException,
        context = context,
      )
    } catch (exception: Exception) {
      NanoAIResult.recoverable(message = message, cause = exception, context = context)
    }
  }
}
