package com.vjaykrsna.nanoai.feature.uiux.presentation

import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityBannerState
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityStatus
import com.vjaykrsna.nanoai.core.domain.uiux.ConnectivityOperationsUseCase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Coordinator responsible for connectivity status and banner management.
 *
 * This is a regular injectable class (not a ViewModel) because it's composed into ShellViewModel
 * rather than used independently with ViewModelProvider.
 */
@Singleton
class ConnectivityCoordinator
@Inject
constructor(
  private val connectivityOperationsUseCase: ConnectivityOperationsUseCase,
  @Suppress("UnusedPrivateProperty")
  @MainImmediateDispatcher
  private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) {

  /** Connectivity banner state for displaying offline/online status and actions. */
  fun connectivityBannerState(scope: CoroutineScope): StateFlow<ConnectivityBannerState> =
    connectivityOperationsUseCase.connectivityBannerState.stateIn(
      scope = scope,
      started = SharingStarted.Eagerly,
      initialValue = ConnectivityBannerState(status = ConnectivityStatus.ONLINE),
    )

  /** Updates connectivity status and handles online/offline transitions. */
  fun updateConnectivity(scope: CoroutineScope, status: ConnectivityStatus) {
    scope.launch(dispatcher) { connectivityOperationsUseCase.updateConnectivity(status) }
  }
}
