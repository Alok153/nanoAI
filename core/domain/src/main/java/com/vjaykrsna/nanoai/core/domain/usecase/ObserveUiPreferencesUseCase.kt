package com.vjaykrsna.nanoai.core.domain.usecase

import com.vjaykrsna.nanoai.core.domain.model.uiux.DataStoreUiPreferences
import com.vjaykrsna.nanoai.core.domain.repository.UiPreferencesRepository
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
constructor(private val repository: UiPreferencesRepository) {
  /**
   * Observes UI preferences with reactive updates.
   *
   * @return Flow of UI preferences that emits whenever preferences change
   */
  operator fun invoke(): Flow<DataStoreUiPreferences> = repository.preferences
}
