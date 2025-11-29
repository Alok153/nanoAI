package com.vjaykrsna.nanoai.feature.uiux.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.uiux.ModeId
import com.vjaykrsna.nanoai.core.domain.model.uiux.RightPanel
import com.vjaykrsna.nanoai.shared.ui.shell.ShellUiEvent
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class ShellViewModelNavigationTest {
  private val dispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension(dispatcher)

  @Test
  fun openMode_closesDrawersAndHidesPalette() =
    runTest(dispatcher) {
      val fakeRepos = createFakeRepositories()
      val viewModel = createShellViewModelForTest(fakeRepos, dispatcher)

      viewModel.onEvent(ShellUiEvent.ModeSelected(ModeId.CHAT))
      advanceUntilIdle()

      val uiState = viewModel.uiState.first { state -> state.layout.activeMode == ModeId.CHAT }
      assertThat(uiState.layout.activeMode).isEqualTo(ModeId.CHAT)
      assertThat(uiState.layout.isLeftDrawerOpen).isFalse()
      assertThat(uiState.layout.showCommandPalette).isFalse()
    }

  @Test
  fun toggleRightDrawer_setsPanelAndReflectsInState() =
    runTest(dispatcher) {
      val fakeRepos = createFakeRepositories()
      val viewModel = createShellViewModelForTest(fakeRepos, dispatcher)

      viewModel.onEvent(ShellUiEvent.ToggleRightDrawer(RightPanel.MODEL_SELECTOR))
      advanceUntilIdle()

      val uiState =
        viewModel.uiState.first { state ->
          state.layout.activeRightPanel == RightPanel.MODEL_SELECTOR
        }
      assertThat(uiState.layout.isRightDrawerOpen).isTrue()
      assertThat(uiState.layout.activeRightPanel).isEqualTo(RightPanel.MODEL_SELECTOR)
    }
}
