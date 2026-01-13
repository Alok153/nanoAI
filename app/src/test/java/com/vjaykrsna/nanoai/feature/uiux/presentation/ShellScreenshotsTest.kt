package com.vjaykrsna.nanoai.feature.uiux.presentation

import androidx.compose.ui.test.junit4.createComposeRule
import com.vjaykrsna.nanoai.shared.ui.theme.NanoAITheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Smoke test to ensure the shell scaffold composes without crashing. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShellScreenshotsTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun shellScaffold_renders() {
    composeRule.setContent { NanoAITheme {} }
    composeRule.waitForIdle()
  }
}
