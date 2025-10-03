# Data Model: Fixes and Inconsistencies Stabilization Pass

**Feature**: 004-fixes-and-inconsistencies  
**Date**: 2025-10-03  

## Entities

### RepoMaintenanceTask
Tracks stabilization work items sourced from `docs/inconsistencies.md`, Detekt reports, and TODO audits.

**Fields**:
- `id`: String (UUID or deterministic slug like `detekt-navigation-scaffold`)
- `title`: String (short description of fix)
- `description`: String (detailed remediation notes + acceptance criteria)
- `category`: MaintenanceCategory (enum: STATIC_ANALYSIS, SECURITY, TESTING, RUNTIME, DOCS)
- `priority`: PriorityLevel (enum: CRITICAL, HIGH, MEDIUM, LOW)
- `status`: MaintenanceStatus (enum: IDENTIFIED, IN_PROGRESS, IN_REVIEW, VERIFIED, BLOCKED)
- `owner`: String? (GitHub username or team alias)
- `blockingRules`: List<String> (Detekt rule IDs, test IDs, or spec references)
- `linkedArtifacts`: List<URI> (links to PRs, tasks, or reports)
- `createdAt`: Instant
- `updatedAt`: Instant

**Validation Rules**:
- `title` max 120 chars; must start with actionable verb ("Refactor", "Secure", etc.).
- `priority = CRITICAL` requires non-empty `blockingRules`.
- `status = VERIFIED` requires `linkedArtifacts` to include at least one merged PR or test report.

**State Transitions**:
- IDENTIFIED → IN_PROGRESS (when work starts)
- IN_PROGRESS → IN_REVIEW (PR opened/tests written)
- IN_REVIEW → VERIFIED (PR merged/tests green)
- IN_REVIEW → BLOCKED (if new blocker found)
- BLOCKED → IN_PROGRESS (once unblocker defined)

### CodeQualityMetric
Represents tracked Detekt/ktlint results for blocking categories.

**Fields**:
- `id`: String (ruleId + file path hash)
- `ruleId`: String (Detekt rule name)
- `filePath`: String (relative path in repo)
- `severity`: SeverityLevel (enum: WARNING, ERROR)
- `occurrences`: Int (count of hits)
- `threshold`: Int (allowed count before failure)
- `firstDetectedAt`: Instant
- `resolvedAt`: Instant?
- `notes`: String? (remediation summary)

**Validation Rules**:
- `occurrences` ≥ 1 when record exists.
- `resolvedAt` set only when `occurrences` == 0.

**Relationships**:
- Many-to-one with RepoMaintenanceTask (multiple metrics may map to same task).

### ModelPackage
Describes downloadable AI model bundles and integrity metadata.

**Fields**:
- `id`: String (model slug)
- `version`: String (semantic version)
- `manifestUrl`: URI
- `checksumSha256`: String (64 hex chars)
- `sizeBytes`: Long (positive)
- `signature`: String? (Base64 signature if manifest signed)
- `deliveryType`: DeliveryType (enum: LOCAL_ARCHIVE, PLAY_ASSET, CLOUD_FALLBACK)
- `minAppVersion`: Int (versionCode required)
- `createdAt`: Instant
- `updatedAt`: Instant

**Validation Rules**:
- `checksumSha256` must be 64 hex characters.
- `sizeBytes` > 0.
- `signature` required when `deliveryType = LOCAL_ARCHIVE`.

**State Transitions**:
- Registered → Verified → Available → Deprecated.
- Deprecated models remain downloadable until replacement flagged.

### DownloadManifest
Represents the manifest fetched by `ModelDownloadWorker` per model version.

**Fields**:
- `modelId`: String
- `version`: String
- `checksumSha256`: String
- `sizeBytes`: Long
- `expiry`: Instant? (optional TTL)
- `downloadUrl`: URI
- `releaseNotes`: String?

**Validation Rules**:
- `downloadUrl` must be HTTPS.
- `expiry` must be in the future at fetch time if provided.

**Relationships**:
- One-to-one with ModelPackage (same `modelId` + `version`).
- Persisted in Room for auditing and reuse between retries.

### SecretCredential
Secure storage abstraction for provider keys and tokens.

**Fields**:
- `providerId`: String (e.g., "openai", "google")
- `keyAlias`: String (alias in EncryptedSharedPreferences)
- `storedAt`: Instant
- `rotatesAfter`: Instant?
- `scope`: CredentialScope (enum: TEXT_INFERENCE, VISION, AUDIO, EXPORT)
- `metadata`: Map<String, String> (e.g., environment, contact email)

**Validation Rules**:
- `providerId` must map to known provider registry entry.
- `rotatesAfter` > `storedAt` when present.

**State Transitions**:
- Stored → Validated → Rotating → Archived.
- Archived credentials removed from encrypted store and redacted from logs.

### ErrorEnvelope
Standardized error payload surfaced to UI and logging.

**Fields**:
- `code`: ErrorCode (enum: NETWORK_UNAVAILABLE, INTEGRITY_FAILURE, AUTH_REQUIRED, OUT_OF_MEMORY, UNKNOWN)
- `message`: String (user-friendly, localized key preferred)
- `cause`: Throwable? (internal debug only)
- `retryPolicy`: RetryPolicy (enum: RETRYABLE, MANUAL_RETRY, DO_NOT_RETRY)
- `telemetryId`: String? (UUID for analytics)
- `timestamp`: Instant
- `context`: Map<String, String> (e.g., modelId, workerId, personaId)

**Validation Rules**:
- `message` must be translatable key, not hardcoded string.
- `cause` never serialized in release builds.
- When `retryPolicy = RETRYABLE`, include `context["retryAfterSeconds"]`.

**Relationships**:
- Emitted by domain use cases returning `NanoAIResult`.
- Logged and bubbled to UI via state reducers.

## Storage Strategy
- `RepoMaintenanceTask` & `CodeQualityMetric`: stored in Room tables to track remediation progress; also exported to docs for transparency.
- `ModelPackage` & `DownloadManifest`: Room entities with DAO for WorkManager access; manifests cached to avoid redundant downloads.
- `SecretCredential`: persisted in `EncryptedSharedPreferences` with migration helpers and backup strategy respecting constitution.
- `ErrorEnvelope`: transient; serialized for logs/analytics; optionally persisted for crash repro.

## Notes
- Data model supports automated dashboards (e.g., Compose screen summarizing maintenance progress).
- Ensure migrations update schemas and provide fallback for existing Room databases.
