package com.vjaykrsna.nanoai.feature.uiux.presentation

import com.vjaykrsna.nanoai.core.domain.model.uiux.ShellUiPreferences
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.core.domain.uiux.ThemeOperationsUseCase
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeCoordinatorTest {
  private val dispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension(dispatcher)

  @Test
  fun updateThemePreference_callsUseCase() =
    runTest(dispatcher) {
      val themeOperationsUseCase = mockk<ThemeOperationsUseCase>(relaxed = true)
      every { themeOperationsUseCase.observeUiPreferences() } returns
        MutableStateFlow(ShellUiPreferences())

      val coordinator = ThemeCoordinator(themeOperationsUseCase, dispatcher)

      coordinator.updateThemePreference(this, ThemePreference.DARK)
      advanceUntilIdle()

      coVerify(exactly = 1) { themeOperationsUseCase.updateTheme(ThemePreference.DARK) }
    }

  @Test
  fun updateVisualDensity_callsUseCase() =
    runTest(dispatcher) {
      val themeOperationsUseCase = mockk<ThemeOperationsUseCase>(relaxed = true)
      every { themeOperationsUseCase.observeUiPreferences() } returns
        MutableStateFlow(ShellUiPreferences())

      val coordinator = ThemeCoordinator(themeOperationsUseCase, dispatcher)

      coordinator.updateVisualDensity(this, VisualDensity.COMPACT)
      advanceUntilIdle()

      coVerify(exactly = 1) { themeOperationsUseCase.updateVisualDensity(VisualDensity.COMPACT) }
    }

  @Test
  fun updateHighContrastEnabled_callsUseCase() =
    runTest(dispatcher) {
      val themeOperationsUseCase = mockk<ThemeOperationsUseCase>(relaxed = true)
      every { themeOperationsUseCase.observeUiPreferences() } returns
        MutableStateFlow(ShellUiPreferences())

      val coordinator = ThemeCoordinator(themeOperationsUseCase, dispatcher)

      coordinator.updateHighContrastEnabled(this, true)
      advanceUntilIdle()

      coVerify(exactly = 1) { themeOperationsUseCase.updateHighContrastEnabled(true) }
    }

  @Test
  fun uiPreferences_returnsStateFlowFromUseCase() =
    runTest(dispatcher) {
      val expected =
        ShellUiPreferences(
          theme = ThemePreference.LIGHT,
          density = VisualDensity.COMPACT,
          highContrastEnabled = true,
        )
      val themeOperationsUseCase = mockk<ThemeOperationsUseCase>(relaxed = true)
      every { themeOperationsUseCase.observeUiPreferences() } returns MutableStateFlow(expected)

      val coordinator = ThemeCoordinator(themeOperationsUseCase, dispatcher)

      // Use backgroundScope so the stateIn collection is cancelled when test ends
      val stateFlow = coordinator.uiPreferences(backgroundScope)
      // Give time for the stateIn to collect the first value from the upstream MutableStateFlow
      advanceUntilIdle()
      testScheduler.advanceTimeBy(100)
      advanceUntilIdle()

      com.google.common.truth.Truth.assertThat(stateFlow.value).isEqualTo(expected)
    }
}
