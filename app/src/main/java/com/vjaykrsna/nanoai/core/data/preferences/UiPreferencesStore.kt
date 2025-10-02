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
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-based storage for UI/UX preferences.
 *
 * Provides reactive Flow-based access to theme, density, onboarding, and other UI settings.
 * Uses Preferences DataStore for simple key-value storage with JSON serialization for complex types.
 */
@Singleton
class UiPreferencesStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
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

            private val json =
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                    explicitNulls = false
                }

            private val stringListSerializer = ListSerializer(String.serializer())
            private val stringBooleanMapSerializer = MapSerializer(String.serializer(), Boolean.serializer())
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
                        preferences[KEY_DISMISSED_TIPS]
                            ?.takeIf { it.isNotBlank() }
                            ?.let { payload ->
                                json.decodeFromString(stringBooleanMapSerializer, payload)
                            } ?: emptyMap(),
                    pinnedToolIds =
                        preferences[KEY_PINNED_TOOL_IDS]
                            ?.takeIf { it.isNotBlank() }
                            ?.let { payload ->
                                json.decodeFromString(stringListSerializer, payload)
                            } ?: emptyList(),
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
                preferences[KEY_DISMISSED_TIPS] = json.encodeToString(stringBooleanMapSerializer, dismissedTips)
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
                    preferences[KEY_DISMISSED_TIPS]
                        ?.takeIf { it.isNotBlank() }
                        ?.let { payload ->
                            json.decodeFromString(stringBooleanMapSerializer, payload)
                        } ?: emptyMap()

                val updated = current.toMutableMap()
                updated[tipId] = true

                preferences[KEY_DISMISSED_TIPS] = json.encodeToString(stringBooleanMapSerializer, updated)
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
                preferences[KEY_PINNED_TOOL_IDS] = json.encodeToString(stringListSerializer, trimmed)
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
                    preferences[KEY_PINNED_TOOL_IDS]
                        ?.takeIf { it.isNotBlank() }
                        ?.let { payload ->
                            json.decodeFromString(stringListSerializer, payload)
                        } ?: emptyList()

                val updated = current.toMutableList()
                if (!updated.contains(toolId) && updated.size < 10) {
                    updated.add(toolId)
                    preferences[KEY_PINNED_TOOL_IDS] = json.encodeToString(stringListSerializer, updated)
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
                    preferences[KEY_PINNED_TOOL_IDS]
                        ?.takeIf { it.isNotBlank() }
                        ?.let { payload ->
                            json.decodeFromString(stringListSerializer, payload)
                        } ?: emptyList()

                val updated = current.toMutableList()
                if (updated.remove(toolId)) {
                    preferences[KEY_PINNED_TOOL_IDS] = json.encodeToString(stringListSerializer, updated)
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
