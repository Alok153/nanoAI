package com.vjaykrsna.nanoai.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.model.uiux.DataStoreUiPreferences
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.core.domain.repository.PrivacyPreferenceRepository
import com.vjaykrsna.nanoai.core.domain.repository.UiPreferencesRepository
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

  @MockK lateinit var uiPreferencesRepository: UiPreferencesRepository
  @MockK lateinit var privacyPreferenceRepository: PrivacyPreferenceRepository

  private lateinit var updateUiPreferencesUseCase: UpdateUiPreferencesUseCase
  private lateinit var observeUiPreferencesUseCase: ObserveUiPreferencesUseCase
  private lateinit var observePrivacyPreferencesUseCase: ObservePrivacyPreferencesUseCase
  private lateinit var observeDisclaimerExposureUseCase: ObserveDisclaimerExposureUseCase
  private lateinit var updatePrivacyPreferencesUseCase: UpdatePrivacyPreferencesUseCase
  private lateinit var dispatcher: TestDispatcher

  @BeforeEach
  fun setUp() {
    dispatcher = UnconfinedTestDispatcher()
    updateUiPreferencesUseCase = UpdateUiPreferencesUseCase(uiPreferencesRepository, dispatcher)
    observeUiPreferencesUseCase = ObserveUiPreferencesUseCase(uiPreferencesRepository)
    observePrivacyPreferencesUseCase = ObservePrivacyPreferencesUseCase(privacyPreferenceRepository)
    observeDisclaimerExposureUseCase = ObserveDisclaimerExposureUseCase(privacyPreferenceRepository)
    updatePrivacyPreferencesUseCase = UpdatePrivacyPreferencesUseCase(privacyPreferenceRepository)
  }

  @Test
  fun `observe preference use cases expose underlying flows`() {
    val uiFlow = flowOf(DataStoreUiPreferences())
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

    every { uiPreferencesRepository.preferences } returns uiFlow
    every { privacyPreferenceRepository.privacyPreference } returns privacyFlow
    every { privacyPreferenceRepository.disclaimerExposure } returns disclaimerFlow

    assertThat(observeUiPreferencesUseCase()).isEqualTo(uiFlow)
    assertThat(observePrivacyPreferencesUseCase()).isEqualTo(privacyFlow)
    assertThat(observeDisclaimerExposureUseCase()).isEqualTo(disclaimerFlow)
  }

  @Test
  fun `updateUiPreferences delegates to store setters`() =
    runTest(dispatcher) {
      coJustRun { uiPreferencesRepository.setThemePreference(any()) }
      coJustRun { uiPreferencesRepository.setVisualDensity(any()) }
      coJustRun { uiPreferencesRepository.setPinnedToolIds(any()) }
      coJustRun { uiPreferencesRepository.addPinnedTool(any()) }
      coJustRun { uiPreferencesRepository.removePinnedTool(any()) }
      coJustRun { uiPreferencesRepository.reorderPinnedTools(any()) }
      coJustRun { uiPreferencesRepository.setCommandPaletteRecents(any()) }
      coJustRun { uiPreferencesRepository.recordCommandPaletteRecent(any()) }
      coJustRun { uiPreferencesRepository.setConnectivityBannerDismissed(any()) }

      updateUiPreferencesUseCase.setThemePreference(ThemePreference.DARK)
      updateUiPreferencesUseCase.setVisualDensity(VisualDensity.COMPACT)
      updateUiPreferencesUseCase.setPinnedToolIds(listOf("a", "b"))
      updateUiPreferencesUseCase.addPinnedTool("c")
      updateUiPreferencesUseCase.removePinnedTool("a")
      updateUiPreferencesUseCase.reorderPinnedTools(listOf("b", "c"))
      updateUiPreferencesUseCase.setCommandPaletteRecents(listOf("cmd"))
      updateUiPreferencesUseCase.recordCommandPaletteRecent("cmd-2")
      updateUiPreferencesUseCase.setConnectivityBannerDismissed(null)

      coVerify { uiPreferencesRepository.setThemePreference(ThemePreference.DARK) }
      coVerify { uiPreferencesRepository.setVisualDensity(VisualDensity.COMPACT) }
      coVerify { uiPreferencesRepository.setPinnedToolIds(listOf("a", "b")) }
      coVerify { uiPreferencesRepository.addPinnedTool("c") }
      coVerify { uiPreferencesRepository.removePinnedTool("a") }
      coVerify { uiPreferencesRepository.reorderPinnedTools(listOf("b", "c")) }
      coVerify { uiPreferencesRepository.setCommandPaletteRecents(listOf("cmd")) }
      coVerify { uiPreferencesRepository.recordCommandPaletteRecent("cmd-2") }
      coVerify { uiPreferencesRepository.setConnectivityBannerDismissed(null) }
    }

  @Test
  fun `setHighContrastEnabled reports success on completion`() =
    runTest(dispatcher) {
      coJustRun { uiPreferencesRepository.setHighContrastEnabled(true) }

      val result = updateUiPreferencesUseCase.setHighContrastEnabled(true)

      assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
      coVerify { uiPreferencesRepository.setHighContrastEnabled(true) }
    }

  @Test
  fun `setHighContrastEnabled surfaces recoverable errors`() =
    runTest(dispatcher) {
      coEvery { uiPreferencesRepository.setHighContrastEnabled(true) } throws
        IllegalStateException("boom")

      val result = updateUiPreferencesUseCase.setHighContrastEnabled(true)

      assertThat(result).isInstanceOf(NanoAIResult.RecoverableError::class.java)
    }

  @Test
  fun `updatePrivacyPreferences delegates to store`() = runTest {
    coJustRun { privacyPreferenceRepository.setTelemetryOptIn(any()) }
    coJustRun { privacyPreferenceRepository.acknowledgeConsent(any()) }
    coJustRun { privacyPreferenceRepository.setRetentionPolicy(any()) }
    coJustRun { privacyPreferenceRepository.setExportWarningsDismissed(any()) }
    coJustRun { privacyPreferenceRepository.incrementDisclaimerShown() }

    val timestamp = Instant.fromEpochMilliseconds(1_700_000_000_000)

    updatePrivacyPreferencesUseCase.setTelemetryOptIn(true)
    updatePrivacyPreferencesUseCase.acknowledgeConsent(timestamp)
    updatePrivacyPreferencesUseCase.setRetentionPolicy(RetentionPolicy.MANUAL_PURGE_ONLY)
    updatePrivacyPreferencesUseCase.setExportWarningsDismissed(true)
    updatePrivacyPreferencesUseCase.incrementDisclaimerShown()

    coVerify { privacyPreferenceRepository.setTelemetryOptIn(true) }
    coVerify { privacyPreferenceRepository.acknowledgeConsent(timestamp) }
    coVerify { privacyPreferenceRepository.setRetentionPolicy(RetentionPolicy.MANUAL_PURGE_ONLY) }
    coVerify { privacyPreferenceRepository.setExportWarningsDismissed(true) }
    coVerify { privacyPreferenceRepository.incrementDisclaimerShown() }
  }
}
