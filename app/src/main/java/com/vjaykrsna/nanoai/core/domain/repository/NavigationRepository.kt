package com.vjaykrsna.nanoai.core.domain.repository

import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandPaletteState
import com.vjaykrsna.nanoai.core.domain.model.uiux.ModeId
import com.vjaykrsna.nanoai.core.domain.model.uiux.PaletteSource
import com.vjaykrsna.nanoai.core.domain.model.uiux.RecentActivityItem
import com.vjaykrsna.nanoai.core.domain.model.uiux.RightPanel
import com.vjaykrsna.nanoai.core.domain.model.uiux.ShellWindowSizeClass
import com.vjaykrsna.nanoai.core.domain.model.uiux.UndoPayload
import kotlinx.coroutines.flow.Flow

interface NavigationRepository : BaseRepository {
  val commandPaletteState: Flow<CommandPaletteState>

  val recentActivity: Flow<List<RecentActivityItem>>

  val windowSizeClass: Flow<ShellWindowSizeClass>

  val undoPayload: Flow<UndoPayload?>

  fun updateWindowSizeClass(sizeClass: ShellWindowSizeClass)

  suspend fun openMode(modeId: ModeId)

  suspend fun toggleLeftDrawer()

  suspend fun setLeftDrawer(open: Boolean)

  suspend fun toggleRightDrawer(panel: RightPanel)

  suspend fun showCommandPalette(source: com.vjaykrsna.nanoai.core.domain.model.uiux.PaletteSource)

  suspend fun hideCommandPalette()

  suspend fun recordUndoPayload(payload: UndoPayload?)
}
