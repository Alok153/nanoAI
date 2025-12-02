package com.vjaykrsna.nanoai.core.domain.repository

import com.vjaykrsna.nanoai.core.domain.settings.model.DisclaimerExposureState
import com.vjaykrsna.nanoai.core.domain.settings.model.PrivacyPreference
import com.vjaykrsna.nanoai.core.domain.settings.model.RetentionPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/** Contract for interacting with privacy preferences and disclaimer exposure counters. */
interface PrivacyPreferenceRepository {
  /** Reactive stream of the latest privacy preference snapshot. */
  val privacyPreference: Flow<PrivacyPreference>

  /** Derived disclaimer exposure state used by AppViewModel. */
  val disclaimerExposure: Flow<DisclaimerExposureState>

  suspend fun setTelemetryOptIn(optIn: Boolean)

  suspend fun acknowledgeConsent(timestamp: Instant)

  suspend fun setRetentionPolicy(policy: RetentionPolicy)

  suspend fun setExportWarningsDismissed(dismissed: Boolean)

  suspend fun incrementDisclaimerShown()
}
