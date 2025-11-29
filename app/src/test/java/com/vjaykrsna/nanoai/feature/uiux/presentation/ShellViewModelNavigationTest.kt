package com.vjaykrsna.nanoai.feature.uiux.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.uiux.ModeId
import com.vjaykrsna.nanoai.core.domain.model.uiux.RightPanel
import com.vjaykrsna.nanoai.core.domain.uiux.NavigationOperationsUseCase
import com.vjaykrsna.nanoai.core.domain.uiux.ObserveUserProfileUseCase
import com.vjaykrsna.nanoai.shared.ui.shell.ShellUiEvent
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.mockk
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
      val navigationOperationsUseCase =
        NavigationOperationsUseCase(fakeRepos.navigationRepository, dispatcher)

      val observeUserProfileUseCase =
        ObserveUserProfileUseCase(fakeRepos.userProfileRepository, dispatcher)

      val navigationCoordinator = mockk<NavigationCoordinator>(relaxed = true)
      val connectivityCoordinator = createConnectivityCoordinator(fakeRepos, dispatcher)
      val themeCoordinator = createThemeCoordinator(fakeRepos, dispatcher)
      val progressCoordinator = createProgressCoordinator(fakeRepos, dispatcher)

      val viewModel =
        ShellViewModel(
          navigationOperationsUseCase,
          observeUserProfileUseCase,
          navigationCoordinator,
          connectivityCoordinator,
          progressCoordinator,
          themeCoordinator,
          dispatcher,
        )

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
      val navigationOperationsUseCase =
        NavigationOperationsUseCase(fakeRepos.navigationRepository, dispatcher)

      val observeUserProfileUseCase =
        ObserveUserProfileUseCase(fakeRepos.userProfileRepository, dispatcher)

      val navigationCoordinator = mockk<NavigationCoordinator>(relaxed = true)
      val connectivityCoordinator = createConnectivityCoordinator(fakeRepos, dispatcher)
      val themeCoordinator = createThemeCoordinator(fakeRepos, dispatcher)
      val progressCoordinator = createProgressCoordinator(fakeRepos, dispatcher)

      val viewModel =
        ShellViewModel(
          navigationOperationsUseCase,
          observeUserProfileUseCase,
          navigationCoordinator,
          connectivityCoordinator,
          progressCoordinator,
          themeCoordinator,
          dispatcher,
        )

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
