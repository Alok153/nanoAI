package com.vjaykrsna.nanoai.core.data.repository

import com.vjaykrsna.nanoai.feature.uiux.state.ConnectivityBannerState
import com.vjaykrsna.nanoai.feature.uiux.state.ConnectivityStatus
import kotlinx.coroutines.flow.Flow

interface ConnectivityRepository : BaseRepository {
  val connectivityBannerState: Flow<ConnectivityBannerState>

  suspend fun updateConnectivity(status: ConnectivityStatus)
}
