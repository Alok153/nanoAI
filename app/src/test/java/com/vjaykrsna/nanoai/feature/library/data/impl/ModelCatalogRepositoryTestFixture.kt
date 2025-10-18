package com.vjaykrsna.nanoai.feature.library.data.impl

import android.content.Context
import com.vjaykrsna.nanoai.core.data.db.daos.ChatThreadDao
import com.vjaykrsna.nanoai.feature.library.model.InstallState
import com.vjaykrsna.nanoai.feature.library.model.ProviderType
import com.vjaykrsna.nanoai.model.catalog.DeliveryType
import com.vjaykrsna.nanoai.model.catalog.ModelPackageEntity
import com.vjaykrsna.nanoai.model.catalog.ModelPackageReadDao
import com.vjaykrsna.nanoai.model.catalog.ModelPackageWriteDao
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

internal data class ModelCatalogRepositoryTestFixture(
  val modelPackageReadDao: ModelPackageReadDao,
  val modelPackageWriteDao: ModelPackageWriteDao,
  val chatThreadDao: ChatThreadDao,
  val context: Context,
  val clock: MutableClock,
  val repository: ModelCatalogRepositoryImpl,
) {
  fun existingEntity(
    modelId: String,
    checksum: String,
    signature: String?,
    downloadTaskId: UUID?,
    installState: InstallState,
  ): ModelPackageEntity =
    ModelPackageEntity(
      modelId = modelId,
      displayName = "Existing $modelId",
      version = "1.0",
      providerType = ProviderType.CLOUD_API,
      deliveryType = DeliveryType.CLOUD_FALLBACK,
      minAppVersion = 5,
      sizeBytes = 128,
      capabilities = setOf("text"),
      installState = installState,
      downloadTaskId = downloadTaskId?.toString(),
      manifestUrl = "https://example.com/$modelId",
      checksumSha256 = checksum,
      signature = signature,
      createdAt = Instant.parse("2025-10-08T00:00:00Z"),
      updatedAt = Instant.parse("2025-10-08T12:00:00Z"),
    )
}

internal fun createModelCatalogRepositoryFixture(
  tempDir: File,
  configureDao: ModelPackageReadDao.() -> Unit = {}
): ModelCatalogRepositoryTestFixture {
  val readDao =
    mockk<ModelPackageReadDao>(relaxed = true) {
      every { observeAll() } returns flowOf(emptyList())
      every { observeInstalled() } returns flowOf(emptyList())
      configureDao()
    }
  val writeDao = mockk<ModelPackageWriteDao>(relaxed = true)
  val chatThreadDao = mockk<ChatThreadDao>(relaxed = true)
  val context = mockk<Context>()
  val clock = MutableClock(Instant.parse("2025-10-10T00:00:00Z"))
  every { context.filesDir } returns tempDir

  val repository =
    ModelCatalogRepositoryImpl(
      modelPackageReadDao = readDao,
      modelPackageWriteDao = writeDao,
      chatThreadDao = chatThreadDao,
      context = context,
      clock = clock,
    )

  return ModelCatalogRepositoryTestFixture(
    modelPackageReadDao = readDao,
    modelPackageWriteDao = writeDao,
    chatThreadDao = chatThreadDao,
    context = context,
    clock = clock,
    repository = repository,
  )
}

internal class MutableClock(private var instant: Instant) : Clock {
  override fun now(): Instant = instant

  fun advanceTo(next: Instant) {
    instant = next
  }
}
