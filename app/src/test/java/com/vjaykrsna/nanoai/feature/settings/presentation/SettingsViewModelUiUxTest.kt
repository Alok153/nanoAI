package com.vjaykrsna.nanoai.feature.settings.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * UI/UX-focused contract test for SettingsViewModel additions introduced in the nanoAI polished
 * experience plan. These tests intentionally fail until the SettingsViewModel exposes:
 * - A theme toggle entry point that persists the user's preference
 * - Visual density toggles for compact/default layouts
 * - An undo affordance for reversible settings changes
 */
class SettingsViewModelUiUxTest {
  @Test
  fun settingsViewModel_providesThemeTogglePersistenceHook() {
    val themeMethod =
      runCatching {
          SettingsViewModel::class.java.getDeclaredMethod(
            "setThemePreference",
            ThemePreference::class.java,
          )
        }
        .getOrNull()

    assertNotNull(themeMethod)
    assertThat(themeMethod!!.returnType).isEqualTo(Void.TYPE)
  }

  @Test
  fun settingsViewModel_exposesDensityToggles_andUndoSupport() {
    val densityMethod =
      runCatching {
          SettingsViewModel::class.java.getDeclaredMethod(
            "applyDensityPreference",
            java.lang.Boolean.TYPE,
          )
        }
        .getOrNull()
    assertNotNull(densityMethod)

    val undoMethod =
      runCatching { SettingsViewModel::class.java.getDeclaredMethod("undoUiPreferenceChange") }
        .getOrNull()
    assertNotNull(undoMethod)

    assertThat(densityMethod!!.returnType).isEqualTo(Void.TYPE)
    assertThat(undoMethod!!.returnType).isEqualTo(Void.TYPE)
  }
}
