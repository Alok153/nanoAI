package com.vjaykrsna.nanoai.feature.uiux.domain

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.data.repository.UserProfileRepository
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Consolidated settings operations for theme and UI preferences management. */
class SettingsOperationsUseCase
@Inject
constructor(
  private val repository: UserProfileRepository,
  @IoDispatcher private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  private val scope = CoroutineScope(SupervisorJob() + dispatcher)

  /** Updates theme preference for the active user. */
  fun updateTheme(themePreference: ThemePreference, userId: String = UIUX_DEFAULT_USER_ID) {
    scope.launch { repository.updateThemePreference(userId, themePreference.name) }
  }

  /** Updates visual density preference for the active user. */
  fun updateVisualDensity(density: VisualDensity, userId: String = UIUX_DEFAULT_USER_ID) {
    scope.launch { repository.updateVisualDensity(userId, density.name) }
  }
}
