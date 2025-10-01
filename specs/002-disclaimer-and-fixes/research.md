# Research: First-launch Disclaimer and Fixes

**Feature**: First-launch Disclaimer and Fixes
**Spec**: /home/vijay/Personal/myGithub/nanoAI/specs/002-disclaimer-and-fixes/spec.md
**Date**: 2025-10-01

## Decisions

1. Export/Import format
   - Decision: Use JSON as the canonical export format; when packaging multiple files (e.g., model binaries in future) provide an optional ZIP wrapper. The spec is updated to prefer JSON and allow ZIP where necessary.
   - Rationale: JSON is human-readable, easy to validate, and portable across platforms. ZIP is optional for bundling large assets.

2. Backup encryption
   - Decision: Backups are unencrypted by default but the UI will clearly warn users; add an advanced option in future to encrypt with a user-provided passphrase.
   - Rationale: Matches spec guidance and minimizes initial complexity while keeping future upgrade path.

3. First-launch disclaimer behavior
   - Decision: Show a non-blocking disclaimer dialog at first launch with "Acknowledge" button. Acknowledge stores a timestamp in `PrivacyPreference` and suppresses future dialogs. A dismiss action without acknowledge will leave dialog to reappear on next launch.
   - Rationale: Aligns with spec acceptance scenarios and legal/UX balance: user's ability to proceed without friction while capturing explicit consent acknowledgement.

4. Sidebar toggles
   - Decision: Provide a single Local/Cloud inference mode toggle and a "Clear Conversation Context" action. Remove the ambiguous "mute future audio output" toggle from this milestone.
   - Rationale: The audio toggle was identified as a mistaken requirement in clarifications.

5. Tests & lint fixes strategy
   - Decision: Triage failing unit tests to two categories: (A) tests that depend on not-yet-implemented features (deferred), and (B) tests that fail due to import/namespace issues or missing test dependencies (fix now). Create contract tests stubs to codify expected behavior. Address lint issues (suspicious indentation) by running ktlint/Detekt fixes and small code cleanups.
   - Rationale: Avoids speculative implementation; keeps CI green by fixing test harness issues and non-functional problems first.

## Alternatives Considered
- Encrypted backups by default — rejected for MVP due to UX friction and key-management complexity.
- Make disclaimer mandatory (blocking) — rejected to preserve immediate access and follow spec guidance.

## Output / Next Steps
- Implement `research.md` decisions into `plan.md` (technical context) and `data-model.md`.
- Create contract stubs for import/export and disclaimer acknowledgement endpoints (settings API) to support tests.
