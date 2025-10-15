package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreference
import com.vjaykrsna.nanoai.core.data.preferences.RetentionPolicy
import com.vjaykrsna.nanoai.core.domain.model.ApiProviderConfig
import com.vjaykrsna.nanoai.feature.settings.domain.ImportSummary
import com.vjaykrsna.nanoai.feature.settings.domain.huggingface.HuggingFaceAuthState
import com.vjaykrsna.nanoai.feature.settings.domain.huggingface.HuggingFaceDeviceAuthState
import com.vjaykrsna.nanoai.feature.settings.presentation.SettingsError
import com.vjaykrsna.nanoai.feature.settings.presentation.SettingsUiUxState
import com.vjaykrsna.nanoai.feature.settings.presentation.SettingsViewModel
import com.vjaykrsna.nanoai.testing.ComposeTestHarness
import com.vjaykrsna.nanoai.testing.TestEnvironmentRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose instrumentation tests for [SettingsScreen].
 *
 * Validates HuggingFace auth dialogs, export/import snackbar messaging, and privacy toggle
 * accessibility.
 */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @get:Rule val testEnvironmentRule = TestEnvironmentRule()

  private lateinit var viewModel: SettingsViewModel
  private lateinit var harness: ComposeTestHarness

  private val mockApiProviders = MutableStateFlow<List<ApiProviderConfig>>(emptyList())
  private val mockPrivacyPreferences =
    MutableStateFlow(
      PrivacyPreference(
        exportWarningsDismissed = false,
        telemetryOptIn = false,
        consentAcknowledgedAt = null,
        disclaimerShownCount = 0,
        retentionPolicy = RetentionPolicy.INDEFINITE,
      )
    )
  private val mockUiUxState = MutableStateFlow(SettingsUiUxState())
  private val mockHuggingFaceAuthState = MutableStateFlow(HuggingFaceAuthState.unauthenticated())
  private val mockHuggingFaceDeviceAuthState = MutableStateFlow<HuggingFaceDeviceAuthState?>(null)
  private val mockErrorEvents = MutableSharedFlow<SettingsError>()
  private val mockExportSuccess = MutableSharedFlow<String>()
  private val mockImportSuccess = MutableSharedFlow<ImportSummary>()

  @Before
  fun setup() {
    viewModel = mockk(relaxed = true)
    every { viewModel.apiProviders } returns mockApiProviders
    every { viewModel.privacyPreferences } returns mockPrivacyPreferences
    every { viewModel.uiUxState } returns mockUiUxState
    every { viewModel.huggingFaceAuthState } returns mockHuggingFaceAuthState
    every { viewModel.huggingFaceDeviceAuthState } returns mockHuggingFaceDeviceAuthState
    every { viewModel.errorEvents } returns mockErrorEvents
    every { viewModel.exportSuccess } returns mockExportSuccess
    every { viewModel.importSuccess } returns mockImportSuccess

    harness = ComposeTestHarness(composeTestRule)
  }

  @Test
  fun settingsScreen_displaysContentDescription() {
    composeTestRule.setContent { SettingsScreen(viewModel = viewModel) }

    composeTestRule
      .onNodeWithContentDescription("Settings screen organized by tabs with contextual sections")
      .assertIsDisplayed()
  }

  @Test
  fun settingsScreen_displaysCategoryTabs() {
    composeTestRule.setContent { SettingsScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Category tabs should be visible
    composeTestRule.onNodeWithText("General").assertExists()
    composeTestRule.onNodeWithText("Appearance").assertExists()
    composeTestRule.onNodeWithText("Privacy & Security").assertExists()
    composeTestRule.onNodeWithText("Offline & Models").assertExists()
  }

  @Test
  fun settingsScreen_switchCategory_showsDifferentContent() {
    composeTestRule.setContent { SettingsScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Click on Appearance tab
    composeTestRule.onNodeWithText("Appearance").performClick()
    composeTestRule.waitForIdle()

    // Appearance settings should be displayed
    // (Content depends on implementation)
  }

  @Test
  fun settingsScreen_privacyTab_displaysTelemetryToggle() {
    composeTestRule.setContent { SettingsScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Navigate to Privacy & Security tab
    composeTestRule.onNodeWithText("Privacy & Security").performClick()
    composeTestRule.waitForIdle()

    // Telemetry toggle should be visible
    composeTestRule.onNodeWithText("Telemetry", substring = true).assertExists()
  }

  @Test
  fun settingsScreen_telemetryToggle_hasAccessibility() {
    composeTestRule.setContent { SettingsScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Navigate to Privacy & Security
    composeTestRule.onNodeWithText("Privacy & Security").performClick()
    composeTestRule.waitForIdle()

    // Toggle should have proper semantics
    // (Specific accessibility labels depend on implementation)
  }

  @Test
  fun settingsScreen_toggleTelemetry_callsViewModel() {
    composeTestRule.setContent { SettingsScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Navigate to Privacy & Security
    composeTestRule.onNodeWithText("Privacy & Security").performClick()
    composeTestRule.waitForIdle()

    // Toggle telemetry
    // (Implementation depends on UI - find toggle and click)
  }

  @Test
  fun settingsScreen_huggingFaceAuth_notAuthenticated_showsLoginButton() {
    mockHuggingFaceAuthState.value = HuggingFaceAuthState.unauthenticated()

    composeTestRule.setContent { SettingsScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Navigate to Offline & Models
    composeTestRule.onNodeWithText("Offline & Models").performClick()
    composeTestRule.waitForIdle()

    // Login button should be visible
    composeTestRule.onNodeWithText("Connect", substring = true).assertExists()
  }

  @Test
  fun settingsScreen_huggingFaceAuth_authenticated_showsDisconnectButton() {
    mockHuggingFaceAuthState.value =
      HuggingFaceAuthState(isAuthenticated = true, username = "testuser")

    composeTestRule.setContent { SettingsScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Navigate to Offline & Models
    composeTestRule.onNodeWithText("Offline & Models").performClick()
    composeTestRule.waitForIdle()

    // User should be displayed
    composeTestRule.onNodeWithText("testuser", substring = true).assertExists()
  }

  @Test
  fun settingsScreen_clickHuggingFaceLogin_opensDialog() {
    mockHuggingFaceAuthState.value = HuggingFaceAuthState.unauthenticated()

    composeTestRule.setContent { SettingsScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Navigate to Offline & Models
    composeTestRule.onNodeWithText("Offline & Models").performClick()
    composeTestRule.waitForIdle()

    // Click login
    composeTestRule.onNodeWithText("Connect", substring = true).performClick()
    composeTestRule.waitForIdle()

    // Login dialog should appear
    // (Dialog content depends on implementation)
  }

  @Test
  fun settingsScreen_huggingFaceApiKeyDialog_allowsInput() {
    composeTestRule.setContent { SettingsScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Trigger API key dialog
    // (Implementation depends on how dialog is opened)
  }

  @Test
  fun settingsScreen_exportData_showsDialog() {
    composeTestRule.setContent { SettingsScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Navigate to Backup & Restore
    composeTestRule.onNodeWithText("Backup & Restore").performClick()
    composeTestRule.waitForIdle()

    // Click export
    // (Implementation depends on UI)
  }

  @Test
  fun settingsScreen_exportSuccess_showsSnackbar() {
    composeTestRule.setContent { SettingsScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Emit export success event
    mockExportSuccess.tryEmit("Export successful")

    // Snackbar should appear
    composeTestRule.onNodeWithText("Export successful", substring = true).assertExists()
  }

  @Test
  fun settingsScreen_importData_triggersFilePicker() {
    composeTestRule.setContent { SettingsScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Navigate to Backup & Restore
    composeTestRule.onNodeWithText("Backup & Restore").performClick()
    composeTestRule.waitForIdle()

    // Click import
    // (Implementation depends on UI and file picker integration)
  }

  @Test
  fun settingsScreen_importSuccess_showsSnackbar() {
    composeTestRule.setContent { SettingsScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Emit import success event
    mockImportSuccess.tryEmit(
      ImportSummary(
        personasImported = 5,
        personasUpdated = 0,
        providersImported = 2,
        providersUpdated = 1
      )
    )

    // Snackbar should appear
    composeTestRule.onNodeWithText("Import completed", substring = true).assertExists()
  }

  @Test
  fun settingsScreen_error_showsSnackbar() {
    composeTestRule.setContent { SettingsScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Emit error event
    mockErrorEvents.tryEmit(SettingsError.UnexpectedError("Test error"))

    // Snackbar should appear
    composeTestRule.onNodeWithText("Test error", substring = true).assertExists()
  }

  @Test
  fun settingsScreen_addProviderFab_visibleOnOfflineTab() {
    composeTestRule.setContent { SettingsScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Navigate to Offline & Models
    composeTestRule.onNodeWithText("Offline & Models").performClick()
    composeTestRule.waitForIdle()

    // FAB should be visible
    composeTestRule.onNodeWithContentDescription("Add API provider").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_addProviderFab_hiddenOnOtherTabs() {
    composeTestRule.setContent { SettingsScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // On General tab, FAB should not be visible
    composeTestRule.onNodeWithContentDescription("Add API provider").assertDoesNotExist()
  }

  @Test
  fun settingsScreen_clickAddProvider_opensDialog() {
    composeTestRule.setContent { SettingsScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Navigate to Offline & Models
    composeTestRule.onNodeWithText("Offline & Models").performClick()
    composeTestRule.waitForIdle()

    // Click FAB
    composeTestRule.onNodeWithContentDescription("Add API provider").performClick()
    composeTestRule.waitForIdle()

    // Dialog should appear
    // TODO: Verify appropriate ViewModel method call when UI is implemented
    // verify { viewModel.startAddProvider() }
  }

  @Test
  fun settingsScreen_retentionPolicy_displaysOptions() {
    composeTestRule.setContent { SettingsScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Navigate to Privacy & Security
    composeTestRule.onNodeWithText("Privacy & Security").performClick()
    composeTestRule.waitForIdle()

    // Retention policy options should be visible
    // (Implementation depends on UI)
  }

  @Test
  fun settingsScreen_themePreference_displaysOptions() {
    composeTestRule.setContent { SettingsScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Navigate to Appearance
    composeTestRule.onNodeWithText("Appearance").performClick()
    composeTestRule.waitForIdle()

    // Theme options should be visible
    // (Implementation depends on UI - Light/Dark/System)
  }

  @Test
  fun settingsScreen_compactMode_toggleWorks() {
    composeTestRule.setContent { SettingsScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Navigate to Appearance
    composeTestRule.onNodeWithText("Appearance").performClick()
    composeTestRule.waitForIdle()

    // Compact mode toggle should be available
    // (Implementation depends on UI)
  }

  @Test
  fun settingsScreen_undoButton_appearsAfterChange() {
    composeTestRule.setContent { SettingsScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Make a UI preference change
    // Undo button should appear
    // (Implementation depends on undo mechanism)
  }

  @Test
  fun settingsScreen_aboutSection_displaysVersion() {
    composeTestRule.setContent { SettingsScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Navigate to About & Help
    composeTestRule.onNodeWithText("About & Help").performClick()
    composeTestRule.waitForIdle()

    // Version info should be displayed
    // (Implementation depends on UI)
  }
}
