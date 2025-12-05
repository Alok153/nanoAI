package com.vjaykrsna.nanoai.feature.chat.domain

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.chat.ConversationUseCase
import com.vjaykrsna.nanoai.core.domain.chat.SendPromptUseCase
import com.vjaykrsna.nanoai.core.domain.chat.SwitchPersonaUseCase
import com.vjaykrsna.nanoai.core.domain.library.ModelCatalogUseCase
import com.vjaykrsna.nanoai.core.domain.library.toModel
import com.vjaykrsna.nanoai.core.domain.usecase.GetDefaultPersonaUseCase
import com.vjaykrsna.nanoai.core.domain.usecase.ObservePersonasUseCase
import com.vjaykrsna.nanoai.testing.DomainTestBuilders
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DefaultChatFeatureCoordinatorTest {

  private lateinit var sendPromptUseCase: SendPromptUseCase
  private lateinit var switchPersonaUseCase: SwitchPersonaUseCase
  private lateinit var conversationUseCase: ConversationUseCase
  private lateinit var observePersonasUseCase: ObservePersonasUseCase
  private lateinit var getDefaultPersonaUseCase: GetDefaultPersonaUseCase
  private lateinit var modelCatalogUseCase: ModelCatalogUseCase
  private lateinit var coordinator: DefaultChatFeatureCoordinator

  @BeforeEach
  fun setup() {
    sendPromptUseCase = mockk(relaxed = true)
    switchPersonaUseCase = mockk(relaxed = true)
    conversationUseCase = mockk(relaxed = true)
    observePersonasUseCase = mockk(relaxed = true)
    getDefaultPersonaUseCase = mockk(relaxed = true)
    modelCatalogUseCase = mockk(relaxed = true)
    every { modelCatalogUseCase.observeInstalledModels() } returns flowOf(emptyList())

    coordinator =
      DefaultChatFeatureCoordinator(
        sendPromptUseCase,
        switchPersonaUseCase,
        conversationUseCase,
        observePersonasUseCase,
        getDefaultPersonaUseCase,
        modelCatalogUseCase,
      )
  }

  @Test
  fun observeInstalledModels_mapsPackagesToUiModels() = runTest {
    val packageDto = DomainTestBuilders.buildModelPackage(modelId = "model-123")
    every { modelCatalogUseCase.observeInstalledModels() } returns flowOf(listOf(packageDto))

    val models = coordinator.observeInstalledModels().first()

    assertThat(models).containsExactly(packageDto.toModel())
  }
}
