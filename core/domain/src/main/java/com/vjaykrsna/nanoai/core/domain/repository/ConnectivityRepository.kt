package com.vjaykrsna.nanoai.core.domain.repository

import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityBannerState
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityStatus
import kotlinx.coroutines.flow.Flow

interface ConnectivityRepository : BaseRepository {
  val connectivityBannerState: Flow<ConnectivityBannerState>

  suspend fun updateConnectivity(status: ConnectivityStatus)
}
