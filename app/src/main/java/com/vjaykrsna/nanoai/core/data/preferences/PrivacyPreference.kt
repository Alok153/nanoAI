package com.vjaykrsna.nanoai.core.data.preferences

import kotlinx.datetime.Instant

/**
 * Data class representing privacy and consent preferences.
 *
 * Stored using DataStore for reactive updates. Acts as a singleton preference set.
 *
 * @property exportWarningsDismissed Whether user has dismissed export warnings
 * @property telemetryOptIn Whether user has opted into telemetry (default false)
 * @property consentAcknowledgedAt Timestamp when consent was last acknowledged
 * @property retentionPolicy Data retention policy preference
 */
data class PrivacyPreference(
    val exportWarningsDismissed: Boolean = false,
    val telemetryOptIn: Boolean = false,
    val consentAcknowledgedAt: Instant? = null,
    val retentionPolicy: RetentionPolicy = RetentionPolicy.INDEFINITE,
)

/**
 * Data retention policy options.
 */
enum class RetentionPolicy {
    /**
     * Keep data indefinitely until manually deleted.
     */
    INDEFINITE,

    /**
     * Only purge data when user explicitly requests it.
     */
    MANUAL_PURGE_ONLY,
}
