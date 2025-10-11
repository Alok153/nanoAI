package com.vjaykrsna.nanoai.feature.library.domain

import android.util.Log
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
  suspend operator fun invoke(): Result<Unit> = runCatching {
    val models = modelCatalogSource.fetchCatalog()
    val context = mapOf("modelCount" to models.size.toString())
    Log.i(TAG, "catalogRefresh success context=${context.toLog()}")
    try {
      modelCatalogRepository.replaceCatalog(models)
    } catch (error: Throwable) {
      val wrapped = IllegalStateException("Failed to replace model catalog", error)
      Log.e(TAG, "catalogRefresh failure context=${context.toLog()}", wrapped)
      throw wrapped
    }
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
