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
@Suppress("TooManyFunctions") // DAOs naturally have many CRUD operations
interface UIStateSnapshotDao {
  /**
   * Observe the UI state snapshot for a user.
   *
   * @param userId The unique user identifier
   * @return Flow emitting the UI state snapshot or null if not found
   */
  @Query("SELECT * FROM ui_state_snapshots WHERE user_id = :userId")
  fun observeByUserId(userId: String): Flow<UIStateSnapshotEntity?>

  /**
   * Get the UI state snapshot for a user (one-shot query).
   *
   * @param userId The unique user identifier
   * @return The UI state snapshot or null if not found
   */
  @Query("SELECT * FROM ui_state_snapshots WHERE user_id = :userId")
  suspend fun getByUserId(userId: String): UIStateSnapshotEntity?

  /**
   * Insert or replace a UI state snapshot.
   *
   * @param snapshot The UI state snapshot to insert
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(snapshot: UIStateSnapshotEntity)

  /**
   * Update an existing UI state snapshot.
   *
   * @param snapshot The UI state snapshot with updated fields
   * @return Number of rows updated (should be 1 if successful)
   */
  @Update suspend fun update(snapshot: UIStateSnapshotEntity): Int

  /**
   * Delete the UI state snapshot for a user.
   *
   * @param userId The unique user identifier
   * @return Number of rows deleted (should be 1 if successful)
   */
  @Query("DELETE FROM ui_state_snapshots WHERE user_id = :userId")
  suspend fun deleteByUserId(userId: String): Int

  /**
   * Update the sidebar collapsed state.
   *
   * @param userId The unique user identifier
   * @param collapsed True if sidebar is collapsed
   * @return Number of rows updated (should be 1 if successful)
   */
  @Query("UPDATE ui_state_snapshots SET sidebar_collapsed = :collapsed WHERE user_id = :userId")
  suspend fun updateSidebarCollapsed(userId: String, collapsed: Boolean): Int

  /**
   * Update the left drawer open state.
   *
   * @param userId The unique user identifier
   * @param open True if the left drawer should be marked as open
   * @return Number of rows updated (should be 1 if successful)
   */
  @Query("UPDATE ui_state_snapshots SET left_drawer_open = :open WHERE user_id = :userId")
  suspend fun updateLeftDrawerOpen(userId: String, open: Boolean): Int

  /**
   * Update the right drawer open state and active panel.
   *
   * @param userId The unique user identifier
   * @param open True if the right drawer should be marked as open
   * @param panel Identifier for the active panel when open
   * @return Number of rows updated (should be 1 if successful)
   */
  @Query(
    "UPDATE ui_state_snapshots SET right_drawer_open = :open, active_right_panel = :panel WHERE user_id = :userId",
  )
  suspend fun updateRightDrawerState(userId: String, open: Boolean, panel: String?): Int

  /**
   * Update the active mode route used for session restoration.
   *
   * @param userId The unique user identifier
   * @param route Navigation route string representing the active mode
   * @return Number of rows updated (should be 1 if successful)
   */
  @Query("UPDATE ui_state_snapshots SET active_mode = :route WHERE user_id = :userId")
  suspend fun updateActiveModeRoute(userId: String, route: String): Int

  /**
   * Update the command palette visibility flag.
   *
   * @param userId The unique user identifier
   * @param visible True if the palette should be shown on restoration
   * @return Number of rows updated (should be 1 if successful)
   */
  @Query("UPDATE ui_state_snapshots SET palette_visible = :visible WHERE user_id = :userId")
  suspend fun updateCommandPaletteVisible(userId: String, visible: Boolean): Int

  /**
   * Update the expanded panels list.
   *
   * @param userId The unique user identifier
   * @param expandedPanels List of expanded panel IDs
   * @return Number of rows updated (should be 1 if successful)
   */
  @Query("UPDATE ui_state_snapshots SET expanded_panels = :expandedPanels WHERE user_id = :userId")
  suspend fun updateExpandedPanels(userId: String, expandedPanels: List<String>): Int

  /**
   * Update the recent actions list.
   *
   * @param userId The unique user identifier
   * @param recentActions List of recent action IDs (max 5 items)
   * @return Number of rows updated (should be 1 if successful)
   */
  @Query("UPDATE ui_state_snapshots SET recent_actions = :recentActions WHERE user_id = :userId")
  suspend fun updateRecentActions(userId: String, recentActions: List<String>): Int

  /**
   * Add a panel to the expanded panels list.
   *
   * This transaction reads the current expanded panels, adds the new panel if not present, and
   * updates the list.
   *
   * @param userId The unique user identifier
   * @param panelId The panel ID to add
   */
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

  /**
   * Remove a panel from the expanded panels list.
   *
   * This transaction reads the current expanded panels, removes the panel if present, and updates
   * the list.
   *
   * @param userId The unique user identifier
   * @param panelId The panel ID to remove
   */
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

  /**
   * Add an action to recent actions, rotating if max size (5) is reached.
   *
   * This transaction reads the current recent actions, adds the new action at the front, removes it
   * from later positions if present (dedupe), truncates to max 5 items, and updates the list.
   *
   * @param userId The unique user identifier
   * @param actionId The action ID to add
   */
  @Transaction
  suspend fun addRecentAction(userId: String, actionId: String) {
    val current = getByUserId(userId)
    if (current != null) {
      val updated = current.recentActions.toMutableList()
      // Remove existing occurrence (dedupe)
      updated.remove(actionId)
      // Add to front
      updated.add(0, actionId)
      // Keep only last allowed recent actions
      val trimmed = updated.take(MAX_RECENT_ACTIONS)
      updateRecentActions(userId, trimmed)
    } else {
      // Initialize with single action if no snapshot exists yet
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
        ),
      )
    }
  }

  /**
   * Clear all recent actions for a user.
   *
   * @param userId The unique user identifier
   * @return Number of rows updated (should be 1 if successful)
   */
  @Query("UPDATE ui_state_snapshots SET recent_actions = '' WHERE user_id = :userId")
  suspend fun clearRecentActions(userId: String): Int

  /**
   * Clear all expanded panels for a user.
   *
   * @param userId The unique user identifier
   * @return Number of rows updated (should be 1 if successful)
   */
  @Query("UPDATE ui_state_snapshots SET expanded_panels = '' WHERE user_id = :userId")
  suspend fun clearExpandedPanels(userId: String): Int

  /**
   * Reset UI state to defaults (collapse sidebar, clear panels and actions).
   *
   * @param userId The unique user identifier
   */
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
        ),
      )
    }
  }
}
