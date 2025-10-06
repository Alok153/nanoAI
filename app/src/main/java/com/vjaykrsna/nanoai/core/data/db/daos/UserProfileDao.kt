package com.vjaykrsna.nanoai.core.data.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vjaykrsna.nanoai.core.data.db.entities.UserProfileEntity
import com.vjaykrsna.nanoai.core.domain.model.uiux.ScreenType
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [UserProfileEntity].
 *
 * Provides reactive Flow-based CRUD operations, pinned tools management, and UI preference updates
 * for the user profile.
 */
@Dao
interface UserProfileDao {
  /**
   * Observe a single user profile by ID.
   *
   * @param userId The unique user identifier
   * @return Flow emitting the user profile or null if not found
   */
  @Query("SELECT * FROM user_profiles WHERE user_id = :userId")
  fun observeById(userId: String): Flow<UserProfileEntity?>

  /**
   * Get a single user profile by ID (one-shot query).
   *
   * @param userId The unique user identifier
   * @return The user profile or null if not found
   */
  @Query("SELECT * FROM user_profiles WHERE user_id = :userId")
  suspend fun getById(userId: String): UserProfileEntity?

  /**
   * Observe all user profiles (typically one, but supports multi-user scenarios).
   *
   * @return Flow emitting list of all user profiles
   */
  @Query("SELECT * FROM user_profiles") fun observeAll(): Flow<List<UserProfileEntity>>

  /**
   * Insert or replace a user profile.
   *
   * @param profile The user profile to insert
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(profile: UserProfileEntity)

  /**
   * Update an existing user profile.
   *
   * @param profile The user profile with updated fields
   * @return Number of rows updated (should be 1 if successful)
   */
  @Update suspend fun update(profile: UserProfileEntity): Int

  /**
   * Delete a user profile by ID.
   *
   * @param userId The unique user identifier
   * @return Number of rows deleted (should be 1 if successful)
   */
  @Query("DELETE FROM user_profiles WHERE user_id = :userId")
  suspend fun deleteById(userId: String): Int

  /**
   * Update the theme preference for a user.
   *
   * @param userId The unique user identifier
   * @param themePreference The new theme preference
   * @return Number of rows updated (should be 1 if successful)
   */
  @Query("UPDATE user_profiles SET theme_preference = :themePreference WHERE user_id = :userId")
  suspend fun updateThemePreference(userId: String, themePreference: ThemePreference): Int

  /**
   * Update the visual density for a user.
   *
   * @param userId The unique user identifier
   * @param visualDensity The new visual density
   * @return Number of rows updated (should be 1 if successful)
   */
  @Query("UPDATE user_profiles SET visual_density = :visualDensity WHERE user_id = :userId")
  suspend fun updateVisualDensity(userId: String, visualDensity: VisualDensity): Int

  /**
   * Update the onboarding completed flag for a user.
   *
   * @param userId The unique user identifier
   * @param completed True if onboarding is completed
   * @return Number of rows updated (should be 1 if successful)
   */
  @Query("UPDATE user_profiles SET onboarding_completed = :completed WHERE user_id = :userId")
  suspend fun updateOnboardingCompleted(userId: String, completed: Boolean): Int

  /**
   * Update the dismissed tips map for a user.
   *
   * @param userId The unique user identifier
   * @param dismissedTips Map of tip IDs to dismissed status
   * @return Number of rows updated (should be 1 if successful)
   */
  @Query("UPDATE user_profiles SET dismissed_tips = :dismissedTips WHERE user_id = :userId")
  suspend fun updateDismissedTips(userId: String, dismissedTips: Map<String, Boolean>): Int

  /**
   * Update the last opened screen for a user.
   *
   * @param userId The unique user identifier
   * @param screenType The last opened screen type
   * @return Number of rows updated (should be 1 if successful)
   */
  @Query("UPDATE user_profiles SET last_opened_screen = :screenType WHERE user_id = :userId")
  suspend fun updateLastOpenedScreen(userId: String, screenType: ScreenType): Int

  /**
   * Update the compact mode flag for a user.
   *
   * @param userId The unique user identifier
   * @param compactMode True to enable compact mode
   * @return Number of rows updated (should be 1 if successful)
   */
  @Query("UPDATE user_profiles SET compact_mode = :compactMode WHERE user_id = :userId")
  suspend fun updateCompactMode(userId: String, compactMode: Boolean): Int

  /**
   * Update the pinned tools list for a user.
   *
   * @param userId The unique user identifier
   * @param pinnedTools List of tool IDs (max 10 items)
   * @return Number of rows updated (should be 1 if successful)
   */
  @Query("UPDATE user_profiles SET pinned_tools = :pinnedTools WHERE user_id = :userId")
  suspend fun updatePinnedTools(userId: String, pinnedTools: List<String>): Int

  /**
   * Update the display name for a user.
   *
   * @param userId The unique user identifier
   * @param displayName The new display name (max 50 characters)
   * @return Number of rows updated (should be 1 if successful)
   */
  @Query("UPDATE user_profiles SET display_name = :displayName WHERE user_id = :userId")
  suspend fun updateDisplayName(userId: String, displayName: String?): Int
}
