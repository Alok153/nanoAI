package com.vjaykrsna.nanoai.feature.uiux.data

import com.vjaykrsna.nanoai.feature.uiux.state.CommandPaletteState
import com.vjaykrsna.nanoai.feature.uiux.state.ConnectivityBannerState
import com.vjaykrsna.nanoai.feature.uiux.state.ConnectivityStatus
import com.vjaykrsna.nanoai.feature.uiux.state.ModeId
import com.vjaykrsna.nanoai.feature.uiux.state.PaletteSource
import com.vjaykrsna.nanoai.feature.uiux.state.ProgressJob
import com.vjaykrsna.nanoai.feature.uiux.state.RecentActivityItem
import com.vjaykrsna.nanoai.feature.uiux.state.RightPanel
import com.vjaykrsna.nanoai.feature.uiux.state.ShellLayoutState
import com.vjaykrsna.nanoai.feature.uiux.state.UiPreferenceSnapshot
import com.vjaykrsna.nanoai.feature.uiux.state.UndoPayload
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Repository surface that coordinates shell state persistence and observations. */
open class ShellStateRepository @Inject constructor() {
  open val shellLayoutState: Flow<ShellLayoutState>
    get() = TODO("Phase 3.3 will expose shell layout state")

  open val commandPaletteState: Flow<CommandPaletteState>
    get() = TODO("Phase 3.3 will expose command palette state")

  open val connectivityBannerState: Flow<ConnectivityBannerState>
    get() = TODO("Phase 3.3 will expose connectivity banner state")

  open val uiPreferenceSnapshot: Flow<UiPreferenceSnapshot>
    get() = TODO("Phase 3.3 will expose UI preferences")

  open val recentActivity: Flow<List<RecentActivityItem>>
    get() = TODO("Phase 3.3 will expose recent activity")

  open suspend fun openMode(modeId: ModeId) {
    TODO("Phase 3.3 will persist mode changes")
  }

  open suspend fun toggleLeftDrawer() {
    TODO("Phase 3.3 will persist drawer state")
  }

  open suspend fun toggleRightDrawer(panel: RightPanel) {
    TODO("Phase 3.3 will persist drawer state")
  }

  open suspend fun showCommandPalette(source: PaletteSource) {
    TODO("Phase 3.3 will manage command palette visibility")
  }

  open suspend fun hideCommandPalette() {
    TODO("Phase 3.3 will manage command palette visibility")
  }

  open suspend fun queueJob(job: ProgressJob) {
    TODO("Phase 3.3 will enqueue jobs")
  }

  open suspend fun completeJob(jobId: UUID) {
    TODO("Phase 3.3 will complete jobs")
  }

  open suspend fun updateConnectivity(status: ConnectivityStatus) {
    TODO("Phase 3.3 will update connectivity")
  }

  open suspend fun recordUndoPayload(payload: UndoPayload?) {
    TODO("Phase 3.3 will persist undo payload")
  }
}
