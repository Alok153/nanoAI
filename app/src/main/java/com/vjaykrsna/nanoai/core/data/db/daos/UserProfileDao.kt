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
interface UserProfileDao :
  UserProfileObservationDao,
  UserProfileWriteDao,
  UserProfilePreferenceDao,
  UserProfileMaintenanceDao

/** Observation helpers for user profiles. */
interface UserProfileObservationDao {
  @Query("SELECT * FROM user_profiles WHERE user_id = :userId")
  fun observeById(userId: String): Flow<UserProfileEntity?>

  @Query("SELECT * FROM user_profiles WHERE user_id = :userId")
  suspend fun getById(userId: String): UserProfileEntity?

  @Query("SELECT * FROM user_profiles") fun observeAll(): Flow<List<UserProfileEntity>>
}

/** Write helpers for user profiles. */
interface UserProfileWriteDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(profile: UserProfileEntity)

  @Update suspend fun update(profile: UserProfileEntity): Int
}

/** Preference update helpers for user profiles. */
interface UserProfilePreferenceDao {
  @Query("UPDATE user_profiles SET theme_preference = :themePreference WHERE user_id = :userId")
  suspend fun updateThemePreference(userId: String, themePreference: ThemePreference): Int

  @Query("UPDATE user_profiles SET visual_density = :visualDensity WHERE user_id = :userId")
  suspend fun updateVisualDensity(userId: String, visualDensity: VisualDensity): Int

  @Query("UPDATE user_profiles SET last_opened_screen = :screenType WHERE user_id = :userId")
  suspend fun updateLastOpenedScreen(userId: String, screenType: ScreenType): Int

  @Query("UPDATE user_profiles SET compact_mode = :compactMode WHERE user_id = :userId")
  suspend fun updateCompactMode(userId: String, compactMode: Boolean): Int

  @Query("UPDATE user_profiles SET pinned_tools = :pinnedTools WHERE user_id = :userId")
  suspend fun updatePinnedTools(userId: String, pinnedTools: List<String>): Int

  @Query("UPDATE user_profiles SET display_name = :displayName WHERE user_id = :userId")
  suspend fun updateDisplayName(userId: String, displayName: String?): Int
}

/** Maintenance helpers for user profiles. */
interface UserProfileMaintenanceDao {
  @Query("DELETE FROM user_profiles WHERE user_id = :userId")
  suspend fun deleteById(userId: String): Int
}
