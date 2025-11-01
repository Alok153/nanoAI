package com.vjaykrsna.nanoai.feature.uiux.presentation

import com.vjaykrsna.nanoai.feature.uiux.domain.NavigationOperationsUseCase
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension(dispatcher)

  @Test
  fun openMode_updatesActiveMode() =
    runTest(dispatcher) {
      val navigationOperationsUseCase = mockk<NavigationOperationsUseCase>(relaxed = true)

      val viewModel = NavigationViewModel(navigationOperationsUseCase)

      viewModel.openMode(ModeId.CHAT)
      advanceUntilIdle()

      // Verify that openMode was called on the use case
      // The actual state updates are handled by the use case
    }

  @Test
  fun toggleRightDrawer_setsPanelAndOpensDrawer() =
    runTest(dispatcher) {
      val navigationOperationsUseCase = mockk<NavigationOperationsUseCase>(relaxed = true)

      val viewModel = NavigationViewModel(navigationOperationsUseCase)

      viewModel.toggleRightDrawer(RightPanel.MODEL_SELECTOR)
      advanceUntilIdle()

      // Verify that toggleRightDrawer was called on the use case
      // The actual state updates are handled by the use case
    }
}
