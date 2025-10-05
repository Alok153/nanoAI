package com.vjaykrsna.nanoai.feature.uiux.state

import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity

/** Lightweight snapshot of persisted UI preferences relevant to the shell. */
data class UiPreferenceSnapshot(
  val theme: ThemePreference = ThemePreference.SYSTEM,
  val density: VisualDensity = VisualDensity.DEFAULT,
  val fontScale: Float = 1f,
  val onboardingCompleted: Boolean = false,
  val dismissedTooltips: Set<String> = emptySet(),
)
