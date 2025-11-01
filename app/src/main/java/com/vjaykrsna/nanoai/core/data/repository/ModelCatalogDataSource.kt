package com.vjaykrsna.nanoai.core.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.network.ConnectivityStatusProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * Data source for model catalog with offline-first approach.
 *
 * Caches catalog in DataStore and provides network fallback for updates.
 */
@Singleton
class ModelCatalogDataSource
@Inject
constructor(
  private val dataStore: DataStore<Preferences>,
  private val connectivityStatusProvider: ConnectivityStatusProvider,
  private val json: Json = Json,
) {

  private val catalogKey = stringPreferencesKey("model_catalog")

  /**
   * Get the model catalog, using offline-first approach.
   *
   * @return Flow of catalog models, cached locally with network updates.
   */
  fun getCatalog(): Flow<List<ModelPackage>> {
    return dataStore.data.map { preferences ->
      val cachedJson = preferences[catalogKey]
      if (cachedJson != null) {
        try {
          json.decodeFromString<List<ModelPackage>>(cachedJson)
        } catch (e: Exception) {
          Log.e("ModelCatalogDataSource", "Failed to decode cached catalog", e)
          emptyList()
        }
      } else {
        emptyList()
      }
    }
  }

  /**
   * Refresh the catalog from network if online, otherwise return cached data.
   *
   * @param networkFetcher Function to fetch catalog from network
   * @return Result containing the catalog models
   */
  suspend fun refreshCatalog(
    networkFetcher: suspend () -> List<ModelPackage>
  ): NanoAIResult<List<ModelPackage>> {
    return try {
      val isOnline = connectivityStatusProvider.isOnline()

      val models =
        if (isOnline) {
          // Try network first
          val networkModels = networkFetcher()
          // Cache the result
          cacheCatalog(networkModels)
          networkModels
        } else {
          // Offline: use cached data
          val cached = getCachedCatalog()
          if (cached.isNotEmpty()) {
            cached
          } else {
            return NanoAIResult.recoverable(
              message = "No cached catalog available and offline",
              cause = null,
              context = emptyMap(),
            )
          }
        }

      NanoAIResult.success(models)
    } catch (e: Exception) {
      // Network failed, try cache
      val cached = getCachedCatalog()
      if (cached.isNotEmpty()) {
        NanoAIResult.success(cached)
      } else {
        NanoAIResult.recoverable(
          message = "Failed to load catalog from network and no cache available",
          cause = e,
          context = emptyMap(),
        )
      }
    }
  }

  /** Get cached catalog synchronously. */
  private suspend fun getCachedCatalog(): List<ModelPackage> {
    return dataStore.data.first()[catalogKey]?.let { jsonString ->
      try {
        json.decodeFromString<List<ModelPackage>>(jsonString)
      } catch (e: Exception) {
        Log.e("ModelCatalogDataSource", "Failed to decode cached catalog", e)
        emptyList()
      }
    } ?: emptyList()
  }

  /** Cache the catalog in DataStore. */
  private suspend fun cacheCatalog(models: List<ModelPackage>) {
    val jsonString = json.encodeToString(models)
    dataStore.edit { preferences -> preferences[catalogKey] = jsonString }
  }
}
