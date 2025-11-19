package com.vjaykrsna.nanoai.core.domain.uiux

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.annotations.OneShot
import com.vjaykrsna.nanoai.core.domain.repository.UserProfileRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Toggles compact UI density, updating repositories and notifying observers. */
class ToggleCompactModeUseCase
@Inject
constructor(
  private val repository: UserProfileRepository,
  @IoDispatcher private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

  /** Updates the persisted compact mode preference and associated layout snapshots. */
  @OneShot("Update compact mode preference")
  suspend fun setCompactMode(
    enabled: Boolean,
    userId: String = UIUX_DEFAULT_USER_ID,
  ): NanoAIResult<Unit> {
    return withContext(dispatcher) {
      runCatching { repository.updateCompactMode(userId, enabled) }
        .fold(
          onSuccess = { NanoAIResult.success(Unit) },
          onFailure = {
            NanoAIResult.recoverable(
              message = "Failed to update compact mode preference",
              cause = it,
              context = mapOf("userId" to userId, "compactMode" to enabled.toString()),
            )
          },
        )
    }
  }
}
