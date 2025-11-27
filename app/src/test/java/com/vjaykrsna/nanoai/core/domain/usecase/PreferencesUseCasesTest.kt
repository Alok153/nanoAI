package com.vjaykrsna.nanoai.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreferenceStore
import com.vjaykrsna.nanoai.core.data.preferences.UiPreferences
import com.vjaykrsna.nanoai.core.data.preferences.UiPreferencesStore
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.core.domain.settings.model.DisclaimerExposureState
import com.vjaykrsna.nanoai.core.domain.settings.model.PrivacyPreference
import com.vjaykrsna.nanoai.core.domain.settings.model.RetentionPolicy
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class PreferencesUseCasesTest {

  @MockK lateinit var uiPreferencesStore: UiPreferencesStore
  @MockK lateinit var privacyPreferenceStore: PrivacyPreferenceStore

  private lateinit var updateUiPreferencesUseCase: UpdateUiPreferencesUseCase
  private lateinit var observeUiPreferencesUseCase: ObserveUiPreferencesUseCase
  private lateinit var observePrivacyPreferencesUseCase: ObservePrivacyPreferencesUseCase
  private lateinit var observeDisclaimerExposureUseCase: ObserveDisclaimerExposureUseCase
  private lateinit var updatePrivacyPreferencesUseCase: UpdatePrivacyPreferencesUseCase
  private lateinit var dispatcher: TestDispatcher

  @BeforeEach
  fun setUp() {
    dispatcher = UnconfinedTestDispatcher()
    updateUiPreferencesUseCase = UpdateUiPreferencesUseCase(uiPreferencesStore, dispatcher)
    observeUiPreferencesUseCase = ObserveUiPreferencesUseCase(uiPreferencesStore)
    observePrivacyPreferencesUseCase = ObservePrivacyPreferencesUseCase(privacyPreferenceStore)
    observeDisclaimerExposureUseCase = ObserveDisclaimerExposureUseCase(privacyPreferenceStore)
    updatePrivacyPreferencesUseCase = UpdatePrivacyPreferencesUseCase(privacyPreferenceStore)
  }

  @Test
  fun `observe preference use cases expose underlying flows`() {
    val uiFlow = flowOf(UiPreferences())
    val privacyFlow =
      flowOf(
        PrivacyPreference(
          exportWarningsDismissed = true,
          telemetryOptIn = true,
          consentAcknowledgedAt = null,
          disclaimerShownCount = 0,
          retentionPolicy = RetentionPolicy.INDEFINITE,
        )
      )
    val disclaimerFlow =
      flowOf(
        DisclaimerExposureState(
          shouldShowDialog = false,
          acknowledged = true,
          acknowledgedAt = Instant.fromEpochMilliseconds(0),
          shownCount = 1,
        )
      )

    every { uiPreferencesStore.uiPreferences } returns uiFlow
    every { privacyPreferenceStore.privacyPreference } returns privacyFlow
    every { privacyPreferenceStore.disclaimerExposure } returns disclaimerFlow

    assertThat(observeUiPreferencesUseCase()).isEqualTo(uiFlow)
    assertThat(observePrivacyPreferencesUseCase()).isEqualTo(privacyFlow)
    assertThat(observeDisclaimerExposureUseCase()).isEqualTo(disclaimerFlow)
  }

  @Test
  fun `updateUiPreferences delegates to store setters`() =
    runTest(dispatcher) {
      coJustRun { uiPreferencesStore.setThemePreference(any()) }
      coJustRun { uiPreferencesStore.setVisualDensity(any()) }
      coJustRun { uiPreferencesStore.setPinnedToolIds(any()) }
      coJustRun { uiPreferencesStore.addPinnedTool(any()) }
      coJustRun { uiPreferencesStore.removePinnedTool(any()) }
      coJustRun { uiPreferencesStore.reorderPinnedTools(any()) }
      coJustRun { uiPreferencesStore.setCommandPaletteRecents(any()) }
      coJustRun { uiPreferencesStore.recordCommandPaletteRecent(any()) }
      coJustRun { uiPreferencesStore.setConnectivityBannerDismissed(any()) }

      updateUiPreferencesUseCase.setThemePreference(ThemePreference.DARK)
      updateUiPreferencesUseCase.setVisualDensity(VisualDensity.COMPACT)
      updateUiPreferencesUseCase.setPinnedToolIds(listOf("a", "b"))
      updateUiPreferencesUseCase.addPinnedTool("c")
      updateUiPreferencesUseCase.removePinnedTool("a")
      updateUiPreferencesUseCase.reorderPinnedTools(listOf("b", "c"))
      updateUiPreferencesUseCase.setCommandPaletteRecents(listOf("cmd"))
      updateUiPreferencesUseCase.recordCommandPaletteRecent("cmd-2")
      updateUiPreferencesUseCase.setConnectivityBannerDismissed(null)

      coVerify { uiPreferencesStore.setThemePreference(ThemePreference.DARK) }
      coVerify { uiPreferencesStore.setVisualDensity(VisualDensity.COMPACT) }
      coVerify { uiPreferencesStore.setPinnedToolIds(listOf("a", "b")) }
      coVerify { uiPreferencesStore.addPinnedTool("c") }
      coVerify { uiPreferencesStore.removePinnedTool("a") }
      coVerify { uiPreferencesStore.reorderPinnedTools(listOf("b", "c")) }
      coVerify { uiPreferencesStore.setCommandPaletteRecents(listOf("cmd")) }
      coVerify { uiPreferencesStore.recordCommandPaletteRecent("cmd-2") }
      coVerify { uiPreferencesStore.setConnectivityBannerDismissed(null) }
    }

  @Test
  fun `setHighContrastEnabled reports success on completion`() =
    runTest(dispatcher) {
      coJustRun { uiPreferencesStore.setHighContrastEnabled(true) }

      val result = updateUiPreferencesUseCase.setHighContrastEnabled(true)

      assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
      coVerify { uiPreferencesStore.setHighContrastEnabled(true) }
    }

  @Test
  fun `setHighContrastEnabled surfaces recoverable errors`() =
    runTest(dispatcher) {
      coEvery { uiPreferencesStore.setHighContrastEnabled(true) } throws
        IllegalStateException("boom")

      val result = updateUiPreferencesUseCase.setHighContrastEnabled(true)

      assertThat(result).isInstanceOf(NanoAIResult.RecoverableError::class.java)
    }

  @Test
  fun `updatePrivacyPreferences delegates to store`() = runTest {
    coJustRun { privacyPreferenceStore.setTelemetryOptIn(any()) }
    coJustRun { privacyPreferenceStore.acknowledgeConsent(any()) }
    coJustRun { privacyPreferenceStore.setRetentionPolicy(any()) }
    coJustRun { privacyPreferenceStore.setExportWarningsDismissed(any()) }
    coJustRun { privacyPreferenceStore.incrementDisclaimerShown() }

    val timestamp = Instant.fromEpochMilliseconds(1_700_000_000_000)

    updatePrivacyPreferencesUseCase.setTelemetryOptIn(true)
    updatePrivacyPreferencesUseCase.acknowledgeConsent(timestamp)
    updatePrivacyPreferencesUseCase.setRetentionPolicy(RetentionPolicy.MANUAL_PURGE_ONLY)
    updatePrivacyPreferencesUseCase.setExportWarningsDismissed(true)
    updatePrivacyPreferencesUseCase.incrementDisclaimerShown()

    coVerify { privacyPreferenceStore.setTelemetryOptIn(true) }
    coVerify { privacyPreferenceStore.acknowledgeConsent(timestamp) }
    coVerify { privacyPreferenceStore.setRetentionPolicy(RetentionPolicy.MANUAL_PURGE_ONLY) }
    coVerify { privacyPreferenceStore.setExportWarningsDismissed(true) }
    coVerify { privacyPreferenceStore.incrementDisclaimerShown() }
  }
}
