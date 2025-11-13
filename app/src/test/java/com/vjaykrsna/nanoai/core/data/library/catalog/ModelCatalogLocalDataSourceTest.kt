package com.vjaykrsna.nanoai.core.data.library.catalog

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.db.entities.DownloadManifestEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ModelCatalogLocalDataSourceTest {

  private val modelPackageWriteDao: ModelPackageWriteDao = mockk(relaxed = true)
  private val relationsDao: ModelPackageRelationsDao = mockk(relaxed = true)
  private val downloadManifestDao: DownloadManifestDao = mockk(relaxed = true)
  private val fixedInstant: Instant = Instant.parse("2025-05-01T12:00:00Z")
  private lateinit var localDataSource: ModelCatalogLocalDataSource

  @BeforeEach
  fun setUp() {
    val fixedClock =
      object : Clock {
        override fun now(): Instant = fixedInstant
      }
    localDataSource =
      ModelCatalogLocalDataSource(
        modelPackageWriteDao = modelPackageWriteDao,
        relationsDao = relationsDao,
        downloadManifestDao = downloadManifestDao,
        clock = fixedClock,
      )
  }

  @Test
  fun `cacheManifest stamps fetchedAt with current clock`() = runTest {
    val manifest = sampleManifest(fetchedAt = Instant.parse("2025-04-30T09:00:00Z"))
    val captured = slot<DownloadManifestEntity>()
    coEvery { downloadManifestDao.upsertManifest(capture(captured)) } returns Unit

    localDataSource.cacheManifest(manifest)

    coVerify(exactly = 1) { downloadManifestDao.upsertManifest(any()) }
    assertThat(captured.captured.fetchedAt).isEqualTo(fixedInstant)
  }

  @Test
  fun `cacheManifests reuses snapshot timestamp`() = runTest {
    val manifests = listOf(sampleManifest(version = "v1"), sampleManifest(version = "v2"))
    val captured = slot<List<DownloadManifestEntity>>()
    coEvery { downloadManifestDao.upsertManifests(capture(captured)) } returns Unit

    localDataSource.cacheManifests(manifests)

    coVerify(exactly = 1) { downloadManifestDao.upsertManifests(any()) }
    assertThat(captured.captured).hasSize(2)
    captured.captured.forEach { assertThat(it.fetchedAt).isEqualTo(fixedInstant) }
  }

  @Test
  fun `updateIntegrityMetadata forwards timestamped arguments`() = runTest {
    localDataSource.updateIntegrityMetadata("persona-model", "checksum", null)

    coVerify(exactly = 1) {
      modelPackageWriteDao.updateIntegrityMetadata(
        modelId = "persona-model",
        checksum = "checksum",
        signature = null,
        updatedAt = fixedInstant,
      )
    }
  }

  @Test
  fun `pruneExpired delegates to dao with supplied instant`() = runTest {
    localDataSource.pruneExpired(fixedInstant)

    coVerify(exactly = 1) { downloadManifestDao.deleteExpiredManifests(fixedInstant) }
  }

  private fun sampleManifest(
    modelId: String = "model",
    version: String = "1.0.0",
    fetchedAt: Instant = fixedInstant,
  ): DownloadManifestEntity =
    DownloadManifestEntity(
      modelId = modelId,
      version = version,
      checksumSha256 = "deadbeef".repeat(8),
      sizeBytes = 1024,
      downloadUrl = "https://cdn.nanoai.app/$modelId/$version.bin",
      signature = null,
      publicKeyUrl = null,
      expiresAt = null,
      fetchedAt = fetchedAt,
      releaseNotes = null,
    )
}
