package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.vjaykrsna.nanoai.feature.settings.presentation.SettingsViewModel
import com.vjaykrsna.nanoai.feature.settings.presentation.model.SettingsUiEvent
import com.vjaykrsna.nanoai.feature.settings.presentation.state.SettingsUiState
import com.vjaykrsna.nanoai.shared.testing.TestEnvironmentRule
import com.vjaykrsna.nanoai.shared.testing.TestingTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule

@HiltAndroidTest
@OptIn(ExperimentalTestApi::class)
abstract class BaseSettingsScreenTest {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1)
  val composeTestRule: ComposeContentTestRule = createAndroidComposeRule<ComponentActivity>()
  @JvmField @Rule val testEnvironmentRule = TestEnvironmentRule()

  protected lateinit var viewModel: SettingsViewModel

  protected val mockState = MutableStateFlow(SettingsUiState())
  protected val mockEvents = MutableSharedFlow<SettingsUiEvent>(extraBufferCapacity = 4)

  @Before
  fun setUpBase() {
    hiltRule.inject()
    viewModel = mockk(relaxed = true)
    every { viewModel.state } returns mockState
    every { viewModel.events } returns mockEvents
  }

  protected fun renderSettingsScreen() {
    composeTestRule.setContent { TestingTheme { SettingsScreen(viewModel = viewModel) } }
    composeTestRule.waitForIdle()
  }

  protected fun updateState(transform: (SettingsUiState) -> SettingsUiState) {
    mockState.value = transform(mockState.value)
  }

  protected fun emitEvent(event: SettingsUiEvent) {
    mockEvents.tryEmit(event)
  }
}
