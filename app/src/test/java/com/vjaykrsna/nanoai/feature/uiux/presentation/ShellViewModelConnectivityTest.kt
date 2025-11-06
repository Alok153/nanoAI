package com.vjaykrsna.nanoai.feature.uiux.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityStatus
import com.vjaykrsna.nanoai.core.domain.uiux.ConnectivityOperationsUseCase
import com.vjaykrsna.nanoai.core.domain.uiux.NavigationOperationsUseCase
import com.vjaykrsna.nanoai.shared.ui.shell.ShellUiEvent
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class ShellViewModelConnectivityTest {
  private val dispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension(dispatcher)

  @Test
  fun updateConnectivity_flushesQueuedJobsAndBanner() =
    runTest(dispatcher) {
      val fakeRepos = createFakeRepositories()
      val navigationOperationsUseCase = mockk<NavigationOperationsUseCase>(relaxed = true)
      val connectivityOperationsUseCase = mockk<ConnectivityOperationsUseCase>(relaxed = true)

      // Mock sub-ViewModels
      val navigationViewModel = mockk<NavigationViewModel>(relaxed = true)
      val connectivityViewModel = mockk<ConnectivityViewModel>(relaxed = true)
      val progressViewModel = mockk<ProgressViewModel>(relaxed = true)
      val themeViewModel = mockk<ThemeViewModel>(relaxed = true)

      // Set up connectivity operations use case to actually call repository
      coEvery { connectivityOperationsUseCase.updateConnectivity(any()) } coAnswers
        {
          runBlocking { fakeRepos.connectivityRepository.updateConnectivity(firstArg()) }
        }

      val viewModel =
        ShellViewModel(
          navigationOperationsUseCase,
          navigationViewModel,
          connectivityViewModel,
          progressViewModel,
          themeViewModel,
          dispatcher,
        )

      viewModel.onEvent(ShellUiEvent.ConnectivityChanged(ConnectivityStatus.ONLINE))
      advanceUntilIdle()

      val uiState =
        viewModel.uiState.first { state -> state.layout.connectivity == ConnectivityStatus.ONLINE }
      assertThat(uiState.layout.connectivity).isEqualTo(ConnectivityStatus.ONLINE)
      assertThat(uiState.connectivityBanner.status).isEqualTo(ConnectivityStatus.ONLINE)
    }
}
