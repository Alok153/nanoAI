package com.vjaykrsna.nanoai.core.domain.uiux

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.domain.model.uiux.DataStoreUiPreferences
import com.vjaykrsna.nanoai.core.domain.model.uiux.LayoutSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.UIStateSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.UserProfile
import com.vjaykrsna.nanoai.core.domain.repository.UserProfileRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/** Observes the composed user interface profile merging Room, DataStore, and UI state flows. */
class ObserveUserProfileUseCase
@Inject
constructor(
  private val repository: UserProfileRepository,
  @IoDispatcher private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  data class Result(
    val userProfile: UserProfile?,
    val layoutSnapshots: List<LayoutSnapshot>,
    val uiState: UIStateSnapshot?,
    val offline: Boolean,
    val hydratedFromCache: Boolean,
  )

  /** Zero-argument Flow accessor used by contract tests. Defaults to the primary user profile. */
  val flow: Flow<Result> = invoke()

  operator fun invoke(userId: String = UIUX_DEFAULT_USER_ID): Flow<Result> =
    callbackFlow {
        var firstEmission = true
        val job =
          combine(
              repository.observeUserProfile(userId),
              repository.observePreferences(),
              repository.observeUIStateSnapshot(userId),
              repository.observeOfflineStatus(),
            ) { profile, preferences, uiState, offline ->
              val preferencesSnapshot = preferences.toSnapshot()
              val mergedProfile = profile?.let { it.withPreferences(preferencesSnapshot) }
              Result(
                userProfile = mergedProfile,
                layoutSnapshots =
                  mergedProfile?.savedLayouts ?: profile?.savedLayouts ?: emptyList(),
                uiState = uiState,
                offline = offline,
                hydratedFromCache = firstEmission,
              )
            }
            .onEach { result ->
              trySend(result.copy(hydratedFromCache = firstEmission))
              if (firstEmission) {
                firstEmission = false
              }
            }
            .launchIn(this)

        awaitClose { job.cancel() }
      }
      .flowOn(dispatcher)

  private fun Any?.toSnapshot(): DataStoreUiPreferences =
    when (this) {
      null -> DataStoreUiPreferences()
      is DataStoreUiPreferences -> this
      else -> DataStoreUiPreferences()
    }
}
