package com.vjaykrsna.nanoai.core.domain.usecase

import com.vjaykrsna.nanoai.core.data.preferences.UiPreferences
import com.vjaykrsna.nanoai.core.data.preferences.UiPreferencesStore
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Use case for observing UI preferences.
 *
 * Provides a clean domain interface for reactive UI preference observation, abstracting the store
 * implementation details.
 */
class ObserveUiPreferencesUseCase
@Inject
constructor(private val uiPreferencesStore: UiPreferencesStore) {
  /**
   * Observes UI preferences with reactive updates.
   *
   * @return Flow of UI preferences that emits whenever preferences change
   */
  operator fun invoke(): Flow<UiPreferences> = uiPreferencesStore.uiPreferences
}
