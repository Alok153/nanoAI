package com.vjaykrsna.nanoai.core.data.library.workers

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.db.entities.ModelPackageEntity
import com.vjaykrsna.nanoai.core.domain.model.library.DeliveryType
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test

class ModelDownloadWorkerNamingTest {
  @Test
  fun resolveModelName_prefersDisplayName_whenPresent() {
    val entity = modelPackageEntity(displayName = "Nano Diffusion XL")

    val name = resolveModelName(entity, fallbackId = "nano-diffusion-xl")

    assertThat(name).isEqualTo("Nano Diffusion XL")
  }

  @Test
  fun resolveModelName_fallsBackToModelId_whenDisplayNameBlankOrMissing() {
    val blankEntity = modelPackageEntity(displayName = "")

    val blankName = resolveModelName(blankEntity, fallbackId = "nano-delta")
    val missingName = resolveModelName(null, fallbackId = "nano-beta")

    assertThat(blankName).isEqualTo("nano-delta")
    assertThat(missingName).isEqualTo("nano-beta")
  }

  private fun modelPackageEntity(displayName: String): ModelPackageEntity =
    ModelPackageEntity(
      modelId = "nano-diffusion-xl",
      displayName = displayName,
      version = "1.0.0",
      providerType = ProviderType.MEDIA_PIPE,
      deliveryType = DeliveryType.LOCAL_ARCHIVE,
      minAppVersion = 1,
      sizeBytes = 1024,
      capabilities = emptySet(),
      installState = InstallState.NOT_INSTALLED,
      downloadTaskId = null,
      manifestUrl = "https://example.com/model",
      checksumSha256 = null,
      signature = null,
      createdAt = Instant.fromEpochMilliseconds(0),
      updatedAt = Instant.fromEpochMilliseconds(0),
    )
}
