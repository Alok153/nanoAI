package com.vjaykrsna.nanoai.core.device

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityStatus
import com.vjaykrsna.nanoai.core.domain.repository.ConnectivityRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/** Emits connectivity changes and updates the repository for banner state. */
interface ConnectivityObserver {
  val status: SharedFlow<ConnectivityStatus>

  fun onStatusChanged(status: ConnectivityStatus)
}

@Singleton
class SharedFlowConnectivityObserver
@Inject
constructor(
  private val connectivityRepository: ConnectivityRepository,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ConnectivityObserver {

  private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
  private val current = MutableStateFlow(ConnectivityStatus.ONLINE)
  private val shared = MutableSharedFlow<ConnectivityStatus>(replay = 1)

  init {
    shared.tryEmit(current.value)
  }

  override val status: SharedFlow<ConnectivityStatus> = shared.asSharedFlow()

  override fun onStatusChanged(status: ConnectivityStatus) {
    if (status == current.value) return

    current.value = status
    scope.launch { shared.emit(status) }
    scope.launch { connectivityRepository.updateConnectivity(status) }
  }
}
