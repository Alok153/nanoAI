package com.vjaykrsna.nanoai.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Singleton

/**
 * DataStore-based storage for UI/UX preferences.
 *
 * Provides reactive Flow-based access to theme, density, onboarding, and other UI settings.
 * Uses Preferences DataStore for simple key-value storage with JSON serialization for complex types.
 */
@Singleton
class UiPreferencesStore(
    @ApplicationContext private val context: Context,
    private val converters: UiPreferencesConverters,
) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = "ui_preferences",
        )

        private val KEY_THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        private val KEY_VISUAL_DENSITY = stringPreferencesKey("visual_density")
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_DISMISSED_TIPS = stringPreferencesKey("dismissed_tips")
        private val KEY_PINNED_TOOL_IDS = stringPreferencesKey("pinned_tool_ids")
    }

    /**
     * Flow of current UI preferences.
     * Emits whenever preferences change.
     */
    val uiPreferences: Flow<UiPreferences> =
        context.dataStore.data.map { preferences ->
            UiPreferences(
                themePreference =
                    preferences[KEY_THEME_PREFERENCE]?.let { name ->
                        ThemePreference.fromName(name)
                    } ?: ThemePreference.SYSTEM,
                visualDensity =
                    preferences[KEY_VISUAL_DENSITY]?.let { name ->
                        VisualDensity
                            .values()
                            .firstOrNull { it.name.equals(name, ignoreCase = true) }
                    } ?: VisualDensity.DEFAULT,
                onboardingCompleted = preferences[KEY_ONBOARDING_COMPLETED] ?: false,
                dismissedTips =
                    converters.decodeBooleanMap(
                        preferences[KEY_DISMISSED_TIPS],
                    ),
                pinnedToolIds =
                    converters.decodeStringList(
                        preferences[KEY_PINNED_TOOL_IDS],
                    ),
            )
        }

    /**
     * Update theme preference.
     *
     * @param themePreference The new theme preference (LIGHT, DARK, SYSTEM)
     */
    suspend fun setThemePreference(themePreference: ThemePreference) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_PREFERENCE] = themePreference.name
        }
    }

    /**
     * Update visual density preference.
     *
     * @param visualDensity The new visual density (DEFAULT, COMPACT, EXPANDED)
     */
    suspend fun setVisualDensity(visualDensity: VisualDensity) {
        context.dataStore.edit { preferences ->
            preferences[KEY_VISUAL_DENSITY] = visualDensity.name
        }
    }

    /**
     * Update onboarding completed flag.
     *
     * @param completed True if onboarding is completed
     */
    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    /**
     * Update dismissed tips map.
     *
     * @param dismissedTips Map of tip IDs to dismissed status
     */
    suspend fun setDismissedTips(dismissedTips: Map<String, Boolean>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DISMISSED_TIPS] = converters.encodeBooleanMap(dismissedTips)
        }
    }

    /**
     * Dismiss a specific tip by ID.
     *
     * @param tipId The tip ID to dismiss
     */
    suspend fun dismissTip(tipId: String) {
        context.dataStore.edit { preferences ->
            val current =
                converters.decodeBooleanMap(
                    preferences[KEY_DISMISSED_TIPS],
                )

            val updated = current.toMutableMap()
            updated[tipId] = true

            preferences[KEY_DISMISSED_TIPS] = converters.encodeBooleanMap(updated)
        }
    }

    /**
     * Update pinned tool IDs list.
     *
     * @param pinnedToolIds List of tool IDs (max 10 items)
     */
    suspend fun setPinnedToolIds(pinnedToolIds: List<String>) {
        context.dataStore.edit { preferences ->
            // Enforce max 10 items
            val trimmed = pinnedToolIds.take(10)
            preferences[KEY_PINNED_TOOL_IDS] = converters.encodeStringList(trimmed)
        }
    }

    /**
     * Add a tool to the pinned tools list.
     *
     * @param toolId The tool ID to add
     */
    suspend fun addPinnedTool(toolId: String) {
        context.dataStore.edit { preferences ->
            val current =
                converters.decodeStringList(
                    preferences[KEY_PINNED_TOOL_IDS],
                )

            val updated = current.toMutableList()
            if (!updated.contains(toolId) && updated.size < 10) {
                updated.add(toolId)
                preferences[KEY_PINNED_TOOL_IDS] = converters.encodeStringList(updated)
            }
        }
    }

    /**
     * Remove a tool from the pinned tools list.
     *
     * @param toolId The tool ID to remove
     */
    suspend fun removePinnedTool(toolId: String) {
        context.dataStore.edit { preferences ->
            val current =
                converters.decodeStringList(
                    preferences[KEY_PINNED_TOOL_IDS],
                )

            val updated = current.toMutableList()
            if (updated.remove(toolId)) {
                preferences[KEY_PINNED_TOOL_IDS] = converters.encodeStringList(updated)
            }
        }
    }

    /**
     * Reorder pinned tools.
     *
     * @param orderedToolIds List of tool IDs in desired order (max 10)
     */
    suspend fun reorderPinnedTools(orderedToolIds: List<String>) {
        setPinnedToolIds(orderedToolIds)
    }

    /**
     * Reset all UI preferences to defaults (for testing).
     */
    suspend fun reset() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
