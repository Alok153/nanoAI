package com.vjaykrsna.nanoai.core.data.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.vjaykrsna.nanoai.core.data.db.entities.UIStateSnapshotEntity
import com.vjaykrsna.nanoai.core.domain.model.uiux.UIStateSnapshot
import kotlinx.coroutines.flow.Flow

private const val MAX_RECENT_ACTIONS = 5

/**
 * Data Access Object for [UIStateSnapshotEntity].
 *
 * Provides reactive Flow-based operations for session restoration, sidebar state management, and
 * recent actions tracking.
 */
@Dao
interface UIStateSnapshotDao :
  UIStateSnapshotObservationDao,
  UIStateSnapshotWriteDao,
  UIStateSnapshotDrawerDao,
  UIStateSnapshotPaletteDao,
  UIStateSnapshotPanelDao,
  UIStateSnapshotRecentActionsDao,
  UIStateSnapshotResetDao

/** Observation helpers for UI state snapshots. */
interface UIStateSnapshotObservationDao {
  @Query("SELECT * FROM ui_state_snapshots WHERE user_id = :userId")
  fun observeByUserId(userId: String): Flow<UIStateSnapshotEntity?>

  @Query("SELECT * FROM ui_state_snapshots WHERE user_id = :userId")
  suspend fun getByUserId(userId: String): UIStateSnapshotEntity?
}

/** Write helpers for UI state snapshots. */
interface UIStateSnapshotWriteDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(snapshot: UIStateSnapshotEntity)

  @Update suspend fun update(snapshot: UIStateSnapshotEntity): Int

  @Query("DELETE FROM ui_state_snapshots WHERE user_id = :userId")
  suspend fun deleteByUserId(userId: String): Int
}

/** Drawer and navigation state updates. */
interface UIStateSnapshotDrawerDao {
  @Query("UPDATE ui_state_snapshots SET sidebar_collapsed = :collapsed WHERE user_id = :userId")
  suspend fun updateSidebarCollapsed(userId: String, collapsed: Boolean): Int

  @Query("UPDATE ui_state_snapshots SET left_drawer_open = :open WHERE user_id = :userId")
  suspend fun updateLeftDrawerOpen(userId: String, open: Boolean): Int

  @Query(
    """
      UPDATE ui_state_snapshots
      SET right_drawer_open = :open,
          active_right_panel = :panel
      WHERE user_id = :userId
    """
  )
  suspend fun updateRightDrawerState(userId: String, open: Boolean, panel: String?): Int

  @Query("UPDATE ui_state_snapshots SET active_mode = :route WHERE user_id = :userId")
  suspend fun updateActiveModeRoute(userId: String, route: String): Int
}

/** Command palette state updates. */
interface UIStateSnapshotPaletteDao {
  @Query("UPDATE ui_state_snapshots SET palette_visible = :visible WHERE user_id = :userId")
  suspend fun updateCommandPaletteVisible(userId: String, visible: Boolean): Int
}

/** Panel state updates, including transactional helpers. */
interface UIStateSnapshotPanelDao : UIStateSnapshotObservationDao, UIStateSnapshotWriteDao {
  @Query("UPDATE ui_state_snapshots SET expanded_panels = :expandedPanels WHERE user_id = :userId")
  suspend fun updateExpandedPanels(userId: String, expandedPanels: List<String>): Int

  @Query("UPDATE ui_state_snapshots SET expanded_panels = '' WHERE user_id = :userId")
  suspend fun clearExpandedPanels(userId: String): Int

  @Transaction
  suspend fun addExpandedPanel(userId: String, panelId: String) {
    val current = getByUserId(userId)
    if (current != null) {
      val updated = current.expandedPanels.toMutableList()
      if (!updated.contains(panelId)) {
        updated.add(panelId)
        updateExpandedPanels(userId, updated)
      }
    }
  }

  @Transaction
  suspend fun removeExpandedPanel(userId: String, panelId: String) {
    val current = getByUserId(userId)
    if (current != null) {
      val updated = current.expandedPanels.toMutableList()
      if (updated.remove(panelId)) {
        updateExpandedPanels(userId, updated)
      }
    }
  }
}

/** Recent action updates, including transactional helpers. */
interface UIStateSnapshotRecentActionsDao : UIStateSnapshotObservationDao, UIStateSnapshotWriteDao {
  @Query("UPDATE ui_state_snapshots SET recent_actions = :recentActions WHERE user_id = :userId")
  suspend fun updateRecentActions(userId: String, recentActions: List<String>): Int

  @Query("UPDATE ui_state_snapshots SET recent_actions = '' WHERE user_id = :userId")
  suspend fun clearRecentActions(userId: String): Int

  @Transaction
  suspend fun addRecentAction(userId: String, actionId: String) {
    val current = getByUserId(userId)
    if (current != null) {
      val updated = current.recentActions.toMutableList()
      updated.remove(actionId)
      updated.add(0, actionId)
      val trimmed = updated.take(MAX_RECENT_ACTIONS)
      updateRecentActions(userId, trimmed)
    } else {
      insert(
        UIStateSnapshotEntity(
          userId = userId,
          expandedPanels = emptyList(),
          recentActions = listOf(actionId),
          sidebarCollapsed = false,
          leftDrawerOpen = false,
          rightDrawerOpen = false,
          activeMode = UIStateSnapshot.DEFAULT_MODE_ROUTE,
          activeRightPanel = null,
          paletteVisible = false,
        )
      )
    }
  }
}

/** Reset helpers for restoring baseline UI state. */
interface UIStateSnapshotResetDao : UIStateSnapshotObservationDao, UIStateSnapshotWriteDao {
  @Transaction
  suspend fun resetToDefaults(userId: String) {
    val current = getByUserId(userId)
    if (current != null) {
      update(
        current.copy(
          expandedPanels = emptyList(),
          recentActions = emptyList(),
          sidebarCollapsed = false,
          leftDrawerOpen = false,
          rightDrawerOpen = false,
          activeMode = UIStateSnapshot.DEFAULT_MODE_ROUTE,
          activeRightPanel = null,
          paletteVisible = false,
        )
      )
    }
  }
}
