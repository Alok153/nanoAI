package com.vjaykrsna.nanoai.core.domain.usecase

import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreferenceStore
import com.vjaykrsna.nanoai.core.data.preferences.RetentionPolicy
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
constructor(private val privacyPreferenceStore: PrivacyPreferenceStore) {
  /** Updates telemetry opt-in preference. */
  suspend fun setTelemetryOptIn(optIn: Boolean) = privacyPreferenceStore.setTelemetryOptIn(optIn)

  /** Acknowledges consent with timestamp. */
  suspend fun acknowledgeConsent(timestamp: Instant) =
    privacyPreferenceStore.acknowledgeConsent(timestamp)

  /** Updates retention policy. */
  suspend fun setRetentionPolicy(policy: RetentionPolicy) =
    privacyPreferenceStore.setRetentionPolicy(policy)

  /** Dismisses export warnings. */
  suspend fun setExportWarningsDismissed(dismissed: Boolean) =
    privacyPreferenceStore.setExportWarningsDismissed(dismissed)
}
