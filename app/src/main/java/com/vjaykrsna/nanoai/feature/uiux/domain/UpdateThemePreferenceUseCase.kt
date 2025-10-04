package com.vjaykrsna.nanoai.feature.uiux.domain

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.data.repository.UserProfileRepository
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Persists theme preference updates while notifying observers for immediate UI feedback. */
class UpdateThemePreferenceUseCase
@Inject
constructor(
  private val repository: UserProfileRepository,
  @IoDispatcher private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  private val scope = CoroutineScope(SupervisorJob() + dispatcher)

  fun updateTheme(themePreference: ThemePreference) {
    updateThemeForUser(UIUX_DEFAULT_USER_ID, themePreference)
  }

  fun updateThemeForUser(
    userId: String,
    themePreference: ThemePreference,
    forceRefresh: Boolean = true
  ) {
    scope.launch {
      repository.updateThemePreference(userId, themePreference.name)
      repository.refreshUserProfile(userId, force = forceRefresh)
    }
  }
}
