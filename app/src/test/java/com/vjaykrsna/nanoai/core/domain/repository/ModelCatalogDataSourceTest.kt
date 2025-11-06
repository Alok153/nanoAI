package com.vjaykrsna.nanoai.core.domain.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.network.ConnectivityStatusProvider
import com.vjaykrsna.nanoai.testing.DomainTestBuilders
import io.mockk.coEvery
import io.mockk.mockk
import java.io.File
import java.io.IOException
import java.nio.file.Path
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@OptIn(ExperimentalCoroutinesApi::class)
class ModelCatalogDataSourceTest {

  @TempDir lateinit var tempDir: Path

  private val connectivityStatusProvider: ConnectivityStatusProvider = mockk()
  private lateinit var json: Json

  @BeforeEach
  fun setUp() {
    json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
  }

  @Test
  fun `getCatalog returns empty list when cached json is invalid`() = runTest {
    val dataStore = newDataStore(this, "invalid_cache.preferences_pb")
    val catalogKey = stringPreferencesKey("model_catalog")
    dataStore.edit { preferences -> preferences[catalogKey] = "not-json" }
    val dataSource = ModelCatalogDataSource(dataStore, connectivityStatusProvider, json)

    val result = dataSource.getCatalog().first()

    assertThat(result).isEmpty()
  }

  @Test
  fun `refreshCatalog falls back to cache on io exception`() = runTest {
    val dataStore = newDataStore(this, "cache_fallback.preferences_pb")
    val cachedModel = DomainTestBuilders.buildModelPackage(modelId = "cached")
    val dataSource = ModelCatalogDataSource(dataStore, connectivityStatusProvider, json)
    coEvery { connectivityStatusProvider.isOnline() } returnsMany listOf(true, true)

    val initialResult = dataSource.refreshCatalog { listOf(cachedModel) }
    assertThat(initialResult).isInstanceOf(NanoAIResult.Success::class.java)

    val fallbackResult = dataSource.refreshCatalog { throw IOException("network down") }

    assertThat(fallbackResult).isInstanceOf(NanoAIResult.Success::class.java)
    val success = fallbackResult as NanoAIResult.Success
    assertThat(success.value).containsExactly(cachedModel)
  }

  @Test
  fun `refreshCatalog returns recoverable error when no cache available`() = runTest {
    val dataStore = newDataStore(this, "no_cache.preferences_pb")
    val dataSource = ModelCatalogDataSource(dataStore, connectivityStatusProvider, json)
    coEvery { connectivityStatusProvider.isOnline() } returns true

    val result = dataSource.refreshCatalog { throw IllegalStateException("unexpected") }

    assertThat(result).isInstanceOf(NanoAIResult.RecoverableError::class.java)
  }
  private fun newDataStore(
    scope: TestScope,
    fileName: String,
  ): androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> {
    val file = File(tempDir.toFile(), fileName)
    return PreferenceDataStoreFactory.create(scope = scope.backgroundScope) { file }
  }
}
