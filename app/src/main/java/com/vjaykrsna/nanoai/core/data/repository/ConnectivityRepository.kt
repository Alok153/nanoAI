package com.vjaykrsna.nanoai.core.data.repository

import com.vjaykrsna.nanoai.feature.uiux.presentation.ConnectivityBannerState
import com.vjaykrsna.nanoai.feature.uiux.presentation.ConnectivityStatus
import kotlinx.coroutines.flow.Flow

interface ConnectivityRepository : BaseRepository {
  val connectivityBannerState: Flow<ConnectivityBannerState>

  suspend fun updateConnectivity(status: ConnectivityStatus)
}
