package com.vjaykrsna.nanoai.feature.settings.presentation

import android.net.Uri
import app.cash.turbine.TurbineTestContext
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.library.ModelDownloadsAndExportUseCase
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.domain.model.uiux.DataStoreUiPreferences
import com.vjaykrsna.nanoai.core.domain.model.uiux.ScreenType
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.UserProfile
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.core.domain.settings.ApiProviderConfigUseCase
import com.vjaykrsna.nanoai.core.domain.settings.BackupLocation
import com.vjaykrsna.nanoai.core.domain.settings.ImportService
import com.vjaykrsna.nanoai.core.domain.settings.ImportSummary
import com.vjaykrsna.nanoai.core.domain.settings.huggingface.HuggingFaceAuthCoordinator
import com.vjaykrsna.nanoai.core.domain.settings.huggingface.HuggingFaceAuthState
import com.vjaykrsna.nanoai.core.domain.settings.huggingface.HuggingFaceDeviceAuthState
import com.vjaykrsna.nanoai.core.domain.settings.huggingface.HuggingFaceOAuthConfig
import com.vjaykrsna.nanoai.core.domain.settings.model.PrivacyPreference
import com.vjaykrsna.nanoai.core.domain.uiux.ObserveUserProfileUseCase
import com.vjaykrsna.nanoai.core.domain.uiux.SettingsOperationsUseCase
import com.vjaykrsna.nanoai.core.domain.uiux.ToggleCompactModeUseCase
import com.vjaykrsna.nanoai.core.domain.usecase.ObservePrivacyPreferencesUseCase
import com.vjaykrsna.nanoai.core.domain.usecase.ObserveUiPreferencesUseCase
import com.vjaykrsna.nanoai.core.domain.usecase.UpdatePrivacyPreferencesUseCase
import com.vjaykrsna.nanoai.core.domain.usecase.UpdateUiPreferencesUseCase
import com.vjaykrsna.nanoai.feature.settings.presentation.model.SettingsUiEvent
import com.vjaykrsna.nanoai.feature.settings.presentation.state.SettingsUiState
import com.vjaykrsna.nanoai.shared.state.ViewModelStateHostTestHarness
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

  @JvmField
  @RegisterExtension
  val dispatcherExtension: MainDispatcherExtension = MainDispatcherExtension()

  private val dispatcher: TestDispatcher = dispatcherExtension.dispatcher

  private lateinit var apiProviderConfigUseCase: ApiProviderConfigUseCase
  private lateinit var modelDownloadsAndExportUseCase: ModelDownloadsAndExportUseCase
  private lateinit var observePrivacyPreferencesUseCase: ObservePrivacyPreferencesUseCase
  private lateinit var observeUiPreferencesUseCase: ObserveUiPreferencesUseCase
  private lateinit var updatePrivacyPreferencesUseCase: UpdatePrivacyPreferencesUseCase
  private lateinit var updateUiPreferencesUseCase: UpdateUiPreferencesUseCase
  private lateinit var importService: ImportService
  private lateinit var observeUserProfileUseCase: ObserveUserProfileUseCase
  private lateinit var settingsOperationsUseCase: SettingsOperationsUseCase
  private lateinit var toggleCompactModeUseCase: ToggleCompactModeUseCase
  private lateinit var huggingFaceAuthCoordinator: HuggingFaceAuthCoordinator
  private lateinit var huggingFaceOAuthConfig: HuggingFaceOAuthConfig

  private lateinit var providersFlow: MutableStateFlow<List<APIProviderConfig>>
  private lateinit var privacyFlow: MutableStateFlow<PrivacyPreference>
  private lateinit var uiPrefsFlow: MutableStateFlow<DataStoreUiPreferences>
  private lateinit var profileFlow: MutableStateFlow<ObserveUserProfileUseCase.Result>
  private lateinit var authStateFlow: MutableStateFlow<HuggingFaceAuthState>
  private lateinit var deviceAuthFlow: MutableStateFlow<HuggingFaceDeviceAuthState?>

  private lateinit var viewModel: SettingsViewModel
  private lateinit var harness: ViewModelStateHostTestHarness<SettingsUiState, SettingsUiEvent>

  @BeforeEach
  fun setUp() {
    initialiseFlows()
    initialiseMocks()
    buildHarness()
  }

  @Test
  fun setThemePreferenceUpdatesStateAndCallsUseCase() =
    runTest(dispatcher) {
      coEvery { settingsOperationsUseCase.updateTheme(ThemePreference.DARK) } returns
        NanoAIResult.success(Unit)

      viewModel.setThemePreference(ThemePreference.DARK)
      advanceUntilIdle()

      val state = harness.awaitState(predicate = { it.themePreference == ThemePreference.DARK })
      assertThat(state.undoAvailable).isTrue()
      assertThat(state.statusMessage).isEqualTo("Theme updated")

      coVerify { settingsOperationsUseCase.updateTheme(ThemePreference.DARK) }
    }

  @Test
  fun setHighContrastEnabledPersistsPreference() =
    runTest(dispatcher) {
      coEvery { updateUiPreferencesUseCase.setHighContrastEnabled(true) } returns
        NanoAIResult.success(Unit)

      viewModel.setHighContrastEnabled(true)
      advanceUntilIdle()

      val state = harness.awaitState(predicate = { it.highContrastEnabled })
      assertThat(state.highContrastEnabled).isTrue()
      assertThat(state.statusMessage).isEqualTo("High contrast enabled")

      coVerify { updateUiPreferencesUseCase.setHighContrastEnabled(true) }
    }

  @Test
  fun undoUiPreferenceChangeRestoresSnapshot() =
    runTest(dispatcher) {
      coEvery { settingsOperationsUseCase.updateTheme(any()) } returns NanoAIResult.success(Unit)
      coEvery { toggleCompactModeUseCase.setCompactMode(any()) } returns NanoAIResult.success(Unit)
      coEvery { updateUiPreferencesUseCase.setHighContrastEnabled(any()) } returns
        NanoAIResult.success(Unit)

      viewModel.setThemePreference(ThemePreference.DARK)
      viewModel.setCompactMode(true)
      viewModel.setHighContrastEnabled(true)
      advanceUntilIdle()

      viewModel.undoUiPreferenceChange()
      advanceUntilIdle()

      val state = harness.awaitState(predicate = { !it.undoAvailable })
      assertThat(state.themePreference).isEqualTo(ThemePreference.SYSTEM)
      assertThat(state.compactModeEnabled).isFalse()
      assertThat(state.highContrastEnabled).isFalse()
      assertThat(state.statusMessage).isEqualTo("Preferences restored")
    }

  @Test
  fun setTelemetryOptInFailureEmitsErrorEvent() =
    runTest(dispatcher) {
      coEvery { updatePrivacyPreferencesUseCase.setTelemetryOptIn(any()) } throws
        IllegalStateException("Store error")

      harness.testEvents {
        viewModel.setTelemetryOptIn(true)
        advanceUntilIdle()

        val event = awaitItem() as SettingsUiEvent.ErrorRaised
        assertThat(event.envelope.userMessage).isEqualTo("Store error")
        val state =
          harness.awaitState(predicate = { it.lastErrorMessage == event.envelope.userMessage })
        assertThat(state.lastErrorMessage).isEqualTo("Store error")
      }
    }

  @Test
  fun exportBackupSuccessEmitsEvent() =
    runTest(dispatcher) {
      coEvery { modelDownloadsAndExportUseCase.exportBackup(any(), any()) } returns
        NanoAIResult.success("/tmp/backup.json")

      harness.testEvents {
        viewModel.exportBackup("/tmp/backup.json")
        advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(SettingsUiEvent.ExportCompleted("/tmp/backup.json"))
      }
    }

  @Test
  fun exportBackupFailureEmitsErrorEvent() =
    runTest(dispatcher) {
      coEvery { modelDownloadsAndExportUseCase.exportBackup(any(), any()) } returns
        NanoAIResult.recoverable("disk full")

      harness.testEvents {
        viewModel.exportBackup("/tmp/backup.json")
        advanceUntilIdle()

        val event = awaitItem() as SettingsUiEvent.ErrorRaised
        assertThat(event.envelope.userMessage).isEqualTo("disk full")
      }
    }

  @Test
  fun importBackupSuccessEmitsEvent() =
    runTest(dispatcher) {
      val summary =
        ImportSummary(
          personasImported = 1,
          personasUpdated = 0,
          providersImported = 0,
          providersUpdated = 0,
        )
      val importUri = mockk<Uri>()
      coEvery { importService.importBackup(BackupLocation(importUri.toString())) } returns
        NanoAIResult.success(summary)

      harness.testEvents {
        viewModel.importBackup(importUri)
        advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(SettingsUiEvent.ImportCompleted(summary))
      }
    }

  @Test
  fun saveHuggingFaceApiKeyUpdatesStatusMessage() =
    runTest(dispatcher) {
      coEvery { huggingFaceAuthCoordinator.savePersonalAccessToken(any()) } returns
        NanoAIResult.success(HuggingFaceAuthState(isAuthenticated = true))

      viewModel.saveHuggingFaceApiKey("hf_token")
      advanceUntilIdle()

      val state = harness.awaitState(predicate = { it.statusMessage == "Hugging Face connected" })
      assertThat(state.statusMessage).isEqualTo("Hugging Face connected")
    }

  @Test
  fun saveHuggingFaceApiKeyFailureEmitsError() =
    runTest(dispatcher) {
      coEvery { huggingFaceAuthCoordinator.savePersonalAccessToken(any()) } returns
        NanoAIResult.success(HuggingFaceAuthState(isAuthenticated = false, lastError = "invalid"))

      harness.testEvents {
        viewModel.saveHuggingFaceApiKey("bad")
        advanceUntilIdle()

        val event = awaitItem() as SettingsUiEvent.ErrorRaised
        assertThat(event.envelope.userMessage).isEqualTo("invalid")
      }
    }

  @Test
  fun startHuggingFaceOAuthLoginWithMissingClientIdEmitsError() =
    runTest(dispatcher) {
      huggingFaceOAuthConfig = HuggingFaceOAuthConfig(clientId = "", scope = "")
      buildHarness()

      harness.testEvents {
        viewModel.startHuggingFaceOAuthLogin()
        advanceUntilIdle()

        val event = awaitItem() as SettingsUiEvent.ErrorRaised
        assertThat(event.envelope.userMessage)
          .isEqualTo("Hugging Face OAuth client ID is not configured")
      }
    }

  @Test
  fun setThemePreferenceFailureEmitsError() =
    runTest(dispatcher) {
      coEvery { settingsOperationsUseCase.updateTheme(ThemePreference.DARK) } returns
        NanoAIResult.recoverable("")

      harness.testEvents {
        viewModel.setThemePreference(ThemePreference.DARK)
        advanceUntilIdle()

        val event = awaitItem() as SettingsUiEvent.ErrorRaised
        assertThat(event.envelope.userMessage).isEqualTo("Failed to update theme preference")
        val state =
          harness.awaitState(predicate = { it.lastErrorMessage == event.envelope.userMessage })
        assertThat(state.lastErrorMessage).isEqualTo("Failed to update theme preference")
      }
    }

  @Test
  fun setCompactModeFailureEmitsError() =
    runTest(dispatcher) {
      coEvery { toggleCompactModeUseCase.setCompactMode(true) } returns NanoAIResult.recoverable("")

      harness.testEvents {
        viewModel.setCompactMode(true)
        advanceUntilIdle()

        val event = awaitItem() as SettingsUiEvent.ErrorRaised
        assertThat(event.envelope.userMessage).isEqualTo("Failed to update compact mode preference")
      }
    }

  @Test
  fun setHighContrastEnabledFailureEmitsError() =
    runTest(dispatcher) {
      coEvery { updateUiPreferencesUseCase.setHighContrastEnabled(true) } returns
        NanoAIResult.recoverable("")

      harness.testEvents {
        viewModel.setHighContrastEnabled(true)
        advanceUntilIdle()

        val event = awaitItem() as SettingsUiEvent.ErrorRaised
        assertThat(event.envelope.userMessage)
          .isEqualTo("Failed to update high contrast preference")
      }
    }

  private fun initialiseFlows() {
    providersFlow = MutableStateFlow(emptyList())
    privacyFlow = MutableStateFlow(PrivacyPreference())
    uiPrefsFlow = MutableStateFlow(DataStoreUiPreferences())
    profileFlow =
      MutableStateFlow(
        ObserveUserProfileUseCase.Result(
          userProfile =
            UserProfile(
              id = "user",
              displayName = "User",
              themePreference = ThemePreference.SYSTEM,
              visualDensity = VisualDensity.DEFAULT,
              lastOpenedScreen = ScreenType.HOME,
              compactMode = false,
              pinnedTools = emptyList(),
              savedLayouts = emptyList(),
              highContrastEnabled = false,
            ),
          layoutSnapshots = emptyList(),
          uiState = null,
          offline = false,
          hydratedFromCache = false,
        )
      )
    authStateFlow = MutableStateFlow(HuggingFaceAuthState())
    deviceAuthFlow = MutableStateFlow(null)
  }

  private fun initialiseMocks() {
    apiProviderConfigUseCase = mockk(relaxed = true)
    modelDownloadsAndExportUseCase = mockk(relaxed = true)
    observePrivacyPreferencesUseCase = mockk(relaxed = true)
    observeUiPreferencesUseCase = mockk(relaxed = true)
    updatePrivacyPreferencesUseCase = mockk(relaxed = true)
    updateUiPreferencesUseCase = mockk(relaxed = true)
    importService = mockk(relaxed = true)
    observeUserProfileUseCase = mockk(relaxed = true)
    settingsOperationsUseCase = mockk(relaxed = true)
    toggleCompactModeUseCase = mockk(relaxed = true)
    huggingFaceAuthCoordinator = mockk(relaxed = true)
    huggingFaceOAuthConfig = HuggingFaceOAuthConfig(clientId = "client", scope = "all")

    every { apiProviderConfigUseCase.observeAllProviders() } returns providersFlow
    every { observePrivacyPreferencesUseCase() } returns privacyFlow
    every { observeUiPreferencesUseCase() } returns uiPrefsFlow
    every { observeUserProfileUseCase.flow } returns profileFlow
    every { huggingFaceAuthCoordinator.state } returns authStateFlow
    every { huggingFaceAuthCoordinator.deviceAuthState } returns deviceAuthFlow

    coEvery { updatePrivacyPreferencesUseCase.setTelemetryOptIn(any()) } returns Unit
    coEvery { updatePrivacyPreferencesUseCase.acknowledgeConsent(any()) } returns Unit
    coEvery { updatePrivacyPreferencesUseCase.setRetentionPolicy(any()) } returns Unit
    coEvery { updatePrivacyPreferencesUseCase.setExportWarningsDismissed(any()) } returns Unit
    coEvery { settingsOperationsUseCase.updateTheme(any()) } returns NanoAIResult.success(Unit)
    coEvery { toggleCompactModeUseCase.setCompactMode(any()) } returns NanoAIResult.success(Unit)
    coEvery { updateUiPreferencesUseCase.setHighContrastEnabled(any()) } returns
      NanoAIResult.success(Unit)
    coEvery { modelDownloadsAndExportUseCase.exportBackup(any(), any()) } returns
      NanoAIResult.success("/tmp/backup.json")
    coEvery { importService.importBackup(any()) } returns
      NanoAIResult.success(ImportSummary(0, 0, 0, 0))
    coEvery { huggingFaceAuthCoordinator.savePersonalAccessToken(any()) } returns
      NanoAIResult.success(HuggingFaceAuthState(isAuthenticated = false))
    coEvery { huggingFaceAuthCoordinator.beginDeviceAuthorization(any(), any()) } returns
      NanoAIResult.success(
        HuggingFaceDeviceAuthState(
          userCode = "CODE",
          verificationUri = "https://huggingface.co/login",
          verificationUriComplete = null,
          expiresAt = Clock.System.now() + 5.minutes,
          pollIntervalSeconds = 5,
        )
      )
    coEvery { huggingFaceAuthCoordinator.refreshAccount() } returns HuggingFaceAuthState()
    coEvery { huggingFaceAuthCoordinator.cancelDeviceAuthorization() } returns Unit
    coEvery { huggingFaceAuthCoordinator.clearCredentials() } returns Unit
  }

  private fun buildHarness() {
    viewModel =
      SettingsViewModel(
        apiProviderConfigUseCase,
        modelDownloadsAndExportUseCase,
        observePrivacyPreferencesUseCase,
        observeUiPreferencesUseCase,
        updatePrivacyPreferencesUseCase,
        updateUiPreferencesUseCase,
        importService,
        observeUserProfileUseCase,
        settingsOperationsUseCase,
        toggleCompactModeUseCase,
        huggingFaceAuthCoordinator,
        huggingFaceOAuthConfig,
        dispatcher,
      )
    harness = ViewModelStateHostTestHarness(viewModel)
  }

  private suspend fun ViewModelStateHostTestHarness<SettingsUiState, SettingsUiEvent>.testEvents(
    block: suspend TurbineTestContext<SettingsUiEvent>.() -> Unit
  ) {
    testEvents(timeout = 5.seconds, block = block)
  }
}
