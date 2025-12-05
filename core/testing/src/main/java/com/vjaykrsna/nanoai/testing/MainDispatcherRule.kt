package com.vjaykrsna.nanoai.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit 4 test rule for replacing [Dispatchers.Main] with a test dispatcher.
 *
 * This rule is retained only for Robolectric tests that require JUnit 4
 * (`@RunWith(RobolectricTestRunner::class)`).
 */
@Deprecated(
  message = "Use MainDispatcherExtension for JUnit 5 tests",
  replaceWith =
    ReplaceWith("MainDispatcherExtension", "com.vjaykrsna.nanoai.testing.MainDispatcherExtension"),
)
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()) :
  TestWatcher() {

  override fun starting(description: Description) {
    Dispatchers.setMain(testDispatcher)
  }

  override fun finished(description: Description) {
    Dispatchers.resetMain()
  }
}
