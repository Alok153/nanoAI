package com.vjaykrsna.nanoai.feature.library.domain

import android.util.Log
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.feature.library.data.ModelCatalogRepository
import com.vjaykrsna.nanoai.feature.library.data.catalog.ModelCatalogSource
import javax.inject.Inject
import javax.inject.Singleton

/** Coordinates catalog refresh from remote or bundled sources. */
@Singleton
class RefreshModelCatalogUseCase
@Inject
constructor(
  private val modelCatalogSource: ModelCatalogSource,
  private val modelCatalogRepository: ModelCatalogRepository,
) {
  suspend operator fun invoke(): NanoAIResult<Unit> {
    val context = mutableMapOf<String, String>()
    val models =
      try {
        modelCatalogSource.fetchCatalog().also { fetched ->
          context["modelCount"] = fetched.size.toString()
          context["source"] = modelCatalogSource.javaClass.simpleName
        }
      } catch (error: Throwable) {
        return handleFetchFailure(context, error)
      }

    Log.i(TAG, "catalogRefresh success context=${context.toLog()}")

    return try {
      modelCatalogRepository.replaceCatalog(models)
      modelCatalogRepository.recordRefreshSuccess(
        source = context["source"] ?: modelCatalogSource.javaClass.simpleName,
        modelCount = models.size,
      )
      NanoAIResult.success(Unit)
    } catch (error: Throwable) {
      val wrapped = IllegalStateException("Failed to replace model catalog", error)
      Log.e(TAG, "catalogRefresh failure context=${context.toLog()}", wrapped)
      NanoAIResult.recoverable(
        message = "Failed to replace model catalog",
        cause = wrapped,
        context = context,
      )
    }
  }

  private suspend fun handleFetchFailure(
    context: MutableMap<String, String>,
    error: Throwable,
  ): NanoAIResult<Unit> {
    context["errorType"] = error.javaClass.simpleName
    error.message?.takeIf { it.isNotBlank() }?.let { message -> context["errorMessage"] = message }
    context["fallback"] = "cached"
    val cachedModels =
      runCatching { modelCatalogRepository.getAllModels() }.getOrDefault(emptyList())
    context["cachedCount"] = cachedModels.size.toString()
    Log.w(TAG, "catalogRefresh fallback context=${context.toLog()}", error)
    modelCatalogRepository.recordOfflineFallback(
      reason = error.javaClass.simpleName,
      cachedCount = cachedModels.size,
      message = error.message,
    )
    // Even though fetch failed, we're falling back to cached data, so this is still a success
    return NanoAIResult.success(Unit)
  }

  private fun Map<String, String>.toLog(): String = buildString {
    append('{')
    entries.joinToString(separator = ",") { (key, value) -> "$key=$value" }.let(this::append)
    append('}')
  }

  companion object {
    private const val TAG = "RefreshModelCatalogUseCase"
  }
}
