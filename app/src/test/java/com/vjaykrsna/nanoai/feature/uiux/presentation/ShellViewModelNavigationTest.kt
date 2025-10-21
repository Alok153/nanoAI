package com.vjaykrsna.nanoai.feature.uiux.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.feature.uiux.domain.ConnectivityOperationsUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.JobOperationsUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.NavigationOperationsUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.QueueJobUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.SettingsOperationsUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.UndoActionUseCase
import com.vjaykrsna.nanoai.feature.uiux.state.ModeId
import com.vjaykrsna.nanoai.feature.uiux.state.RightPanel
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
      val repository = FakeShellStateRepository()
      val actionProvider = createFakeCommandPaletteActionProvider()
      val progressCoordinator = createFakeProgressCenterCoordinator()
      val navigationOperationsUseCase = mockk<NavigationOperationsUseCase>(relaxed = true)
      val connectivityOperationsUseCase = mockk<ConnectivityOperationsUseCase>(relaxed = true)
      val queueJobUseCase = mockk<QueueJobUseCase>(relaxed = true)
      val jobOperationsUseCase = mockk<JobOperationsUseCase>(relaxed = true)
      val undoActionUseCase = mockk<UndoActionUseCase>(relaxed = true)
      val settingsOperationsUseCase = mockk<SettingsOperationsUseCase>(relaxed = true)

      // Set up navigation use case to actually call repository
      every { navigationOperationsUseCase.openMode(any()) } answers
        {
          runBlocking { repository.openMode(firstArg()) }
        }

      val viewModel =
        ShellViewModel(
          repository,
          actionProvider,
          progressCoordinator,
          navigationOperationsUseCase,
          connectivityOperationsUseCase,
          queueJobUseCase,
          jobOperationsUseCase,
          undoActionUseCase,
          settingsOperationsUseCase,
          dispatcher,
        )

      viewModel.openMode(ModeId.CHAT)
      advanceUntilIdle()

      val uiState =
        viewModel.uiState.first { state ->
          repository.openModeCalls.isNotEmpty() && state.layout.activeMode == ModeId.CHAT
        }
      assertThat(uiState.layout.activeMode).isEqualTo(ModeId.CHAT)
      assertThat(uiState.layout.isLeftDrawerOpen).isFalse()
      assertThat(uiState.layout.showCommandPalette).isFalse()
      assertThat(repository.openModeCalls).containsExactly(ModeId.CHAT)
    }

  @Test
  fun toggleRightDrawer_setsPanelAndReflectsInState() =
    runTest(dispatcher) {
      val repository = FakeShellStateRepository()
      val actionProvider = createFakeCommandPaletteActionProvider()
      val progressCoordinator = createFakeProgressCenterCoordinator()
      val navigationOperationsUseCase = mockk<NavigationOperationsUseCase>(relaxed = true)
      val connectivityOperationsUseCase = mockk<ConnectivityOperationsUseCase>(relaxed = true)
      val queueJobUseCase = mockk<QueueJobUseCase>(relaxed = true)
      val jobOperationsUseCase = mockk<JobOperationsUseCase>(relaxed = true)
      val undoActionUseCase = mockk<UndoActionUseCase>(relaxed = true)
      val settingsOperationsUseCase = mockk<SettingsOperationsUseCase>(relaxed = true)

      // Set up navigation use case to actually call repository
      every { navigationOperationsUseCase.toggleRightDrawer(any()) } answers
        {
          runBlocking { repository.toggleRightDrawer(firstArg()) }
        }

      val viewModel =
        ShellViewModel(
          repository,
          actionProvider,
          progressCoordinator,
          navigationOperationsUseCase,
          connectivityOperationsUseCase,
          queueJobUseCase,
          jobOperationsUseCase,
          undoActionUseCase,
          settingsOperationsUseCase,
          dispatcher,
        )

      viewModel.toggleRightDrawer(RightPanel.MODEL_SELECTOR)
      advanceUntilIdle()

      val uiState =
        viewModel.uiState.first { state ->
          repository.rightDrawerToggles.contains(RightPanel.MODEL_SELECTOR) &&
            state.layout.activeRightPanel == RightPanel.MODEL_SELECTOR
        }
      assertThat(uiState.layout.isRightDrawerOpen).isTrue()
      assertThat(uiState.layout.activeRightPanel).isEqualTo(RightPanel.MODEL_SELECTOR)
      assertThat(repository.rightDrawerToggles).containsExactly(RightPanel.MODEL_SELECTOR)
    }
}
