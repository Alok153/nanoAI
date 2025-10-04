package com.vjaykrsna.nanoai.core.network.dto

import com.vjaykrsna.nanoai.core.domain.model.uiux.LayoutSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.ScreenType
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.UserProfile
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for user profile retrieved from `/user/profile` endpoint.
 *
 * Maps between REST API JSON representation and domain [UserProfile] model.
 */
@Serializable
data class UserProfileDto(
  val id: String,
  @SerialName("display_name") val displayName: String? = null,
  @SerialName("theme_preference") val themePreference: String? = null,
  @SerialName("visual_density") val visualDensity: String? = null,
  @SerialName("onboarding_completed") val onboardingCompleted: Boolean? = null,
  @SerialName("dismissed_tips") val dismissedTips: Map<String, Boolean>? = null,
  @SerialName("last_opened_screen") val lastOpenedScreen: String? = null,
  @SerialName("compact_mode") val compactMode: Boolean? = null,
  @SerialName("pinned_tools") val pinnedTools: List<String>? = null,
  @SerialName("saved_layouts") val savedLayouts: List<LayoutSnapshotDto>? = null,
)

/** Data Transfer Object for saved layout snapshot. */
@Serializable
data class LayoutSnapshotDto(
  val id: String,
  val name: String,
  @SerialName("last_opened_screen") val lastOpenedScreen: String,
  @SerialName("pinned_tools") val pinnedTools: List<String>,
  @SerialName("is_compact") val isCompact: Boolean,
)

/** Convert DTO to domain model with safe fallbacks for optional/nullable fields. */
fun UserProfileDto.toDomain(): UserProfile =
  UserProfile(
    id = id,
    displayName = displayName,
    themePreference = themePreference.toThemePreference(),
    visualDensity = visualDensity.toVisualDensity(),
    onboardingCompleted = onboardingCompleted ?: false,
    dismissedTips = dismissedTips ?: emptyMap(),
    lastOpenedScreen = lastOpenedScreen.toScreenType(),
    compactMode = compactMode ?: false,
    pinnedTools = pinnedTools ?: emptyList(),
    savedLayouts = savedLayouts?.map { it.toDomain() } ?: emptyList(),
  )

/** Convert LayoutSnapshotDto to domain LayoutSnapshot. */
fun LayoutSnapshotDto.toDomain(): LayoutSnapshot =
  LayoutSnapshot(
    id = id,
    name = name,
    lastOpenedScreen = lastOpenedScreen,
    pinnedTools = pinnedTools,
    isCompact = isCompact,
  )

/** Convert domain UserProfile to DTO for API requests (if needed for PUT/PATCH). */
fun UserProfile.toDto(): UserProfileDto =
  UserProfileDto(
    id = id,
    displayName = displayName,
    themePreference = themePreference.name,
    visualDensity = visualDensity.name,
    onboardingCompleted = onboardingCompleted,
    dismissedTips = dismissedTips,
    lastOpenedScreen = lastOpenedScreen.name,
    compactMode = compactMode,
    pinnedTools = pinnedTools,
    savedLayouts = savedLayouts.map { it.toDto() },
  )

/** Convert domain LayoutSnapshot to DTO. */
fun LayoutSnapshot.toDto(): LayoutSnapshotDto =
  LayoutSnapshotDto(
    id = id,
    name = name,
    lastOpenedScreen = lastOpenedScreen,
    pinnedTools = pinnedTools,
    isCompact = isCompact,
  )

private fun String?.toThemePreference(): ThemePreference =
  this?.let { ThemePreference.fromName(it) } ?: ThemePreference.SYSTEM

private fun String?.toVisualDensity(): VisualDensity {
  if (this == null) return VisualDensity.DEFAULT
  return VisualDensity.values().firstOrNull { it.name.equals(this, ignoreCase = true) }
    ?: VisualDensity.DEFAULT
}

private fun String?.toScreenType(): ScreenType {
  if (this == null) return ScreenType.HOME
  return ScreenType.values().firstOrNull { it.name.equals(this, ignoreCase = true) }
    ?: ScreenType.HOME
}
