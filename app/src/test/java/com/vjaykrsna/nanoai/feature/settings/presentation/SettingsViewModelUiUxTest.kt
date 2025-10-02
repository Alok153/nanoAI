package com.vjaykrsna.nanoai.feature.settings.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * UI/UX-focused contract test for SettingsViewModel additions introduced in the
 * nanoAI polished experience plan. These tests intentionally fail until the
 * SettingsViewModel exposes:
 *  - A theme toggle entry point that persists the user's preference
 *  - Visual density toggles for compact/default layouts
 *  - An undo affordance for reversible settings changes
 */
class SettingsViewModelUiUxTest {
    @Test
    fun settingsViewModel_providesThemeTogglePersistenceHook() {
        val themeMethod =
            SettingsViewModel::class.java.methods.firstOrNull { method ->
                method.name.contains("Theme", ignoreCase = true) &&
                    method.parameterTypes.firstOrNull() == ThemePreference::class.java
            }
        assertNotNull(themeMethod)
        assertThat(themeMethod!!.returnType).isEqualTo(Void.TYPE)
    }

    @Test
    fun settingsViewModel_exposesDensityToggles_andUndoSupport() {
        val densityMethod =
            SettingsViewModel::class.java.methods.firstOrNull { method ->
                method.name.contains("Density", ignoreCase = true) ||
                    method.name.contains("Compact", ignoreCase = true)
            }
        assertNotNull(densityMethod)

        val undoMethod =
            SettingsViewModel::class.java.methods.firstOrNull { method ->
                method.name.contains("Undo", ignoreCase = true)
            }
        assertNotNull(undoMethod)

        assertThat(densityMethod!!.returnType).isEqualTo(Void.TYPE)
        assertThat(undoMethod!!.returnType).isEqualTo(Void.TYPE)
    }
}
