# Contract Tests: Fixes and Inconsistencies Stabilization Pass

These contract tests must be implemented to enforce the operational contracts defined in `openapi.yaml`. Add them under `app/src/test/contract/` and ensure they fail until the corresponding feature work is complete.

## Required Test Skeletons

1. `ModelManifestContractTest`
   - Verifies `GET /catalog/models/{modelId}/manifest` provides signed manifest with SHA-256 checksum, size, and HTTPS URL.
   - Assert failure on missing signature or invalid checksum length.

2. `ModelVerificationContractTest`
   - Exercises `POST /catalog/models/{modelId}/verify` success and integrity failure responses.
   - Ensures `status=RETRY` includes `nextRetryAfterSeconds` and `ErrorEnvelope` enumerations map to sealed error codes.

3. `CredentialRotationContractTest`
   - Validates `POST /credentials/providers/{providerId}` happy path and rejects unsupported provider/environment combinations.
   - Confirms response contains `keyAlias` for local encrypted storage and `migrationRequired` when payload changes schema.

## Implementation Notes
- Use `MockWebServer` to stub responses and validate request payloads.
- Assert JSON schemas with `networknt` JSON schema validator (already in dependencies).
- Mark tests with `@Ignore("Pending implementation")` initially so they fail when the ignore is removed during task execution.
- Include TODO references to the corresponding RepoMaintenanceTask IDs for traceability.
