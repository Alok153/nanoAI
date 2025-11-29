package com.vjaykrsna.nanoai.core.data.library.huggingface

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.library.huggingface.network.HuggingFaceService
import java.net.HttpURLConnection
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private val jsonMediaType = "application/json".toMediaType()

private val json = Json { ignoreUnknownKeys = true }

class HuggingFaceManifestFetcherTest {
  private lateinit var server: MockWebServer
  private lateinit var service: HuggingFaceService
  private lateinit var fetcher: HuggingFaceManifestFetcher

  @BeforeEach
  fun setUp() {
    server = MockWebServer()
    server.start()

    val retrofit =
      Retrofit.Builder()
        .baseUrl(server.url("/").toString())
        .client(OkHttpClient())
        .addConverterFactory(json.asConverterFactory(jsonMediaType))
        .build()

    service = retrofit.create(HuggingFaceService::class.java)
    fetcher = HuggingFaceManifestFetcher(service, FixedClock)
  }

  @AfterEach
  fun tearDown() {
    server.shutdown()
  }

  @Test
  fun `fetchManifest uses paths-info metadata when available`() = runTest {
    enqueueJson(
      body =
        """
        [
          {
            "path": "LiteRT/model.bin",
            "size": 1048576,
            "sha256": "${"a".repeat(64)}"
          }
        ]
        """
          .trimIndent()
    )

    val request =
      HuggingFaceManifestRequest(
        modelId = "gemma",
        repository = "google/gemma-2-2b-it",
        revision = "main",
        artifactPath = "LiteRT/model.bin",
        version = "main",
      )

    val manifest = fetcher.fetchManifest(request)

    assertThat(manifest.modelId).isEqualTo("gemma")
    assertThat(manifest.checksumSha256).isEqualTo("${"a".repeat(64)}")
    assertThat(manifest.sizeBytes).isEqualTo(1_048_576)
    assertThat(manifest.downloadUrl)
      .isEqualTo(
        "https://huggingface.co/google/gemma-2-2b-it/resolve/main/LiteRT/model.bin?download=1"
      )
    assertThat(manifest.fetchedAt).isEqualTo(FixedClock.now())
    assertThat(server.requestCount).isEqualTo(1)
  }

  @Test
  fun `fetchManifest falls back to model summary`() = runTest {
    enqueueJson("[]")
    enqueueJson(
      body =
        """
        {
          "modelId": "google/gemma-2-2b-it",
          "sha": "${"b".repeat(64)}",
          "siblings": [
            {
              "rfilename": "LiteRT/model.bin",
              "size": 2097152,
              "lfs": {
                "oid": "sha256:${"c".repeat(64)}",
                "size": 2097152
              }
            }
          ]
        }
        """
          .trimIndent()
    )

    val request =
      HuggingFaceManifestRequest(
        modelId = "gemma",
        repository = "google/gemma-2-2b-it",
        revision = "main",
        artifactPath = "LiteRT/model.bin",
        version = "main",
      )

    val manifest = fetcher.fetchManifest(request)

    assertThat(manifest.checksumSha256).isEqualTo("${"c".repeat(64)}")
    assertThat(manifest.sizeBytes).isEqualTo(2_097_152)
    assertThat(server.requestCount).isEqualTo(2)
  }

  @Test
  fun `fetchManifest throws when checksum missing`() = runTest {
    enqueueJson("[]")
    enqueueJson(
      body =
        """
        {
          "modelId": "google/gemma-2-2b-it",
          "siblings": [
            {
              "rfilename": "LiteRT/model.bin",
              "size": 1024
            }
          ]
        }
        """
          .trimIndent()
    )

    val request =
      HuggingFaceManifestRequest(
        modelId = "gemma",
        repository = "google/gemma-2-2b-it",
        revision = "main",
        artifactPath = "LiteRT/model.bin",
        version = "main",
      )

    assertThrows<IllegalArgumentException> { fetcher.fetchManifest(request) }
  }

  private fun enqueueJson(body: String, status: Int = HttpURLConnection.HTTP_OK) {
    server.enqueue(
      MockResponse()
        .setResponseCode(status)
        .setHeader("Content-Type", "application/json")
        .setBody(Buffer().writeString(body, Charsets.UTF_8))
    )
  }

  private object FixedClock : Clock {
    private val fixed = Instant.parse("2024-01-01T00:00:00Z")

    override fun now(): Instant = fixed
  }
}
