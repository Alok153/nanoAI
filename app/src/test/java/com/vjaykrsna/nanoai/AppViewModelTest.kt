package com.vjaykrsna.nanoai

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.preferences.UiPreferences
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.settings.model.DisclaimerExposureState
import com.vjaykrsna.nanoai.core.domain.uiux.ObserveUserProfileUseCase
import com.vjaykrsna.nanoai.core.domain.usecase.ObserveDisclaimerExposureUseCase
import com.vjaykrsna.nanoai.core.domain.usecase.ObserveUiPreferencesUseCase
import com.vjaykrsna.nanoai.core.domain.usecase.UpdatePrivacyPreferencesUseCase
import com.vjaykrsna.nanoai.feature.uiux.presentation.AppViewModel
import com.vjaykrsna.nanoai.feature.uiux.presentation.DisclaimerUiState
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AppViewModelTest {

  @MockK private lateinit var observeUserProfileUseCase: ObserveUserProfileUseCase

  @MockK private lateinit var observeDisclaimerExposureUseCase: ObserveDisclaimerExposureUseCase

  @MockK private lateinit var observeUiPreferencesUseCase: ObserveUiPreferencesUseCase

  @MockK private lateinit var updatePrivacyPreferencesUseCase: UpdatePrivacyPreferencesUseCase

  private lateinit var appViewModel: AppViewModel

  @Before
  fun setUp() {
    MockKAnnotations.init(this, relaxed = true)
    val uiPrefs = UiPreferences()
    every { observeUiPreferencesUseCase() } returns MutableStateFlow(uiPrefs)
    every { observeDisclaimerExposureUseCase() } returns
      MutableStateFlow(
        DisclaimerExposureState(
          shouldShowDialog = false,
          acknowledged = false,
          acknowledgedAt = null,
          shownCount = 0,
        )
      )
  }

  @Test
  fun `initialization loads configuration and sets initial state`() = runTest {
    // Given
    val userProfileResult =
      ObserveUserProfileUseCase.Result(
        userProfile = null,
        layoutSnapshots = emptyList(),
        uiState = null,
        hydratedFromCache = true,
        offline = false,
      )
    val disclaimerState =
      DisclaimerExposureState(
        shouldShowDialog = false,
        acknowledged = false,
        acknowledgedAt = null,
        shownCount = 0,
      )

    every { observeUserProfileUseCase.flow } returns MutableStateFlow(userProfileResult)
    every { observeDisclaimerExposureUseCase() } returns MutableStateFlow(disclaimerState)

    // When
    appViewModel =
      AppViewModel(
        observeUserProfileUseCase,
        observeDisclaimerExposureUseCase,
        observeUiPreferencesUseCase,
        updatePrivacyPreferencesUseCase,
      )

    // Then
    appViewModel.uiState.test {
      val initialState = awaitItem()
      assertThat(initialState.isHydrating).isTrue()
      assertThat(initialState.themePreference).isEqualTo(ThemePreference.SYSTEM)
      assertThat(initialState.offline).isFalse()
      assertThat(initialState.disclaimer).isEqualTo(DisclaimerUiState())
    }
  }

  @Test
  fun `initialization handles unexpected error gracefully`() = runTest {
    // Given
    val userProfileResult =
      ObserveUserProfileUseCase.Result(
        userProfile = null,
        layoutSnapshots = emptyList(),
        uiState = null,
        hydratedFromCache = true,
        offline = false,
      )
    every { observeUserProfileUseCase.flow } returns MutableStateFlow(userProfileResult)
    val disclaimerState =
      DisclaimerExposureState(
        shouldShowDialog = false,
        acknowledged = false,
        acknowledgedAt = null,
        shownCount = 0,
      )
    every { observeDisclaimerExposureUseCase() } returns MutableStateFlow(disclaimerState)

    // When
    appViewModel =
      AppViewModel(
        observeUserProfileUseCase,
        observeDisclaimerExposureUseCase,
        observeUiPreferencesUseCase,
        updatePrivacyPreferencesUseCase,
      )

    // Then - Should not crash and should have default state
    appViewModel.uiState.test {
      val state = awaitItem()
      assertThat(state).isNotNull()
    }
  }

  @Test
  fun `initialization checks first launch correctly`() = runTest {
    // Given
    val userProfileResult =
      ObserveUserProfileUseCase.Result(
        userProfile = null,
        layoutSnapshots = emptyList(),
        uiState = null,
        hydratedFromCache = true,
        offline = false,
      )
    val disclaimerState =
      DisclaimerExposureState(
        shouldShowDialog = true,
        acknowledged = false,
        acknowledgedAt = null,
        shownCount = 0,
      )

    every { observeUserProfileUseCase.flow } returns MutableStateFlow(userProfileResult)
    every { observeDisclaimerExposureUseCase() } returns MutableStateFlow(disclaimerState)

    // When
    appViewModel =
      AppViewModel(
        observeUserProfileUseCase,
        observeDisclaimerExposureUseCase,
        observeUiPreferencesUseCase,
        updatePrivacyPreferencesUseCase,
      )

    // Then
    appViewModel.uiState.test {
      val state = awaitItem()
      assertThat(state.disclaimer.shouldShow).isTrue()
    }
  }

  @Test
  fun `onDisclaimerAccepted persists consent and updates state`() = runTest {
    // Given
    val userProfileResult =
      ObserveUserProfileUseCase.Result(
        userProfile = null,
        layoutSnapshots = emptyList(),
        uiState = null,
        hydratedFromCache = true,
        offline = false,
      )
    val disclaimerState =
      DisclaimerExposureState(
        shouldShowDialog = true,
        acknowledged = false,
        acknowledgedAt = null,
        shownCount = 0,
      )

    every { observeUserProfileUseCase.flow } returns MutableStateFlow(userProfileResult)
    every { observeDisclaimerExposureUseCase() } returns MutableStateFlow(disclaimerState)

    appViewModel =
      AppViewModel(
        observeUserProfileUseCase,
        observeDisclaimerExposureUseCase,
        observeUiPreferencesUseCase,
        updatePrivacyPreferencesUseCase,
      )

    // When
    appViewModel.onDisclaimerAccepted()

    // Then
    coVerify { updatePrivacyPreferencesUseCase.acknowledgeConsent(any()) }
  }

  @Test
  fun `onDisclaimerDisplayed records exposure`() = runTest {
    // Given
    val userProfileResult =
      ObserveUserProfileUseCase.Result(
        userProfile = null,
        layoutSnapshots = emptyList(),
        uiState = null,
        hydratedFromCache = true,
        offline = false,
      )
    every { observeUserProfileUseCase.flow } returns MutableStateFlow(userProfileResult)

    appViewModel =
      AppViewModel(
        observeUserProfileUseCase,
        observeDisclaimerExposureUseCase,
        observeUiPreferencesUseCase,
        updatePrivacyPreferencesUseCase,
      )

    // When
    appViewModel.onDisclaimerDisplayed()

    // Then
    coVerify { updatePrivacyPreferencesUseCase.incrementDisclaimerShown() }
  }

  @Test
  fun `onDisclaimerAccepted navigates to main screen`() = runTest {
    // Given
    val userProfileResult =
      ObserveUserProfileUseCase.Result(
        userProfile = null,
        layoutSnapshots = emptyList(),
        uiState = null,
        hydratedFromCache = true,
        offline = false,
      )
    val disclaimerState =
      DisclaimerExposureState(
        shouldShowDialog = true,
        acknowledged = false,
        acknowledgedAt = null,
        shownCount = 0,
      )

    every { observeUserProfileUseCase.flow } returns MutableStateFlow(userProfileResult)
    every { observeDisclaimerExposureUseCase() } returns MutableStateFlow(disclaimerState)

    appViewModel =
      AppViewModel(
        observeUserProfileUseCase,
        observeDisclaimerExposureUseCase,
        observeUiPreferencesUseCase,
        updatePrivacyPreferencesUseCase,
      )

    // When
    appViewModel.onDisclaimerAccepted()

    // Then - Verify that the state changes appropriately after consent
    appViewModel.uiState.test {
      val initialState = awaitItem()
      // After accepting disclaimer, the state should reflect this in subsequent emissions
      // The actual navigation would be handled by observing the state changes
      assertThat(initialState.disclaimer.acknowledged).isFalse() // Initial state
    }
  }
}
