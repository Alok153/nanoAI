package com.vjaykrsna.nanoai.core.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vjaykrsna.nanoai.core.domain.model.uiux.LayoutSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.ScreenType
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.UserProfile
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity

/** Room entity backing the persisted UI/UX user profile record. */
@Entity(tableName = "user_profiles")
data class UserProfileEntity(
  @PrimaryKey @ColumnInfo(name = "user_id") val userId: String,
  @ColumnInfo(name = "display_name") val displayName: String?,
  @ColumnInfo(name = "theme_preference") val themePreference: ThemePreference,
  @ColumnInfo(name = "visual_density") val visualDensity: VisualDensity,
  @ColumnInfo(name = "onboarding_completed") val onboardingCompleted: Boolean,
  @ColumnInfo(name = "dismissed_tips") val dismissedTips: Map<String, Boolean>,
  @ColumnInfo(name = "last_opened_screen") val lastOpenedScreen: ScreenType,
  @ColumnInfo(name = "compact_mode") val compactMode: Boolean,
  @ColumnInfo(name = "pinned_tools") val pinnedTools: List<String>,
)

fun UserProfileEntity.toDomain(savedLayouts: List<LayoutSnapshot> = emptyList()): UserProfile =
  UserProfile(
    id = userId,
    displayName = displayName,
    themePreference = themePreference,
    visualDensity = visualDensity,
    onboardingCompleted = onboardingCompleted,
    dismissedTips = dismissedTips,
    lastOpenedScreen = lastOpenedScreen,
    compactMode = compactMode,
    pinnedTools = pinnedTools,
    savedLayouts = savedLayouts,
  )

fun UserProfile.toEntity(): UserProfileEntity =
  UserProfileEntity(
    userId = id,
    displayName = displayName,
    themePreference = themePreference,
    visualDensity = visualDensity,
    onboardingCompleted = onboardingCompleted,
    dismissedTips = dismissedTips,
    lastOpenedScreen = lastOpenedScreen,
    compactMode = compactMode,
    pinnedTools = pinnedTools,
  )
