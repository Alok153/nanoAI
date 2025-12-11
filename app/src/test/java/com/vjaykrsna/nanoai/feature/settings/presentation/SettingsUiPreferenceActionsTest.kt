package com.vjaykrsna.nanoai.feature.settings.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.error.NanoAIErrorEnvelope
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.uiux.SettingsOperationsUseCase
import com.vjaykrsna.nanoai.core.domain.uiux.ToggleCompactModeUseCase
import com.vjaykrsna.nanoai.core.domain.usecase.UpdateUiPreferencesUseCase
import com.vjaykrsna.nanoai.feature.settings.presentation.state.SettingsUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsUiPreferenceActionsTest {

  private val testDispatcher = StandardTestDispatcher()
  private val testScope = TestScope(testDispatcher)
  private lateinit var settingsOperationsUseCase: SettingsOperationsUseCase
  private lateinit var toggleCompactModeUseCase: ToggleCompactModeUseCase
  private lateinit var updateUiPreferencesUseCase: UpdateUiPreferencesUseCase
  private lateinit var state: MutableStateFlow<SettingsUiState>
  private lateinit var errors: MutableList<NanoAIErrorEnvelope>
  private lateinit var actions: SettingsUiPreferenceActions

  @BeforeEach
  fun setUp() {
    settingsOperationsUseCase = mockk(relaxed = true)
    toggleCompactModeUseCase = mockk(relaxed = true)
    updateUiPreferencesUseCase = mockk(relaxed = true)
    state = MutableStateFlow(SettingsUiState())
    errors = mutableListOf()

    actions =
      SettingsUiPreferenceActions(
        scope = testScope,
        state = state,
        updateState = { transform -> state.value = state.value.transform() },
        dependencies =
          SettingsUiPreferenceDependencies(
            settingsOperationsUseCase = settingsOperationsUseCase,
            toggleCompactModeUseCase = toggleCompactModeUseCase,
            updateUiPreferencesUseCase = updateUiPreferencesUseCase,
            emitError = { error -> errors.add(error) },
          ),
      )
  }

  @Test
  fun `setThemePreference updates state and calls use case`() =
    testScope.runTest {
      coEvery { settingsOperationsUseCase.updateTheme(ThemePreference.DARK) } returns
        NanoAIResult.success(Unit)

      actions.setThemePreference(ThemePreference.DARK)
      advanceUntilIdle()

      assertThat(state.value.themePreference).isEqualTo(ThemePreference.DARK)
      assertThat(state.value.undoAvailable).isTrue()
      coVerify { settingsOperationsUseCase.updateTheme(ThemePreference.DARK) }
    }

  @Test
  fun `setThemePreference does nothing when same theme`() =
    testScope.runTest {
      state.value = state.value.copy(themePreference = ThemePreference.LIGHT)

      actions.setThemePreference(ThemePreference.LIGHT)
      advanceUntilIdle()

      coVerify(exactly = 0) { settingsOperationsUseCase.updateTheme(any()) }
    }

  @Test
  fun `setThemePreference emits error on failure`() =
    testScope.runTest {
      coEvery { settingsOperationsUseCase.updateTheme(any()) } returns
        NanoAIResult.recoverable(message = "Failed")

      actions.setThemePreference(ThemePreference.DARK)
      advanceUntilIdle()

      assertThat(errors).hasSize(1)
    }

  @Test
  fun `setCompactMode updates state and calls use case`() =
    testScope.runTest {
      coEvery { toggleCompactModeUseCase.setCompactMode(true) } returns NanoAIResult.success(Unit)

      actions.setCompactMode(true)
      advanceUntilIdle()

      assertThat(state.value.compactModeEnabled).isTrue()
      assertThat(state.value.undoAvailable).isTrue()
      coVerify { toggleCompactModeUseCase.setCompactMode(true) }
    }

  @Test
  fun `setCompactMode does nothing when same value`() =
    testScope.runTest {
      state.value = state.value.copy(compactModeEnabled = true)

      actions.setCompactMode(true)
      advanceUntilIdle()

      coVerify(exactly = 0) { toggleCompactModeUseCase.setCompactMode(any()) }
    }

  @Test
  fun `setHighContrastEnabled updates state and calls use case`() =
    testScope.runTest {
      coEvery { updateUiPreferencesUseCase.setHighContrastEnabled(true) } returns
        NanoAIResult.success(Unit)

      actions.setHighContrastEnabled(true)
      advanceUntilIdle()

      assertThat(state.value.highContrastEnabled).isTrue()
      assertThat(state.value.undoAvailable).isTrue()
      coVerify { updateUiPreferencesUseCase.setHighContrastEnabled(true) }
    }

  @Test
  fun `setHighContrastEnabled does nothing when same value`() =
    testScope.runTest {
      state.value = state.value.copy(highContrastEnabled = true)

      actions.setHighContrastEnabled(true)
      advanceUntilIdle()

      coVerify(exactly = 0) { updateUiPreferencesUseCase.setHighContrastEnabled(any()) }
    }

  @Test
  fun `undoUiPreferenceChange restores previous state`() =
    testScope.runTest {
      val originalState =
        SettingsUiState(
          themePreference = ThemePreference.LIGHT,
          compactModeEnabled = false,
          highContrastEnabled = false,
        )
      state.value = originalState

      coEvery { settingsOperationsUseCase.updateTheme(any()) } returns NanoAIResult.success(Unit)
      coEvery { toggleCompactModeUseCase.setCompactMode(any()) } returns NanoAIResult.success(Unit)
      coEvery { updateUiPreferencesUseCase.setHighContrastEnabled(any()) } returns
        NanoAIResult.success(Unit)

      actions.setThemePreference(ThemePreference.DARK)
      advanceUntilIdle()

      assertThat(state.value.themePreference).isEqualTo(ThemePreference.DARK)

      actions.undoUiPreferenceChange()
      advanceUntilIdle()

      assertThat(state.value.themePreference).isEqualTo(ThemePreference.LIGHT)
      assertThat(state.value.undoAvailable).isFalse()
    }

  @Test
  fun `undoUiPreferenceChange does nothing when no snapshot`() =
    testScope.runTest {
      val originalState = state.value.copy()

      actions.undoUiPreferenceChange()
      advanceUntilIdle()

      assertThat(state.value).isEqualTo(originalState)
    }

  @Test
  fun `applyDensityPreference calls setCompactMode`() =
    testScope.runTest {
      coEvery { toggleCompactModeUseCase.setCompactMode(true) } returns NanoAIResult.success(Unit)

      actions.applyDensityPreference(true)
      advanceUntilIdle()

      assertThat(state.value.compactModeEnabled).isTrue()
    }
}
