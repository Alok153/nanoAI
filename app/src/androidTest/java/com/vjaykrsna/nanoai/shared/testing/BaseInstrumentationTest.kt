package com.vjaykrsna.nanoai.shared.testing

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vjaykrsna.nanoai.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

/**
 * Base class for all instrumentation tests providing standardized setup:
 * - AndroidJUnit4 runner
 * - Hilt dependency injection
 * - TestEnvironmentRule for clean test state
 * - Compose testing rule with MainActivity
 */
@RunWith(AndroidJUnit4::class)
abstract class BaseInstrumentationTest {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val environmentRule = TestEnvironmentRule()

  @get:Rule(order = 2) val composeRule = createAndroidComposeRule<MainActivity>()

  val composeTestRule: ComposeContentTestRule = composeRule

  @Before
  fun injectDependencies() {
    hiltRule.inject()
  }
}
