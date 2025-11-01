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
class ShellViewModelJobUndoTest {
  private val dispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension(dispatcher)

  @Test
  fun undoAction_callsProgressViewModel() =
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

      val payload = UndoPayload(actionId = "test-action", metadata = mapOf("key" to "value"))

      viewModel.onEvent(ShellUiEvent.Undo(payload))

      verify { progressViewModel.undoAction(payload) }
    }
}
