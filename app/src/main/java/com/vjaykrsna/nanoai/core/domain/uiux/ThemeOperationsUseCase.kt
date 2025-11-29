package com.vjaykrsna.nanoai.core.domain.uiux

import android.database.sqlite.SQLiteException
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.annotations.OneShot
import com.vjaykrsna.nanoai.core.common.annotations.ReactiveStream
import com.vjaykrsna.nanoai.core.domain.model.uiux.ShellUiPreferences
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.core.domain.repository.ThemeRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

/**
 * Use case for theme and UI preference operations.
 *
 * Provides clean domain interface for theme-related operations, wrapping ThemeRepository.
 */
class ThemeOperationsUseCase @Inject constructor(private val themeRepository: ThemeRepository) {

  /** Observes UI preference snapshot with reactive updates. */
  @ReactiveStream("Observe UI preference snapshot changes")
  fun observeUiPreferences(): Flow<ShellUiPreferences> = themeRepository.shellUiPreferences

  /** Updates theme preference. */
  @OneShot("Update theme preference")
  suspend fun updateTheme(theme: ThemePreference): NanoAIResult<Unit> =
    guardThemeOperation(
      message = "Failed to update theme",
      context = mapOf("theme" to theme.name),
    ) {
      themeRepository.updateThemePreference(theme)
      NanoAIResult.success(Unit)
    }

  /** Updates visual density. */
  @OneShot("Update visual density preference")
  suspend fun updateVisualDensity(density: VisualDensity): NanoAIResult<Unit> =
    guardThemeOperation(
      message = "Failed to update visual density",
      context = mapOf("density" to density.name),
    ) {
      themeRepository.updateVisualDensity(density)
      NanoAIResult.success(Unit)
    }

  /** Updates high contrast enabled. */
  @OneShot("Toggle high contrast accessibility")
  suspend fun updateHighContrastEnabled(enabled: Boolean): NanoAIResult<Unit> =
    guardThemeOperation(
      message = "Failed to update high contrast",
      context = mapOf("enabled" to enabled.toString()),
    ) {
      themeRepository.updateHighContrastEnabled(enabled)
      NanoAIResult.success(Unit)
    }

  private inline fun <T> guardThemeOperation(
    message: String,
    context: Map<String, String>,
    block: () -> NanoAIResult<T>,
  ): NanoAIResult<T> {
    return try {
      block()
    } catch (cancellation: CancellationException) {
      throw cancellation
    } catch (sqliteException: SQLiteException) {
      NanoAIResult.recoverable(message = message, cause = sqliteException, context = context)
    } catch (ioException: IOException) {
      NanoAIResult.recoverable(message = message, cause = ioException, context = context)
    } catch (illegalStateException: IllegalStateException) {
      NanoAIResult.recoverable(message = message, cause = illegalStateException, context = context)
    } catch (illegalArgumentException: IllegalArgumentException) {
      NanoAIResult.recoverable(
        message = message,
        cause = illegalArgumentException,
        context = context,
      )
    }
  }
}
