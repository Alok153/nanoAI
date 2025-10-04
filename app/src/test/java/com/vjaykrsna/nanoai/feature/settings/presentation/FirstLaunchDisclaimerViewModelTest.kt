package com.vjaykrsna.nanoai.feature.settings.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreference
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreferenceStore
import com.vjaykrsna.nanoai.core.data.preferences.RetentionPolicy
import com.vjaykrsna.nanoai.testing.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FirstLaunchDisclaimerViewModelTest {
  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var preferenceFlow: MutableStateFlow<PrivacyPreference>
  private lateinit var store: PrivacyPreferenceStore

  @Before
  fun setUp() {
    preferenceFlow =
      MutableStateFlow(
        PrivacyPreference(
          exportWarningsDismissed = false,
          telemetryOptIn = false,
          consentAcknowledgedAt = null,
          disclaimerShownCount = 0,
          retentionPolicy = RetentionPolicy.INDEFINITE,
        ),
      )

    store = mockk(relaxed = true) { every { privacyPreference } returns preferenceFlow }
  }

  @Test
  fun `uiState shows dialog when consent not acknowledged`() = runTest {
    val viewModel = FirstLaunchDisclaimerViewModel(store)

    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertThat(state.shouldShowDialog).isTrue()
    assertThat(state.disclaimerShownCount).isEqualTo(0)
  }

  @Test
  fun `onDismiss increments shown count but keeps dialog visible`() = runTest {
    val viewModel = FirstLaunchDisclaimerViewModel(store)

    viewModel.onDismiss()

    coVerify { store.incrementDisclaimerShown() }

    preferenceFlow.update { it.copy(disclaimerShownCount = 1) }
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertThat(state.shouldShowDialog).isTrue()
    assertThat(state.disclaimerShownCount).isEqualTo(1)
  }

  @Test
  fun `onAcknowledge records consent and hides dialog`() = runTest {
    val viewModel = FirstLaunchDisclaimerViewModel(store)
    val before = Clock.System.now()

    viewModel.onAcknowledge()

    coVerify { store.incrementDisclaimerShown() }
    coVerify { store.acknowledgeConsent(match { it >= before }) }

    val acknowledgedAt = Clock.System.now()
    preferenceFlow.update {
      it.copy(
        disclaimerShownCount = 1,
        consentAcknowledgedAt = acknowledgedAt,
      )
    }
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertThat(state.shouldShowDialog).isFalse()
  }
}
