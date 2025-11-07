package com.vjaykrsna.nanoai.feature.uiux.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityBannerState
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityStatus
import com.vjaykrsna.nanoai.core.domain.uiux.ConnectivityOperationsUseCase
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel responsible for connectivity status and banner management. */
class ConnectivityViewModel
@Inject
constructor(
  private val connectivityOperationsUseCase: ConnectivityOperationsUseCase,
  @Suppress("UnusedPrivateProperty")
  @MainImmediateDispatcher
  private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {

  /** Connectivity banner state for displaying offline/online status and actions. */
  val connectivityBannerState: StateFlow<ConnectivityBannerState> =
    connectivityOperationsUseCase.connectivityBannerState.stateIn(
      scope = viewModelScope,
      started = SharingStarted.Eagerly,
      initialValue = ConnectivityBannerState(status = ConnectivityStatus.ONLINE),
    )

  /** Updates connectivity status and handles online/offline transitions. */
  fun updateConnectivity(status: ConnectivityStatus) {
    viewModelScope.launch(dispatcher) { connectivityOperationsUseCase.updateConnectivity(status) }
  }
}
