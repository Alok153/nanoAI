package com.vjaykrsna.nanoai.core.domain.usecase

import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreferenceStore
import com.vjaykrsna.nanoai.core.domain.settings.model.DisclaimerExposureState
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Use case for observing privacy disclaimer exposure state.
 *
 * Provides a domain-friendly handle over the underlying preference store while preserving the
 * reactive API surface needed by UI consumers.
 */
class ObserveDisclaimerExposureUseCase
@Inject
constructor(private val privacyPreferenceStore: PrivacyPreferenceStore) {
  /**
   * Observes the privacy disclaimer exposure state.
   *
   * @return Flow emitting the latest disclaimer exposure information.
   */
  operator fun invoke(): Flow<DisclaimerExposureState> = privacyPreferenceStore.disclaimerExposure
}
