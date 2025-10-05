package com.vjaykrsna.nanoai.core.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.vjaykrsna.nanoai.core.domain.model.uiux.UIStateSnapshot

/** Room entity storing restoration-ready UI state for a user session. */
@Entity(
  tableName = "ui_state_snapshots",
  foreignKeys =
    [
      ForeignKey(
        entity = UserProfileEntity::class,
        parentColumns = ["user_id"],
        childColumns = ["user_id"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE,
      ),
    ],
)
data class UIStateSnapshotEntity(
  @PrimaryKey @ColumnInfo(name = "user_id") val userId: String,
  @ColumnInfo(name = "expanded_panels") val expandedPanels: List<String>,
  @ColumnInfo(name = "recent_actions") val recentActions: List<String>,
  @ColumnInfo(name = "sidebar_collapsed") val sidebarCollapsed: Boolean,
  @ColumnInfo(name = "left_drawer_open") val leftDrawerOpen: Boolean,
  @ColumnInfo(name = "right_drawer_open") val rightDrawerOpen: Boolean,
  @ColumnInfo(name = "active_mode") val activeMode: String,
  @ColumnInfo(name = "active_right_panel") val activeRightPanel: String?,
  @ColumnInfo(name = "palette_visible") val paletteVisible: Boolean,
)

fun UIStateSnapshotEntity.toDomain(): UIStateSnapshot =
  UIStateSnapshot(
    userId = userId,
    expandedPanels = expandedPanels,
    recentActions = recentActions,
    isSidebarCollapsed = sidebarCollapsed,
    isLeftDrawerOpen = leftDrawerOpen,
    isRightDrawerOpen = rightDrawerOpen,
    activeModeRoute = activeMode,
    activeRightPanel = activeRightPanel,
    isCommandPaletteVisible = paletteVisible,
  )

fun UIStateSnapshot.toEntity(): UIStateSnapshotEntity =
  UIStateSnapshotEntity(
    userId = userId,
    expandedPanels = expandedPanels,
    recentActions = recentActions,
    sidebarCollapsed = isSidebarCollapsed,
    leftDrawerOpen = isLeftDrawerOpen,
    rightDrawerOpen = isRightDrawerOpen,
    activeMode = activeModeRoute,
    activeRightPanel = activeRightPanel,
    paletteVisible = isCommandPaletteVisible,
  )
