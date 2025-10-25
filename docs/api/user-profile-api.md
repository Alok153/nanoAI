# User Profile Metadata API

## GET /user/profile

Returns the UI personalization payload consumed by the nanoAI UI/UX feature set. The endpoint is idempotent and safe to cache; responses are merged with local Room/DataStore snapshots for offline continuity.

```http
GET /user/profile HTTP/1.1
Accept: application/json
Authorization: Bearer <token>
```

```json
Response 200
{
  "id": "user-123",
  "displayName": "Vijay",
  "themePreference": "SYSTEM",
  "visualDensity": "DEFAULT",
  "pinnedTools": ["summarize", "translator"],
  "dismissedTips": {"home_tools_tip": true},
  "savedLayouts": [
    {
      "id": "layout-01",
      "name": "Desk Setup",
      "lastOpenedScreen": "HOME",
      "pinnedTools": ["summarize"],
      "isCompact": false
    }
  ],
  "onboardingCompleted": true,
  "lastOpenedScreen": "HOME"
}
```

## Caching & Privacy

- Clients persist the response in Room (`UserProfileEntity`, `UIStateSnapshotEntity`) and DataStore (`UiPreferencesStore`) to serve offline sessions.
- Sensitive fields (display name, pinned tool IDs) remain on-device; telemetry egress is conditional on explicit consent captured in `privacy.telemetryOptIn`.
- Error responses should include 401 (revoked session), 403 (consent revoked), and 503 (maintenance). The repository wraps these into domain-level failure states surfaced to the Offline banner and Settings screen.
