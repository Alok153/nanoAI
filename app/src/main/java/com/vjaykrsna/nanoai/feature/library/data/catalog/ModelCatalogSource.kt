package com.vjaykrsna.nanoai.feature.library.data.catalog

import android.content.Context
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

/** Abstraction for loading model catalog definitions from an external source. */
interface ModelCatalogSource {
  suspend fun fetchCatalog(): List<ModelPackage>
}

/** Asset-backed implementation that reads `model-catalog.json` from bundled assets. */
@Singleton
class AssetModelCatalogSource
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val json: Json,
  private val clock: Clock = Clock.System,
) : ModelCatalogSource {
  private val ioDispatcher = Dispatchers.IO

  override suspend fun fetchCatalog(): List<ModelPackage> =
    withContext(ioDispatcher) {
      val payload = readAsset(ASSET_FILE_NAME)
      val config =
        runCatching { json.decodeFromString(ModelCatalogConfig.serializer(), payload) }
          .getOrElse { error ->
            throw CatalogLoadException("Failed to parse $ASSET_FILE_NAME", error)
          }
      config.models.map { it.toModelPackage(clock) }
    }

  private fun readAsset(assetName: String): String {
    return runCatching { context.assets.open(assetName).bufferedReader().use { it.readText() } }
      .getOrElse { error -> throw CatalogLoadException("Missing asset $assetName", error) }
  }

  private companion object {
    private const val ASSET_FILE_NAME = "model-catalog.json"
  }
}

/** Exception thrown when the catalog cannot be loaded from the configured source. */
class CatalogLoadException(message: String, cause: Throwable? = null) :
  IllegalStateException(message, cause)
