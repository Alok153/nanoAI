package com.vjaykrsna.nanoai.core.domain.library

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.library.DeliveryType
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Test

class ListLeapModelsUseCaseTest {

  @Test
  fun invoke_returnsLeapModels() = runTest {
    val repository = mockk<ModelCatalogRepository>()
    val leapModel = modelPackage(id = "leap", providerType = ProviderType.LEAP)
    val otherModel = modelPackage(id = "other", providerType = ProviderType.MEDIA_PIPE)
    coEvery { repository.getAllModels() } returns listOf(leapModel, otherModel)

    val result = ListLeapModelsUseCase(repository).invoke()

    assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
    val success = result as NanoAIResult.Success
    assertThat(success.value).containsExactly(leapModel)
  }

  @Test
  fun invoke_wrapsRepositoryErrors() = runTest {
    val repository = mockk<ModelCatalogRepository>()
    val failure = IllegalStateException("db locked")
    coEvery { repository.getAllModels() } throws failure

    val result = ListLeapModelsUseCase(repository).invoke()

    assertThat(result).isInstanceOf(NanoAIResult.RecoverableError::class.java)
    val error = result as NanoAIResult.RecoverableError
    assertThat(error.cause).isEqualTo(failure)
    assertThat(error.context["providerType"]).isEqualTo(ProviderType.LEAP.name)
  }

  private fun modelPackage(id: String, providerType: ProviderType): ModelPackage =
    ModelPackage(
      modelId = id,
      displayName = "Model $id",
      version = "1.0.0",
      providerType = providerType,
      deliveryType = DeliveryType.LOCAL_ARCHIVE,
      minAppVersion = 1,
      sizeBytes = 1_024,
      capabilities = setOf("text"),
      installState = InstallState.NOT_INSTALLED,
      manifestUrl = "https://example.com/$id",
      checksumSha256 = "a".repeat(64),
      signature = null,
      downloadTaskId = null,
      createdAt = Instant.fromEpochSeconds(0),
      updatedAt = Instant.fromEpochSeconds(0),
    )
}
