package com.vjaykrsna.nanoai.core.data.uiux

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.data.preferences.UiPreferencesStore
import com.vjaykrsna.nanoai.core.domain.model.uiux.DataStoreUiPreferences
import com.vjaykrsna.nanoai.core.domain.model.uiux.ShellUiPreferences
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.core.domain.repository.ThemeRepository
import com.vjaykrsna.nanoai.core.domain.repository.UserProfileRepository
import com.vjaykrsna.nanoai.core.domain.uiux.UIUX_DEFAULT_USER_ID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

@Singleton
class ThemeRepositoryImpl
@Inject
constructor(
  private val userProfileRepository: UserProfileRepository,
  private val uiPreferencesStore: UiPreferencesStore,
  @IoDispatcher override val ioDispatcher: CoroutineDispatcher,
) : ThemeRepository {

  private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
  private val userId = UIUX_DEFAULT_USER_ID

  private val preferences =
    userProfileRepository
      .observePreferences()
      .stateIn(scope, SharingStarted.Eagerly, DataStoreUiPreferences())

  override val shellUiPreferences: Flow<ShellUiPreferences> =
    preferences
      .map { snapshot -> snapshot.toShellUiPreferences() }
      .stateIn(scope, SharingStarted.Eagerly, ShellUiPreferences())

  override suspend fun updateThemePreference(theme: ThemePreference) {
    withContext(ioDispatcher) { userProfileRepository.updateThemePreference(userId, theme.name) }
  }

  override suspend fun updateVisualDensity(density: VisualDensity) {
    withContext(ioDispatcher) { userProfileRepository.updateVisualDensity(userId, density.name) }
  }

  override suspend fun updateHighContrastEnabled(enabled: Boolean) {
    withContext(ioDispatcher) { uiPreferencesStore.setHighContrastEnabled(enabled) }
  }

  private fun DataStoreUiPreferences.toShellUiPreferences(): ShellUiPreferences =
    ShellUiPreferences(
      theme = themePreference,
      density = visualDensity,
      fontScale = 1f,
      dismissedTooltips = emptySet(),
      highContrastEnabled = highContrastEnabled,
    )
}
