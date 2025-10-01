package com.vjaykrsna.nanoai.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-based storage for privacy preferences.
 *
 * Provides reactive Flow-based access to privacy settings and consent tracking.
 * Uses Preferences DataStore for simple key-value storage.
 */
@Singleton
class PrivacyPreferenceStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        companion object {
            private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
                name = "privacy_preferences",
            )

            private val KEY_EXPORT_WARNINGS_DISMISSED = booleanPreferencesKey("export_warnings_dismissed")
            private val KEY_TELEMETRY_OPT_IN = booleanPreferencesKey("telemetry_opt_in")
            private val KEY_CONSENT_ACKNOWLEDGED_AT = longPreferencesKey("consent_acknowledged_at")
            private val KEY_RETENTION_POLICY = stringPreferencesKey("retention_policy")
        }

        /**
         * Flow of current privacy preferences.
         * Emits whenever preferences change.
         */
        val privacyPreference: Flow<PrivacyPreference> =
            context.dataStore.data.map { preferences ->
                PrivacyPreference(
                    exportWarningsDismissed = preferences[KEY_EXPORT_WARNINGS_DISMISSED] ?: false,
                    telemetryOptIn = preferences[KEY_TELEMETRY_OPT_IN] ?: false,
                    consentAcknowledgedAt =
                        preferences[KEY_CONSENT_ACKNOWLEDGED_AT]?.let {
                            Instant.fromEpochMilliseconds(it)
                        },
                    retentionPolicy =
                        preferences[KEY_RETENTION_POLICY]?.let {
                            RetentionPolicy.valueOf(it)
                        } ?: RetentionPolicy.INDEFINITE,
                )
            }

        /**
         * Update export warnings dismissed flag.
         */
        suspend fun setExportWarningsDismissed(dismissed: Boolean) {
            context.dataStore.edit { preferences ->
                preferences[KEY_EXPORT_WARNINGS_DISMISSED] = dismissed
            }
        }

        /**
         * Update telemetry opt-in preference.
         */
        suspend fun setTelemetryOptIn(optIn: Boolean) {
            context.dataStore.edit { preferences ->
                preferences[KEY_TELEMETRY_OPT_IN] = optIn
            }
        }

        /**
         * Update consent acknowledgment timestamp to current time.
         */
        suspend fun acknowledgeConsent(timestamp: Instant) {
            context.dataStore.edit { preferences ->
                preferences[KEY_CONSENT_ACKNOWLEDGED_AT] = timestamp.toEpochMilliseconds()
            }
        }

        /**
         * Update data retention policy.
         */
        suspend fun setRetentionPolicy(policy: RetentionPolicy) {
            context.dataStore.edit { preferences ->
                preferences[KEY_RETENTION_POLICY] = policy.name
            }
        }

        /**
         * Reset all privacy preferences to defaults (for testing).
         */
        suspend fun reset() {
            context.dataStore.edit { preferences ->
                preferences.clear()
            }
        }
    }
