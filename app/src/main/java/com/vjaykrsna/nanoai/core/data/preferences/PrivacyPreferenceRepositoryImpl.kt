package com.vjaykrsna.nanoai.core.data.preferences

import com.vjaykrsna.nanoai.core.domain.repository.PrivacyPreferenceRepository
import com.vjaykrsna.nanoai.core.domain.settings.model.DisclaimerExposureState
import com.vjaykrsna.nanoai.core.domain.settings.model.PrivacyPreference
import com.vjaykrsna.nanoai.core.domain.settings.model.RetentionPolicy
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/** Wraps [PrivacyPreferenceStore] to enforce the domain-facing interface. */
@Singleton
class PrivacyPreferenceRepositoryImpl
@Inject
constructor(private val privacyPreferenceStore: PrivacyPreferenceStore) :
  PrivacyPreferenceRepository {
  override val privacyPreference: Flow<PrivacyPreference> = privacyPreferenceStore.privacyPreference

  override val disclaimerExposure: Flow<DisclaimerExposureState> =
    privacyPreferenceStore.disclaimerExposure

  override suspend fun setTelemetryOptIn(optIn: Boolean) {
    privacyPreferenceStore.setTelemetryOptIn(optIn)
  }

  override suspend fun acknowledgeConsent(timestamp: Instant) {
    privacyPreferenceStore.acknowledgeConsent(timestamp)
  }

  override suspend fun setRetentionPolicy(policy: RetentionPolicy) {
    privacyPreferenceStore.setRetentionPolicy(policy)
  }

  override suspend fun setExportWarningsDismissed(dismissed: Boolean) {
    privacyPreferenceStore.setExportWarningsDismissed(dismissed)
  }

  override suspend fun incrementDisclaimerShown() {
    privacyPreferenceStore.incrementDisclaimerShown()
  }
}
