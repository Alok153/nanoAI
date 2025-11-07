package com.vjaykrsna.nanoai.feature.uiux.presentation

import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityStatus
import com.vjaykrsna.nanoai.core.domain.uiux.ConnectivityOperationsUseCase
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectivityViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension(dispatcher)

  @Test
  fun updateConnectivity_updatesBannerState() =
    runTest(dispatcher) {
      val connectivityOperationsUseCase = mockk<ConnectivityOperationsUseCase>(relaxed = true)

      val viewModel = ConnectivityViewModel(connectivityOperationsUseCase, dispatcher)

      viewModel.updateConnectivity(ConnectivityStatus.ONLINE)
      advanceUntilIdle()

      coVerify(exactly = 1) {
        connectivityOperationsUseCase.updateConnectivity(ConnectivityStatus.ONLINE)
      }
    }
}
