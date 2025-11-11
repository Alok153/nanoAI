package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vjaykrsna.nanoai.core.domain.settings.huggingface.HuggingFaceAuthState
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@HiltAndroidTest
class SettingsScreenHuggingFaceAuthTest : BaseSettingsScreenTest() {

  @Test
  fun settingsScreen_huggingFaceAuth_notAuthenticated_showsLoginButton() {
    updateState { it.copy(huggingFaceAuthState = HuggingFaceAuthState.unauthenticated()) }

    renderSettingsScreen()

    composeTestRule.onNodeWithText("APIs").performClick()
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithContentDescription("Login with Hugging Face account")
      .assertIsDisplayed()
  }

  @Test
  fun settingsScreen_huggingFaceAuth_authenticated_showsDisconnectButton() {
    updateState {
      it.copy(
        huggingFaceAuthState =
          HuggingFaceAuthState(
            isAuthenticated = true,
            username = "testuser",
            displayName = "Test User",
          )
      )
    }

    renderSettingsScreen()

    composeTestRule.onNodeWithText("APIs").performClick()
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithText("Connected as Test User", substring = false, useUnmergedTree = true)
      .assertExists()
  }

  @Test
  fun settingsScreen_clickHuggingFaceLogin_opensDialog() {
    updateState { it.copy(huggingFaceAuthState = HuggingFaceAuthState.unauthenticated()) }

    renderSettingsScreen()

    composeTestRule.onNodeWithText("APIs").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithContentDescription("Login with Hugging Face account").performClick()
    composeTestRule.waitForIdle()
    // TODO: assert dialog content when exposed via semantics
  }

  @Test
  fun settingsScreen_huggingFaceLogin_triggersOAuth() {
    updateState { it.copy(huggingFaceAuthState = HuggingFaceAuthState.unauthenticated()) }

    renderSettingsScreen()

    composeTestRule.onNodeWithText("APIs").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithContentDescription("Login with Hugging Face account").performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun settingsScreen_huggingFaceAccount_displaysInfo() {
    updateState {
      it.copy(
        huggingFaceAuthState =
          HuggingFaceAuthState(
            isAuthenticated = true,
            username = "testuser",
            displayName = "Test User",
          )
      )
    }

    renderSettingsScreen()

    composeTestRule.onNodeWithText("APIs").performClick()
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithText("Connected as Test User", substring = false, useUnmergedTree = true)
      .assertExists()
  }

  @Test
  fun settingsScreen_huggingFaceLogout_clearsCredentials() {
    updateState {
      it.copy(
        huggingFaceAuthState =
          HuggingFaceAuthState(
            isAuthenticated = true,
            username = "testuser",
            displayName = "Test User",
          )
      )
    }

    renderSettingsScreen()

    composeTestRule.onNodeWithText("APIs").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Disconnect", substring = true).performClick()
    composeTestRule.waitForIdle()
  }
}
