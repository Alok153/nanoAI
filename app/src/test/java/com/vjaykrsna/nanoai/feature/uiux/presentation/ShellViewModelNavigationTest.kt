package com.vjaykrsna.nanoai.feature.uiux.presentation

import com.vjaykrsna.nanoai.feature.uiux.domain.NavigationOperationsUseCase
import com.vjaykrsna.nanoai.shared.ui.shell.ShellUiEvent
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class ShellViewModelNavigationTest {
  private val dispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension(dispatcher)

  @Test
  fun openMode_callsNavigationViewModel() =
    runTest(dispatcher) {
      val navigationOperationsUseCase = mockk<NavigationOperationsUseCase>(relaxed = true)

      // Mock sub-ViewModels
      val navigationViewModel = mockk<NavigationViewModel>(relaxed = true)
      val connectivityViewModel = mockk<ConnectivityViewModel>(relaxed = true)
      val progressViewModel = mockk<ProgressViewModel>(relaxed = true)
      val themeViewModel = mockk<ThemeViewModel>(relaxed = true)

      val viewModel =
        ShellViewModel(
          navigationOperationsUseCase,
          navigationViewModel,
          connectivityViewModel,
          progressViewModel,
          themeViewModel,
          dispatcher,
        )

      viewModel.onEvent(ShellUiEvent.ModeSelected(ModeId.CHAT))

      verify { navigationViewModel.openMode(ModeId.CHAT) }
    }

  @Test
  fun toggleRightDrawer_callsNavigationViewModel() =
    runTest(dispatcher) {
      val navigationOperationsUseCase = mockk<NavigationOperationsUseCase>(relaxed = true)

      // Mock sub-ViewModels
      val navigationViewModel = mockk<NavigationViewModel>(relaxed = true)
      val connectivityViewModel = mockk<ConnectivityViewModel>(relaxed = true)
      val progressViewModel = mockk<ProgressViewModel>(relaxed = true)
      val themeViewModel = mockk<ThemeViewModel>(relaxed = true)

      val viewModel =
        ShellViewModel(
          navigationOperationsUseCase,
          navigationViewModel,
          connectivityViewModel,
          progressViewModel,
          themeViewModel,
          dispatcher,
        )

      viewModel.onEvent(ShellUiEvent.ToggleRightDrawer(RightPanel.MODEL_SELECTOR))

      verify { navigationViewModel.toggleRightDrawer(RightPanel.MODEL_SELECTOR) }
    }
}
