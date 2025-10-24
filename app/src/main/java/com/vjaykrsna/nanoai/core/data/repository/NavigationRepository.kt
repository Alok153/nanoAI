package com.vjaykrsna.nanoai.core.data.repository

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import com.vjaykrsna.nanoai.feature.uiux.presentation.CommandPaletteState
import com.vjaykrsna.nanoai.feature.uiux.presentation.ModeId
import com.vjaykrsna.nanoai.feature.uiux.presentation.RecentActivityItem
import com.vjaykrsna.nanoai.feature.uiux.presentation.RightPanel
import com.vjaykrsna.nanoai.feature.uiux.presentation.UndoPayload
import kotlinx.coroutines.flow.Flow

interface NavigationRepository : BaseRepository {
  val commandPaletteState: Flow<CommandPaletteState>

  val recentActivity: Flow<List<RecentActivityItem>>

  val windowSizeClass: Flow<WindowSizeClass>

  val undoPayload: Flow<UndoPayload?>

  fun updateWindowSizeClass(sizeClass: WindowSizeClass)

  suspend fun openMode(modeId: ModeId)

  suspend fun toggleLeftDrawer()

  suspend fun setLeftDrawer(open: Boolean)

  suspend fun toggleRightDrawer(panel: RightPanel)

  suspend fun showCommandPalette(source: com.vjaykrsna.nanoai.feature.uiux.presentation.PaletteSource)

  suspend fun hideCommandPalette()

  suspend fun recordUndoPayload(payload: UndoPayload?)
}
