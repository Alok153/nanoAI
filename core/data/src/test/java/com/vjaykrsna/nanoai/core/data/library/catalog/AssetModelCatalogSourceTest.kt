package com.vjaykrsna.nanoai.core.data.library.catalog

import android.content.Context
import android.content.res.AssetManager
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class AssetModelCatalogSourceTest {

  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun fetchCatalog_parsesAssetsCorrectly() = runTest {
    val assetManager = mockk<AssetManager>()
    val context = mockk<Context>()
    val payload =
      """
        {
          "version": 3,
          "models": [
            {
              "id": "model-1",
              "display_name": "Model One",
              "version": "1.0.0",
              "provider": "cloud_api",
              "delivery": "cloud_fallback",
              "min_app_version": 5,
              "size_bytes": 1024,
              "capabilities": [" text ", ""],
              "manifest_url": "https://example.com/model-1",
              "checksum_sha256": "abc123",
              "signature": "sig",
              "created_at": "2025-01-01T00:00:00Z",
              "updated_at": "2025-01-02T00:00:00Z"
            }
          ]
        }
      """
        .trimIndent()
    every { assetManager.open("model-catalog.json") } returns
      ByteArrayInputStream(payload.toByteArray())
    every { context.assets } returns assetManager
    val clock = MutableClock(Instant.parse("2025-01-15T00:00:00Z"))
    val source = AssetModelCatalogSource(context, json, clock)

    val catalog = source.fetchCatalog()

    assertThat(catalog).hasSize(1)
    val model = catalog.first()
    assertThat(model.modelId).isEqualTo("model-1")
    assertThat(model.displayName).isEqualTo("Model One")
    assertThat(model.capabilities).containsExactly("text")
    assertThat(model.checksumSha256).isEqualTo("abc123")
    assertThat(model.createdAt).isEqualTo(Instant.parse("2025-01-01T00:00:00Z"))
    assertThat(model.updatedAt).isEqualTo(Instant.parse("2025-01-02T00:00:00Z"))
  }

  @Test
  fun fetchCatalog_handlesMalformedJson() = runTest {
    val assetManager = mockk<AssetManager>()
    val context = mockk<Context>()
    val invalidPayload = "{ invalid json"
    every { assetManager.open("model-catalog.json") } returns
      ByteArrayInputStream(invalidPayload.toByteArray())
    every { context.assets } returns assetManager
    val source =
      AssetModelCatalogSource(context, json, MutableClock(Instant.parse("2025-01-01T00:00:00Z")))

    val error = assertFailsWith<CatalogLoadException> { source.fetchCatalog() }

    assertThat(error.message).contains("Failed to parse")
  }

  @Test
  fun fetchCatalog_missingAssetThrowsCatalogLoadException() = runTest {
    val assetManager = mockk<AssetManager>()
    val context = mockk<Context>()
    every { assetManager.open("model-catalog.json") } throws FileNotFoundException("missing")
    every { context.assets } returns assetManager
    val source =
      AssetModelCatalogSource(context, json, MutableClock(Instant.parse("2025-01-01T00:00:00Z")))

    val error = assertFailsWith<CatalogLoadException> { source.fetchCatalog() }

    assertThat(error.message).contains("Missing asset")
  }

  @Test
  fun fetchCatalog_populatesTimestampsWhenMissing() = runTest {
    val assetManager = mockk<AssetManager>()
    val context = mockk<Context>()
    val payload =
      """
        {
          "version": 1,
          "models": [
            {
              "id": "model-2",
              "display_name": "Model Two",
              "version": "2.0",
              "provider": "cloud_api",
              "delivery": "cloud_fallback",
              "min_app_version": 1,
              "size_bytes": 2048,
              "capabilities": ["vision"],
              "manifest_url": "https://example.com/model-2"
            }
          ]
        }
      """
        .trimIndent()
    every { assetManager.open("model-catalog.json") } returns
      ByteArrayInputStream(payload.toByteArray())
    every { context.assets } returns assetManager
    val clock = MutableClock(Instant.parse("2025-02-01T12:00:00Z"))
    val source = AssetModelCatalogSource(context, json, clock)

    val catalog = source.fetchCatalog()
    val expected = clock.now()

    assertThat(catalog.single().createdAt).isEqualTo(expected)
    assertThat(catalog.single().updatedAt).isEqualTo(expected)
  }

  private class MutableClock(private var instant: Instant) : Clock {
    override fun now(): Instant = instant
  }
}
