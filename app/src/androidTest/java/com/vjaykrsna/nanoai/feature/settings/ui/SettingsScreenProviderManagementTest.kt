package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vjaykrsna.nanoai.core.domain.model.ApiProviderConfig
import com.vjaykrsna.nanoai.core.model.APIType
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@HiltAndroidTest
class SettingsScreenProviderManagementTest : BaseSettingsScreenTest() {

  @Test
  fun settingsScreen_addProviderFab_visibleOnOfflineTab() {
    renderSettingsScreen()

    composeTestRule.onNodeWithText("APIs").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithContentDescription("Add API provider").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_addProviderFab_hiddenOnOtherTabs() {
    renderSettingsScreen()

    composeTestRule.onNodeWithContentDescription("Add API provider").assertDoesNotExist()
  }

  @Test
  fun settingsScreen_clickAddProvider_opensDialog() {
    renderSettingsScreen()

    composeTestRule.onNodeWithText("APIs").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithContentDescription("Add API provider").performClick()
    composeTestRule.waitForIdle()
    // TODO: verify dialog content when exposed via semantics
  }

  @Test
  fun settingsScreen_displaysProviderList() {
    updateState {
      it.copy(
        apiProviders =
          listOf(
            ApiProviderConfig(
              providerId = "gemini-cloud",
              providerName = "Google Gemini",
              baseUrl = "https://gemini.googleapis.com",
              apiType = APIType.OPENAI_COMPATIBLE,
              isEnabled = true,
              credentialId = "test-credential-id",
            )
          )
      )
    }

    renderSettingsScreen()

    composeTestRule.onNodeWithText("APIs").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Google Gemini", substring = true).assertIsDisplayed()
  }

  @Test
  fun settingsScreen_addProviderDialog_showsForm() {
    renderSettingsScreen()

    composeTestRule.onNodeWithText("APIs").performClick()
    composeTestRule.waitForIdle()
    // TODO: trigger add provider dialog when fields expose tags
  }

  @Test
  fun settingsScreen_editProvider_updatesData() {
    updateState {
      it.copy(
        apiProviders =
          listOf(
            ApiProviderConfig(
              providerId = "gemini-cloud",
              providerName = "Google Gemini",
              baseUrl = "https://gemini.googleapis.com",
              apiType = APIType.OPENAI_COMPATIBLE,
              isEnabled = true,
              credentialId = "test-credential-id",
            )
          )
      )
    }

    renderSettingsScreen()

    composeTestRule.onNodeWithText("APIs").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Google Gemini", substring = true).performClick()
    // TODO: assert edit dialog once implementation provides test tags
  }
}
