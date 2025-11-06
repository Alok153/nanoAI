package com.vjaykrsna.nanoai.core.data.uiux

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandAction
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandCategory
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandDestination
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityBannerState
import com.vjaykrsna.nanoai.core.domain.model.uiux.ConnectivityStatus
import com.vjaykrsna.nanoai.core.domain.model.uiux.ModeId
import com.vjaykrsna.nanoai.core.domain.model.uiux.UiPreferencesSnapshot as DomainUiPreferencesSnapshot
import com.vjaykrsna.nanoai.core.domain.repository.ConnectivityRepository
import com.vjaykrsna.nanoai.core.domain.repository.UserProfileRepository
import com.vjaykrsna.nanoai.core.domain.uiux.navigation.Screen
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.toJavaInstant

@Singleton
class ConnectivityRepositoryImpl
@Inject
constructor(
  private val userProfileRepository: UserProfileRepository,
  @IoDispatcher override val ioDispatcher: CoroutineDispatcher,
) : ConnectivityRepository {

  private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

  private val connectivity = MutableStateFlow(ConnectivityStatus.ONLINE)

  private val preferences: StateFlow<DomainUiPreferencesSnapshot> =
    userProfileRepository
      .observePreferences()
      .stateIn(scope, SharingStarted.Eagerly, DomainUiPreferencesSnapshot())

  override val connectivityBannerState: Flow<ConnectivityBannerState> =
    combine(connectivity, preferences) {
        status: ConnectivityStatus,
        prefs: DomainUiPreferencesSnapshot ->
        ConnectivityBannerState(
          status = status,
          lastDismissedAt = prefs.connectivityBannerLastDismissed?.toJavaInstant(),
          queuedActionCount = 0, // TODO: get from ProgressRepository
          cta = modelLibraryCta(status),
        )
      }
      .stateIn(scope, SharingStarted.Eagerly, ConnectivityBannerState(status = connectivity.value))

  override suspend fun updateConnectivity(status: ConnectivityStatus) {
    connectivity.value = status
    userProfileRepository.setOfflineOverride(status != ConnectivityStatus.ONLINE)
  }

  private fun modelLibraryCta(status: ConnectivityStatus): CommandAction? =
    when (status) {
      ConnectivityStatus.OFFLINE,
      ConnectivityStatus.LIMITED -> MODEL_LIBRARY_CTA
      ConnectivityStatus.ONLINE -> null
    }

  private companion object {
    private val MODEL_LIBRARY_CTA =
      CommandAction(
        id = "open-model-library",
        title = "Manage downloads",
        category = CommandCategory.JOBS,
        destination = CommandDestination.Navigate(Screen.fromModeId(ModeId.LIBRARY).route),
      )
  }
}
