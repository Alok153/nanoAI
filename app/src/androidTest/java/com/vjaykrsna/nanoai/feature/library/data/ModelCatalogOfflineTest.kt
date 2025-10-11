package com.vjaykrsna.nanoai.feature.library.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.vjaykrsna.nanoai.core.data.db.NanoAIDatabase
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.data.catalog.ModelCatalogConfig
import com.vjaykrsna.nanoai.feature.library.data.catalog.ModelCatalogSource
import com.vjaykrsna.nanoai.feature.library.data.impl.ModelCatalogRepositoryImpl
import com.vjaykrsna.nanoai.feature.library.domain.RefreshModelCatalogUseCase
import com.vjaykrsna.nanoai.feature.library.model.InstallState
import com.vjaykrsna.nanoai.feature.library.model.ProviderType
import com.vjaykrsna.nanoai.model.catalog.DeliveryType
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ModelCatalogOfflineTest {

  private lateinit var context: Context
  private lateinit var database: NanoAIDatabase
  private lateinit var repository: ModelCatalogRepositoryImpl
  private lateinit var useCase: RefreshModelCatalogUseCase
  private lateinit var dispatcher: TestDispatcher
  private lateinit var mockWebServer: MockWebServer
  private val json = Json { ignoreUnknownKeys = true }

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    dispatcher = StandardTestDispatcher()
    Dispatchers.setMain(dispatcher)
    mockWebServer = MockWebServer().apply { start() }
    database =
      Room.inMemoryDatabaseBuilder(context, NanoAIDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    repository =
      ModelCatalogRepositoryImpl(
        database.modelPackageReadDao(),
        database.modelPackageWriteDao(),
        database.chatThreadDao(),
        context,
      )
    useCase = RefreshModelCatalogUseCase(HttpModelCatalogSource(mockWebServer, json), repository)
  }

  @After
  fun tearDown() {
    database.close()
    mockWebServer.shutdown()
    Dispatchers.resetMain()
  }

  @Test
  fun offlineRefresh_preservesCachedCatalogAndSignalsFallbackSuccess() =
    runTest(dispatcher) {
      val cached = sampleModel("cached-model")
      repository.replaceCatalog(listOf(cached))

      mockWebServer.enqueue(MockResponse().setResponseCode(503).setBody("Device farm offline"))

      val result = useCase()

      assertWithMessage("offline refresh should resolve with cached data")
        .that(result.isSuccess)
        .isTrue()
      assertThat(result.exceptionOrNull()).isNull()
      assertThat(repository.getAllModels()).containsExactly(cached)
      val recorded = mockWebServer.takeRequest(1, TimeUnit.SECONDS)
      assertThat(recorded).isNotNull()
      assertThat(recorded?.path).isEqualTo("/catalog")
    }

  private fun sampleModel(id: String): ModelPackage =
    ModelPackage(
      modelId = id,
      displayName = "Sample $id",
      version = "1.0.0",
      providerType = ProviderType.CLOUD_API,
      deliveryType = DeliveryType.CLOUD_FALLBACK,
      minAppVersion = 1,
      sizeBytes = 1_024,
      capabilities = setOf("text"),
      installState = InstallState.NOT_INSTALLED,
      downloadTaskId = null,
      manifestUrl = "${mockWebServer.url("/")}$id.manifest",
      checksumSha256 = "checksum-$id",
      signature = null,
      createdAt = Instant.parse("2025-10-10T00:00:00Z"),
      updatedAt = Instant.parse("2025-10-10T00:00:00Z"),
    )

  private class HttpModelCatalogSource(
    private val server: MockWebServer,
    private val json: Json,
    private val client: OkHttpClient = OkHttpClient(),
  ) : ModelCatalogSource {
    private val clock = Clock.System

    override suspend fun fetchCatalog(): List<ModelPackage> {
      val request =
        Request.Builder().url(server.url("/catalog")).header("Accept", "application/json").build()
      val response = client.newCall(request).execute()
      if (!response.isSuccessful) {
        response.close()
        throw IOException("Device farm offline: HTTP ${'$'}{response.code}")
      }
      val body = response.body?.string() ?: throw IOException("Empty catalog payload")
      response.close()
      val config = json.decodeFromString(ModelCatalogConfig.serializer(), body)
      return config.models.map { model -> model.toModelPackage(clock) }
    }
  }
}
