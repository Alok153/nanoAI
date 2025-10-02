package com.vjaykrsna.nanoai.core.domain.model.uiux

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

/**
 * Aggregate domain model capturing persisted UI/UX preferences and layout state for a user.
 */
data class UserProfile(
    val id: String,
    val displayName: String?,
    val themePreference: ThemePreference,
    val visualDensity: VisualDensity,
    val onboardingCompleted: Boolean,
    var dismissedTips: Map<String, Boolean>,
    val lastOpenedScreen: ScreenType,
    val compactMode: Boolean,
    var pinnedTools: List<String>,
    var savedLayouts: List<LayoutSnapshot>,
) {
    init {
        require(id.isNotBlank()) { "UserProfile id cannot be blank." }
        displayName?.let {
            require(it.length <= MAX_DISPLAY_NAME) {
                "Display name must be ≤ $MAX_DISPLAY_NAME characters."
            }
        }

        dismissedTips = sanitizeDismissedTips(dismissedTips)
        pinnedTools = sanitizePinnedTools(pinnedTools)
        savedLayouts = sanitizeSavedLayouts(savedLayouts)

        if (compactMode) {
            require(visualDensity == VisualDensity.COMPACT) {
                "Compact mode requires visual density COMPACT to remain consistent with UI expectations."
            }
        }
    }

    /** Updates layouts while re-validating invariants. */
    fun withLayouts(layouts: List<LayoutSnapshot>): UserProfile = copy(savedLayouts = sanitizeSavedLayouts(layouts))

    /** Updates pinned tools while preserving ordering and validation guarantees. */
    fun withPinnedTools(tools: List<String>): UserProfile = copy(pinnedTools = sanitizePinnedTools(tools))

    /** Updates dismissed tips, ensuring non-blank identifiers. */
    fun withDismissedTips(tips: Map<String, Boolean>): UserProfile = copy(dismissedTips = sanitizeDismissedTips(tips))

    /** Applies the supplied UI preferences snapshot to this profile. */
    fun withPreferences(preferences: UiPreferencesSnapshot): UserProfile {
        val normalized = preferences.normalized()
        return copy(
            themePreference = normalized.themePreference,
            visualDensity = normalized.visualDensity,
            onboardingCompleted = normalized.onboardingCompleted,
            dismissedTips = normalized.dismissedTips,
            pinnedTools = normalized.pinnedTools,
            compactMode = normalized.visualDensity == VisualDensity.COMPACT,
        )
    }

    companion object {
        const val MAX_DISPLAY_NAME = 50
        const val MAX_PINNED_TOOLS = 10
        const val MAX_SAVED_LAYOUTS = 5

        /** Builds a domain profile directly from preference state when Room has not hydrated yet. */
        fun fromPreferences(
            id: String,
            preferences: UiPreferencesSnapshot,
            lastOpenedScreen: ScreenType = ScreenType.HOME,
            savedLayouts: List<LayoutSnapshot> = emptyList(),
            displayName: String? = null,
        ): UserProfile {
            val normalized = preferences.normalized()
            return UserProfile(
                id = id,
                displayName = displayName,
                themePreference = normalized.themePreference,
                visualDensity = normalized.visualDensity,
                onboardingCompleted = normalized.onboardingCompleted,
                dismissedTips = normalized.dismissedTips,
                lastOpenedScreen = lastOpenedScreen,
                compactMode = normalized.visualDensity == VisualDensity.COMPACT,
                pinnedTools = normalized.pinnedTools,
                savedLayouts = sanitizeSavedLayouts(savedLayouts),
            )
        }
    }
}

/** Lightweight record mirroring persisted profile state prior to domain mapping. */
data class UserProfileRecord(
    val id: String,
    val displayName: String?,
    val themePreference: ThemePreference,
    val visualDensity: VisualDensity,
    val onboardingCompleted: Boolean,
    val dismissedTips: Map<String, Boolean>,
    val lastOpenedScreen: ScreenType,
    val compactMode: Boolean,
    val pinnedTools: List<String>,
    val savedLayouts: List<LayoutSnapshot>,
)

fun UserProfileRecord.toDomain(): UserProfile =
    UserProfile(
        id = id,
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

fun UserProfile.toRecord(): UserProfileRecord =
    UserProfileRecord(
        id = id,
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

/** Snapshot of preference state emitted by DataStore before merging into the domain profile. */
data class UiPreferencesSnapshot(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val visualDensity: VisualDensity = VisualDensity.DEFAULT,
    val onboardingCompleted: Boolean = false,
    var dismissedTips: Map<String, Boolean> = emptyMap(),
    var pinnedTools: List<String> = emptyList(),
) {
    init {
        dismissedTips = sanitizeDismissedTips(dismissedTips)
        pinnedTools = sanitizePinnedTools(pinnedTools)
    }
}

fun UiPreferencesSnapshot.normalized(): UiPreferencesSnapshot = copy()

fun UserProfile.toPreferencesSnapshot(): UiPreferencesSnapshot =
    UiPreferencesSnapshot(
        themePreference = themePreference,
        visualDensity = visualDensity,
        onboardingCompleted = onboardingCompleted,
        dismissedTips = dismissedTips,
        pinnedTools = pinnedTools,
    )

fun Flow<UserProfileRecord?>.mapToUserProfile(): Flow<UserProfile?> = map { record -> record?.toDomain() }

fun Flow<UserProfileRecord?>.mergeWithPreferences(
    preferences: Flow<UiPreferencesSnapshot>,
    fallback: (UiPreferencesSnapshot) -> UserProfile? = { null },
): Flow<UserProfile?> =
    combine(this, preferences) { record, prefs ->
        val normalized = prefs.normalized()
        val base = record?.toDomain() ?: fallback(normalized)
        base?.withPreferences(normalized)
    }

fun Flow<UserProfileRecord?>.requireUserProfile(
    preferences: Flow<UiPreferencesSnapshot>,
    fallback: (UiPreferencesSnapshot) -> UserProfile,
): Flow<UserProfile> = mergeWithPreferences(preferences, fallback).mapNotNull { it }

private fun sanitizeDismissedTips(tips: Map<String, Boolean>): Map<String, Boolean> {
    val sanitized = tips.filterKeys { it.isNotBlank() }
    require(sanitized.size == tips.size) { "Dismissed tip identifiers must be non-blank." }
    return sanitized
}

private fun sanitizePinnedTools(tools: List<String>): List<String> {
    val sanitized = tools.filter { it.isNotBlank() }
    require(sanitized.size == tools.size) { "Pinned tool identifiers must be non-blank." }
    val unique = sanitized.distinct()
    require(unique.size == sanitized.size) { "Pinned tool identifiers must be unique." }
    require(unique.size <= UserProfile.MAX_PINNED_TOOLS) {
        "Pinned tools cannot exceed ${UserProfile.MAX_PINNED_TOOLS}."
    }
    return unique
}

private fun sanitizeSavedLayouts(layouts: List<LayoutSnapshot>): List<LayoutSnapshot> =
    try {
        require(layouts.size <= UserProfile.MAX_SAVED_LAYOUTS) {
            "Saved layouts cannot exceed ${UserProfile.MAX_SAVED_LAYOUTS}."
        }
        val distinct = layouts.distinctBy(LayoutSnapshot::id)
        require(distinct.size == layouts.size) { "Saved layouts must have unique identifiers." }
        distinct
    } catch (error: ClassCastException) {
        throw IllegalArgumentException("Saved layouts must contain LayoutSnapshot entries.", error)
    }
