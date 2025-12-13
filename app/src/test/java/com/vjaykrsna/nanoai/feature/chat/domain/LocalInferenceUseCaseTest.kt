package com.vjaykrsna.nanoai.feature.chat.domain

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.library.DeliveryType
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import com.vjaykrsna.nanoai.testing.DomainTestBuilders
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LocalInferenceUseCaseTest {
  private lateinit var repository: FakeLocalInferenceRepository
  private lateinit var useCase: LocalInferenceUseCase

  @BeforeEach
  fun setUp() {
    repository = FakeLocalInferenceRepository()
    useCase = LocalInferenceUseCase(repository)
  }

  @Test
  fun `prepareForOffline prefers current ready model`() = runTest {
    val readyModel = installedLocal(modelId = "local-ready")
    repository.setModels(listOf(readyModel))
    repository.setReadyModels(setOf("local-ready"))

    val result = useCase.prepareForOffline("local-ready", personaModelPreference = null)

    val readiness = result as LocalModelReadiness.Ready
    assertThat(readiness.candidate.modelId).isEqualTo("local-ready")
    assertThat(readiness.autoSelected).isFalse()
  }

  @Test
  fun `prepareForOffline falls back to persona preference`() = runTest {
    val currentModel = installedLocal(modelId = "cloud-bound")
    val personaPreferred = installedLocal(modelId = "persona-preferred")
    repository.setModels(listOf(currentModel, personaPreferred))
    repository.setReadyModels(setOf("persona-preferred"))

    val result =
      useCase.prepareForOffline("cloud-bound", personaModelPreference = "persona-preferred")

    val readiness = result as LocalModelReadiness.Ready
    assertThat(readiness.candidate.modelId).isEqualTo("persona-preferred")
    assertThat(readiness.autoSelected).isTrue()
  }

  @Test
  fun `prepareForOffline reports missing when no ready models`() = runTest {
    val installed = installedLocal(modelId = "local-offline")
    repository.setModels(listOf(installed))
    repository.setReadyModels(emptySet())

    val result =
      useCase.prepareForOffline(currentModelId = "local-offline", personaModelPreference = null)

    val missing = result as LocalModelReadiness.Missing
    assertThat(missing.reason).isEqualTo(LocalModelReadiness.MissingReason.NOT_READY)
  }

  @Test
  fun `observeLocalModelCandidates filters out cloud models`() = runTest {
    val localReady = installedLocal(modelId = "on-device", providerType = ProviderType.MEDIA_PIPE)
    val cloudModel =
      DomainTestBuilders.buildModelPackage(
        modelId = "cloud",
        providerType = ProviderType.CLOUD_API,
        installState = InstallState.INSTALLED,
        deliveryType = DeliveryType.CLOUD_FALLBACK,
      )
    repository.setModels(listOf(localReady, cloudModel))

    val candidates = useCase.observeLocalModelCandidates().first()

    assertThat(candidates).hasSize(1)
    assertThat(candidates.first().modelId).isEqualTo("on-device")
  }

  private fun installedLocal(
    modelId: String,
    providerType: ProviderType = ProviderType.MEDIA_PIPE,
  ): ModelPackage =
    DomainTestBuilders.buildModelPackage(
      modelId = modelId,
      providerType = providerType,
      installState = InstallState.INSTALLED,
    )
}

private class FakeLocalInferenceRepository : LocalInferenceRepository {
  private val models = MutableStateFlow<List<ModelPackage>>(emptyList())
  private val readyModels = mutableSetOf<String>()

  override fun observeInstalledLocalModels(): Flow<List<ModelPackage>> = models

  override suspend fun getInstalledLocalModels(): List<ModelPackage> = models.value

  override suspend fun isModelReady(modelId: String): Boolean = readyModels.contains(modelId)

  fun setModels(newModels: List<ModelPackage>) {
    models.value = newModels
  }

  fun setReadyModels(ids: Set<String>) {
    readyModels.clear()
    readyModels.addAll(ids)
  }
}
