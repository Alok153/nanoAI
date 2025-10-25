# Privacy Data Store

## Schema

Privacy preferences are stored in DataStore (key-value pairs) using the `PrivacyPreference` data class.

```kotlin
data class PrivacyPreference(
  val exportWarningsDismissed: Boolean = false,    // Whether export warnings have been dismissed
  val telemetryOptIn: Boolean = false,             // Whether telemetry is opted-in (default: opt-out)
  val consentAcknowledgedAt: Instant? = null,      // When consent was last acknowledged
  val disclaimerShownCount: Int = 0,               // Number of times disclaimer dialog has been shown
  val retentionPolicy: RetentionPolicy = RetentionPolicy.INDEFINITE,  // Data retention policy
)

/** Data retention policy options. */
enum class RetentionPolicy {
  INDEFINITE,          // Keep data indefinitely until manually deleted
  MANUAL_PURGE_ONLY,   // Only purge data when user explicitly requests it
}
```

## Keys

The preference keys used in DataStore:

```kotlin
private val EXPORT_WARNINGS_DISMISSED = booleanPreferencesKey("export_warnings_dismissed")
private val TELEMETRY_OPT_IN = booleanPreferencesKey("telemetry_opt_in")
private val CONSENT_ACKNOWLEDGED_AT = longPreferencesKey("consent_acknowledged_at")
private val DISCLAIMER_SHOWN_COUNT = intPreferencesKey("disclaimer_shown_count")
private val RETENTION_POLICY = stringPreferencesKey("retention_policy")
```

## Example

```json
{
  "export_warnings_dismissed": false,
  "telemetry_opt_in": false,
  "consent_acknowledged_at": 1696156800000,
  "disclaimer_shown_count": 5,
  "retention_policy": "INDEFINITE"
}
