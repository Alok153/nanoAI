package com.vjaykrsna.nanoai.core.domain.usecase

import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreference
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreferenceStore
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Use case for observing privacy preferences.
 *
 * Provides a clean domain interface for reactive privacy preference observation, abstracting the
 * store implementation details.
 */
class ObservePrivacyPreferencesUseCase
@Inject
constructor(private val privacyPreferenceStore: PrivacyPreferenceStore) {
  /**
   * Observes privacy preferences with reactive updates.
   *
   * @return Flow of privacy preferences that emits whenever preferences change
   */
  operator fun invoke(): Flow<PrivacyPreference> = privacyPreferenceStore.privacyPreference
}
