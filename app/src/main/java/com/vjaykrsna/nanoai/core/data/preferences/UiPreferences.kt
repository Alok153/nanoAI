package com.vjaykrsna.nanoai.core.data.preferences

import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.UiPreferencesSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity

/**
 * Data class representing UI/UX preferences.
 *
 * Stored using DataStore for reactive updates. These preferences overlay the database-backed
 * UserProfile for quick access to frequently-accessed UI settings.
 *
 * @property themePreference User's theme preference (light/dark/system)
 * @property visualDensity Visual density preference for UI spacing
 * @property onboardingCompleted Whether user has completed onboarding flow
 * @property dismissedTips Map of tip IDs to dismissed status
 * @property pinnedToolIds Ordered list of pinned tool IDs (max 10)
 */
data class UiPreferences(
    val themePreference: ThemePreference,
    val visualDensity: VisualDensity,
    val onboardingCompleted: Boolean,
    val dismissedTips: Map<String, Boolean>,
    val pinnedToolIds: List<String>,
) {
    constructor() : this(
        themePreference = ThemePreference.SYSTEM,
        visualDensity = VisualDensity.DEFAULT,
        onboardingCompleted = false,
        dismissedTips = emptyMap(),
        pinnedToolIds = emptyList(),
    )
}

/**
 * Convert DataStore UiPreferences to domain UiPreferencesSnapshot.
 *
 * This mapper bridges the persistence layer (DataStore) to the domain layer,
 * allowing preferences to be merged with database-backed UserProfile records.
 */
fun UiPreferences.toDomainSnapshot(): UiPreferencesSnapshot =
    UiPreferencesSnapshot(
        themePreference = themePreference,
        visualDensity = visualDensity,
        onboardingCompleted = onboardingCompleted,
        dismissedTips = dismissedTips,
        pinnedTools = pinnedToolIds,
    )

/**
 * Convert domain UiPreferencesSnapshot to DataStore UiPreferences.
 *
 * This reverse mapper allows domain preferences to be persisted to DataStore.
 */
fun UiPreferencesSnapshot.toDataStorePreferences(): UiPreferences =
    UiPreferences(
        themePreference = themePreference,
        visualDensity = visualDensity,
        onboardingCompleted = onboardingCompleted,
        dismissedTips = dismissedTips,
        pinnedToolIds = pinnedTools,
    )
