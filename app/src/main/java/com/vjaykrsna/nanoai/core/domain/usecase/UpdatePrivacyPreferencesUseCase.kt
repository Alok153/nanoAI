package com.vjaykrsna.nanoai.core.domain.usecase

import com.vjaykrsna.nanoai.core.domain.repository.PrivacyPreferenceRepository
import com.vjaykrsna.nanoai.core.domain.settings.model.RetentionPolicy
import javax.inject.Inject
import kotlinx.datetime.Instant

/**
 * Use case for updating privacy preferences.
 *
 * Provides a clean domain interface for privacy preference updates, abstracting the store
 * implementation details.
 */
class UpdatePrivacyPreferencesUseCase
@Inject
constructor(private val repository: PrivacyPreferenceRepository) {
  /** Updates telemetry opt-in preference. */
  suspend fun setTelemetryOptIn(optIn: Boolean) = repository.setTelemetryOptIn(optIn)

  /** Acknowledges consent with timestamp. */
  suspend fun acknowledgeConsent(timestamp: Instant) = repository.acknowledgeConsent(timestamp)

  /** Updates retention policy. */
  suspend fun setRetentionPolicy(policy: RetentionPolicy) = repository.setRetentionPolicy(policy)

  /** Dismisses export warnings. */
  suspend fun setExportWarningsDismissed(dismissed: Boolean) =
    repository.setExportWarningsDismissed(dismissed)

  /** Records that the disclaimer dialog was shown to the user. */
  suspend fun incrementDisclaimerShown() = repository.incrementDisclaimerShown()
}
