package com.vjaykrsna.nanoai.core.domain.uiux

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityBannerState
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityStatus
import com.vjaykrsna.nanoai.core.domain.repository.ConnectivityRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** Consolidated connectivity operations for network status management. */
class ConnectivityOperationsUseCase
@Inject
constructor(
  private val repository: ConnectivityRepository,
  @IoDispatcher private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  private val scope = CoroutineScope(SupervisorJob() + dispatcher)

  val connectivityBannerState: Flow<ConnectivityBannerState> = repository.connectivityBannerState

  /** Updates connectivity status and handles online/offline transitions. */
  fun updateConnectivity(status: ConnectivityStatus) {
    scope.launch { repository.updateConnectivity(status) }
  }
}
