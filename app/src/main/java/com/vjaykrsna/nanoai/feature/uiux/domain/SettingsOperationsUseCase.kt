package com.vjaykrsna.nanoai.feature.uiux.domain

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.data.repository.UserProfileRepository
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Consolidated settings operations for theme and UI preferences management. */
class SettingsOperationsUseCase
@Inject
constructor(
  private val repository: UserProfileRepository,
  @IoDispatcher private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

  /** Updates theme preference for the active user. */
  suspend fun updateTheme(
    themePreference: ThemePreference,
    userId: String = UIUX_DEFAULT_USER_ID,
  ): NanoAIResult<Unit> {
    return withContext(dispatcher) {
      runCatching { repository.updateThemePreference(userId, themePreference.name) }
        .fold(
          onSuccess = { NanoAIResult.success(Unit) },
          onFailure = {
            NanoAIResult.recoverable(
              message = "Failed to update theme preference",
              cause = it,
              context = mapOf("userId" to userId, "theme" to themePreference.name),
            )
          },
        )
    }
  }

  /** Updates visual density preference for the active user. */
  suspend fun updateVisualDensity(
    density: VisualDensity,
    userId: String = UIUX_DEFAULT_USER_ID,
  ): NanoAIResult<Unit> {
    return withContext(dispatcher) {
      runCatching { repository.updateVisualDensity(userId, density.name) }
        .fold(
          onSuccess = { NanoAIResult.success(Unit) },
          onFailure = {
            NanoAIResult.recoverable(
              message = "Failed to update visual density preference",
              cause = it,
              context = mapOf("userId" to userId, "density" to density.name),
            )
          },
        )
    }
  }
}
