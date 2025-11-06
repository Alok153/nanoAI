package com.vjaykrsna.nanoai.core.domain.repository

import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.UiPreferenceSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import kotlinx.coroutines.flow.Flow

interface ThemeRepository : BaseRepository {
  val uiPreferenceSnapshot: Flow<UiPreferenceSnapshot>

  suspend fun updateThemePreference(theme: ThemePreference)

  suspend fun updateVisualDensity(density: VisualDensity)

  suspend fun updateHighContrastEnabled(enabled: Boolean)
}
