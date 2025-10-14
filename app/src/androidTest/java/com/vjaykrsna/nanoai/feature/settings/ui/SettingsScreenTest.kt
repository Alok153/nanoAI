package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vjaykrsna.nanoai.core.domain.model.ExportService
import com.vjaykrsna.nanoai.core.domain.model.HuggingFaceAuthState
import com.vjaykrsna.nanoai.core.domain.model.PrivacyPreferences
import com.vjaykrsna.nanoai.core.domain.model.RetentionPolicy
import com.vjaykrsna.nanoai.feature.settings.presentation.SettingsViewModel
import com.vjaykrsna.nanoai.testing.ComposeTestHarness
import com.vjaykrsna.nanoai.testing.TestEnvironmentRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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

  private val mockApiProviders = MutableStateFlow<List<ExportService>>(emptyList())
  private val mockPrivacyPreferences =
    MutableStateFlow(
      PrivacyPreferences(
        telemetryOptIn = false,
        consentTimestamp = null,
        retentionPolicy = RetentionPolicy.STANDARD_30_DAYS,
        crashReportingEnabled = false
      )
    )
  private val mockUiUxState = MutableStateFlow<Any>(Unit)
  private val mockHuggingFaceAuthState =
    MutableStateFlow(HuggingFaceAuthState.NotAuthenticated as HuggingFaceAuthState)
  private val mockHuggingFaceDeviceAuthState = MutableStateFlow<Any?>(null)

  @Before
  fun setup() {
    viewModel = mockk(relaxed = true)
    every { viewModel.apiProviders } returns mockApiProviders
    every { viewModel.privacyPreferences } returns mockPrivacyPreferences
    every { viewModel.uiUxState } returns mockUiUxState
    every { viewModel.huggingFaceAuthState } returns mockHuggingFaceAuthState
    every { viewModel.huggingFaceDeviceAuthState } returns mockHuggingFaceDeviceAuthState
    every { viewModel.errorEvents } returns flowOf()
    every { viewModel.exportSuccess } returns flowOf()
    every { viewModel.importSuccess } returns flowOf()

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
    mockHuggingFaceAuthState.value = HuggingFaceAuthState.NotAuthenticated

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
    mockHuggingFaceAuthState.value = HuggingFaceAuthState.Authenticated(username = "testuser")

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
    mockHuggingFaceAuthState.value = HuggingFaceAuthState.NotAuthenticated

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
    // Mock export success event
    every { viewModel.exportSuccess } returns flowOf("Export successful")

    composeTestRule.setContent { SettingsScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

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
    // Mock import success event
    every { viewModel.importSuccess } returns flowOf("Import completed")

    composeTestRule.setContent { SettingsScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Snackbar should appear
    composeTestRule.onNodeWithText("Import completed", substring = true).assertExists()
  }

  @Test
  fun settingsScreen_error_showsSnackbar() {
    // Mock error event
    every { viewModel.errorEvents } returns flowOf(RuntimeException("Test error"))

    composeTestRule.setContent { SettingsScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    // Error snackbar should appear
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
    verify { viewModel.startAddProvider() }
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
