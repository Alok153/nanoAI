package com.vjaykrsna.nanoai.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreference
import com.vjaykrsna.nanoai.core.data.preferences.RetentionPolicy
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.feature.settings.presentation.SettingsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI instrumentation test for Settings features.
 * Tests cloud fallback badges, quota chips, export warning dialog, and consent flows.
 *
 * TDD: This test is written BEFORE the UI is implemented.
 * Expected to FAIL with compilation errors until:
 * - SettingsScreen composable is created
 * - SettingsViewModel is defined
 * - ExportDialog is implemented
 * - PrivacyDashboard component exists
 */
@RunWith(AndroidJUnit4::class)
class CloudFallbackAndExportTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: SettingsViewModel
    private lateinit var apiProvidersFlow: MutableStateFlow<List<APIProviderConfig>>
    private lateinit var privacyPreferencesFlow: MutableStateFlow<PrivacyPreference>
    private lateinit var isLoadingFlow: MutableStateFlow<Boolean>

    @Before
    fun setup() {
        viewModel = mockk(relaxed = true)
        apiProvidersFlow = MutableStateFlow(emptyList())
        privacyPreferencesFlow =
            MutableStateFlow(
                PrivacyPreference(
                    exportWarningsDismissed = false,
                    telemetryOptIn = false,
                    consentAcknowledgedAt = null,
                    retentionPolicy = RetentionPolicy.INDEFINITE,
                ),
            )
        isLoadingFlow = MutableStateFlow(false)

        coEvery { viewModel.apiProviders } returns apiProvidersFlow
        coEvery { viewModel.privacyPreferences } returns privacyPreferencesFlow
        coEvery { viewModel.isLoading } returns isLoadingFlow
    }

    @Test
    fun apiProviderList_shouldDisplayConfiguredProviders() {
        // Arrange
        val providers =
            listOf(
                APIProviderConfig(
                    providerId = "openai",
                    providerName = "OpenAI",
                    baseUrl = "https://api.openai.com/v1",
                    apiKey = "sk-test***",
                    apiType = APIType.OPENAI_COMPATIBLE,
                    isEnabled = true,
                    quotaResetAt = null,
                    lastStatus = ProviderStatus.OK,
                ),
                APIProviderConfig(
                    providerId = "google",
                    providerName = "Google Gemini",
                    baseUrl = "https://generativelanguage.googleapis.com/v1",
                    apiKey = "AIza***",
                    apiType = APIType.GEMINI,
                    isEnabled = false,
                    quotaResetAt = Instant.now().plusSeconds(3600),
                    lastStatus = ProviderStatus.RATE_LIMITED,
                ),
            )

        uiState.value = uiState.value.copy(apiProviders = providers)

        // Act
        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel)
        }

        // Assert
        composeTestRule.onNodeWithText("OpenAI").assertIsDisplayed()
        composeTestRule.onNodeWithText("Google Gemini").assertIsDisplayed()
    }

    @Test
    fun cloudBadge_shouldShowStatusForEnabledProvider() {
        // Arrange
        val provider =
            APIProviderConfig(
                providerId = "openai",
                providerName = "OpenAI",
                baseUrl = "https://api.openai.com/v1",
                apiKey = "sk-test***",
                apiType = APIType.OPENAI_COMPATIBLE,
                isEnabled = true,
                quotaResetAt = null,
                lastStatus = ProviderStatus.OK,
            )

        uiState.value = uiState.value.copy(apiProviders = listOf(provider))

        // Act
        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel)
        }

        // Assert
        composeTestRule.onNodeWithTag("cloud-badge-ok").assertIsDisplayed()
        composeTestRule.onNodeWithText("Active").assertIsDisplayed()
    }

    @Test
    fun quotaChip_shouldDisplayWhenRateLimited() {
        // Arrange
        val rateLimitedProvider =
            APIProviderConfig(
                providerId = "openai",
                providerName = "OpenAI",
                baseUrl = "https://api.openai.com/v1",
                apiKey = "sk-test***",
                apiType = APIType.OPENAI_COMPATIBLE,
                isEnabled = true,
                quotaResetAt = Instant.now().plusSeconds(1800), // 30 minutes
                lastStatus = ProviderStatus.RATE_LIMITED,
            )

        uiState.value = uiState.value.copy(apiProviders = listOf(rateLimitedProvider))

        // Act
        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel)
        }

        // Assert
        composeTestRule.onNodeWithText("Rate Limited").assertIsDisplayed()
        composeTestRule.onNodeWithText("Resets in 30m").assertIsDisplayed()
    }

    @Test
    fun quotaChip_shouldShowErrorStatus() {
        // Arrange
        val errorProvider =
            APIProviderConfig(
                providerId = "google",
                providerName = "Google Gemini",
                baseUrl = "https://generativelanguage.googleapis.com/v1",
                apiKey = "AIza***",
                apiType = APIType.GEMINI,
                isEnabled = true,
                quotaResetAt = null,
                lastStatus = ProviderStatus.ERROR,
            )

        uiState.value = uiState.value.copy(apiProviders = listOf(errorProvider))

        // Act
        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel)
        }

        // Assert
        composeTestRule.onNodeWithTag("cloud-badge-error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Error").assertIsDisplayed()
    }

    @Test
    fun exportButton_shouldOpenExportDialog() {
        // Arrange
        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel)
        }

        // Act
        composeTestRule.onNodeWithText("Export Backup").performClick()

        // Assert
        composeTestRule.onNodeWithText("Export Configuration").assertIsDisplayed()
    }

    @Test
    fun exportDialog_shouldShowWarningAboutUnencryptedExport() {
        // Arrange
        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel)
        }

        // Act
        composeTestRule.onNodeWithText("Export Backup").performClick()

        // Assert
        composeTestRule.onNodeWithText("Unencrypted Export").assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                "This export will not be encrypted. Store it securely.",
            ).assertIsDisplayed()
    }

    @Test
    fun exportDialog_shouldOfferChatHistoryOption() {
        // Arrange
        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel)
        }

        // Act
        composeTestRule.onNodeWithText("Export Backup").performClick()

        // Assert
        composeTestRule.onNodeWithText("Include chat history").assertIsDisplayed()
    }

    @Test
    fun exportDialog_confirmButton_shouldTriggerExport() {
        // Arrange
        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Export Backup").performClick()

        // Act
        composeTestRule.onNodeWithText("Export").performClick()

        // Assert
        coVerify { viewModel.exportBackup(includeChatHistory = false) }
    }

    @Test
    fun exportDialog_withChatHistory_shouldPassFlag() {
        // Arrange
        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Export Backup").performClick()

        // Act: Enable chat history checkbox
        composeTestRule
            .onNodeWithContentDescription("Include chat history toggle")
            .performClick()
        composeTestRule.onNodeWithText("Export").performClick()

        // Assert
        coVerify { viewModel.exportBackup(includeChatHistory = true) }
    }

    @Test
    fun exportProgress_shouldShowLoadingIndicator() {
        // Arrange
        uiState.value = uiState.value.copy(exportInProgress = true)

        // Act
        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel)
        }

        // Assert
        composeTestRule
            .onNodeWithContentDescription("Export in progress")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Creating backup...").assertIsDisplayed()
    }

    @Test
    fun privacyDashboard_shouldDisplayConsentTimestamp() {
        // Arrange
        val preferences =
            PrivacyPreference(
                preferenceId = 1,
                exportWarningsDismissed = false,
                telemetryOptIn = false,
                consentAcknowledgedAt = Instant.now().minusSeconds(86400), // 1 day ago
                retentionPolicy = RetentionPolicy.INDEFINITE,
            )

        uiState.value = uiState.value.copy(privacyPreferences = preferences)

        // Act
        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel)
        }

        // Assert
        composeTestRule.onNodeWithText("Privacy").assertIsDisplayed()
        composeTestRule.onNodeWithText("Consent recorded 1 day ago").assertIsDisplayed()
    }

    @Test
    fun privacyDashboard_shouldShowTelemetryToggle() {
        // Arrange
        val preferences =
            PrivacyPreference(
                preferenceId = 1,
                exportWarningsDismissed = false,
                telemetryOptIn = false,
                consentAcknowledgedAt = Instant.now(),
                retentionPolicy = RetentionPolicy.INDEFINITE,
            )

        uiState.value = uiState.value.copy(privacyPreferences = preferences)

        // Act
        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel)
        }

        // Assert
        composeTestRule.onNodeWithText("Share anonymous usage data").assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Telemetry toggle")
            .assertIsDisplayed()
    }

    @Test
    fun telemetryToggle_whenDisabled_shouldNotCollectData() {
        // Arrange
        val preferences =
            PrivacyPreference(
                preferenceId = 1,
                exportWarningsDismissed = false,
                telemetryOptIn = false,
                consentAcknowledgedAt = Instant.now(),
                retentionPolicy = RetentionPolicy.INDEFINITE,
            )

        uiState.value = uiState.value.copy(privacyPreferences = preferences)

        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel)
        }

        // Assert: Toggle should be off
        val toggleNode = composeTestRule.onNodeWithContentDescription("Telemetry toggle")
        toggleNode.assertExists()
        // Check that it's not enabled (would need semantic property check)
    }

    @Test
    fun telemetryToggle_whenClicked_shouldUpdatePreference() {
        // Arrange
        val preferences =
            PrivacyPreference(
                preferenceId = 1,
                exportWarningsDismissed = false,
                telemetryOptIn = false,
                consentAcknowledgedAt = Instant.now(),
                retentionPolicy = RetentionPolicy.INDEFINITE,
            )

        uiState.value = uiState.value.copy(privacyPreferences = preferences)

        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel)
        }

        // Act
        composeTestRule
            .onNodeWithContentDescription("Telemetry toggle")
            .performClick()

        // Assert
        coVerify { viewModel.updateTelemetryConsent(true) }
    }

    @Test
    fun consentFlow_shouldUpdateTimestamp() {
        // Arrange
        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel)
        }

        // Act: Acknowledge privacy consent
        composeTestRule
            .onNodeWithText("Acknowledge Privacy Policy")
            .performClick()

        // Assert
        coVerify { viewModel.acknowledgePrivacyConsent() }
    }

    @Test
    fun exportWarning_firstTime_shouldShowFullWarning() {
        // Arrange: First export, warning not dismissed
        val preferences =
            PrivacyPreference(
                preferenceId = 1,
                exportWarningsDismissed = false,
                telemetryOptIn = false,
                consentAcknowledgedAt = Instant.now(),
                retentionPolicy = RetentionPolicy.INDEFINITE,
            )

        uiState.value = uiState.value.copy(privacyPreferences = preferences)

        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel)
        }

        // Act
        composeTestRule.onNodeWithText("Export Backup").performClick()

        // Assert
        composeTestRule.onNodeWithText("Important Security Notice").assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                "Your export contains API keys and sensitive data in plain text.",
            ).assertIsDisplayed()
    }

    @Test
    fun exportWarning_canBeDismissed() {
        // Arrange
        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Export Backup").performClick()

        // Act
        composeTestRule
            .onNodeWithText("Don't show this again")
            .performClick()

        // Assert
        coVerify { viewModel.dismissExportWarning() }
    }

    @Test
    fun unauthorizedProvider_shouldShowRefreshButton() {
        // Arrange
        val unauthorizedProvider =
            APIProviderConfig(
                providerId = "openai",
                providerName = "OpenAI",
                baseUrl = "https://api.openai.com/v1",
                apiKey = "sk-invalid",
                apiType = APIType.OPENAI_COMPATIBLE,
                isEnabled = true,
                quotaResetAt = null,
                lastStatus = ProviderStatus.UNAUTHORIZED,
            )

        uiState.value = uiState.value.copy(apiProviders = listOf(unauthorizedProvider))

        // Act
        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel)
        }

        // Assert
        composeTestRule.onNodeWithText("Unauthorized").assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Update API key")
            .assertIsDisplayed()
    }

    @Test
    fun customEndpoint_shouldAllowConfiguration() {
        // Arrange
        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel)
        }

        // Act
        composeTestRule.onNodeWithText("Add Custom Provider").performClick()

        // Assert
        composeTestRule.onNodeWithText("Provider Name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Base URL").assertIsDisplayed()
        composeTestRule.onNodeWithText("API Key").assertIsDisplayed()
        composeTestRule.onNodeWithText("API Type").assertIsDisplayed()
    }

    @Test
    fun talkbackLabels_shouldDescribeCloudStatus() {
        // Arrange
        val provider =
            APIProviderConfig(
                providerId = "openai",
                providerName = "OpenAI",
                baseUrl = "https://api.openai.com/v1",
                apiKey = "sk-test***",
                apiType = APIType.OPENAI_COMPATIBLE,
                isEnabled = true,
                quotaResetAt = null,
                lastStatus = ProviderStatus.OK,
            )

        uiState.value = uiState.value.copy(apiProviders = listOf(provider))

        // Act
        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel)
        }

        // Assert
        composeTestRule
            .onNodeWithContentDescription("OpenAI provider status: Active")
            .assertExists()
    }

    @Test
    fun talkbackLabels_shouldDescribeExportWarning() {
        // Arrange
        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Export Backup").performClick()

        // Assert
        composeTestRule
            .onNodeWithContentDescription(
                "Security warning: Export will not be encrypted",
            ).assertExists()
    }
}
