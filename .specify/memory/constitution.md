<!-- Sync Impact Report
Version change: 1.4.1 → 1.5.0
Modified principles: Added Test Coverage and Types, File Organization and Modularity, expanded Documentation & Knowledge Sharing, added Tooling and Automation Standards
Added sections: Test Coverage and Types, File Organization and Modularity, Tooling and Automation Standards
Removed sections: None
Templates requiring updates:
✅ .specify/templates/plan-template.md updated
✅ .specify/templates/spec-template.md updated
✅ .specify/templates/tasks-template.md updated
✅ Runtime docs (README.md, docs/) updated
Follow-up TODOs: Implement automated constitution checks in CI/hooks, add READMEs to docs/ and scripts/, re-enable lint if resolved
-->

# nanoAI Android App Constitution

## Core Principles

### Kotlin-First Clean Architecture
- All product code MUST be written in Kotlin; Java is permitted only for third-party interop with an approved exception note (e.g., legacy libraries) documented in the PR.
- UI controllers (Activities, Fragments, Compose UI) MUST remain passive; state and business logic live in ViewModels, use cases, or domain modules.
- Data access MUST be exposed through suspend functions or Kotlin Flow; direct database or network calls from UI layers are forbidden.
Rationale: Enforcing clean boundaries and idiomatic Kotlin keeps the app testable, maintainable, and ready for modular scaling.

### Polished Material UX
- Screens MUST follow Material 3 guidelines, including typography, spacing, elevation.
- Every user interaction MUST respond within 100ms for touch feedback and 500ms for content updates; longer tasks require progress indicators or loading states.
Rationale: Consistent, responsive UI earns trust and reduces rework when new surfaces are introduced.

### Resilient Performance & Offline Readiness
- Any long-running or I/O heavy work MUST execute off the main thread via coroutines or WorkManager.
- All user data, configurations, and user profiles stay offline; no online sync is needed.
- AI model loading/unloading MUST manage memory (e.g., via weak references or explicit GC triggers) to prevent OOM; downloaded models limited to device storage thresholds with user notifications.
Rationale: Users stay engaged only when the app feels fast and reliable regardless of network conditions or resource constraints.

### Automated Quality Gates
- Unit tests MUST cover every ViewModel, use case, and repository with deterministic assertions; UI and instrumentation tests cover critical flows.
- Static analysis (ktlint, Detekt, Android Lint) and dependency vulnerability scans MUST run in CI with zero ignored blockers.
- No code merges without a green CI signal and reviewer verification against constitution checklists.

#### Test Coverage and Types
- Unit tests MUST achieve ≥75% coverage for ViewModels/Domain, ≥65% for UI/Compose, and ≥70% for Data/Repositories; integration tests MUST cover critical flows (e.g., offline sync, model downloads).
- Instrumentation tests MUST verify accessibility and offline scenarios; macrobenchmarks MUST enforce performance budgets (e.g., <1.5s cold start).
Rationale: Prevents testing inconsistencies in a growing codebase and ensures privacy/offline features are robust.

Rationale: Automation prevents regressions and keeps delivery velocity sustainable.

### Privacy & Data Stewardship
- Sensitive data at rest MUST use Android keystore-backed encryption; network calls MUST enforce TLS 1.3 and certificate pinning for first-party APIs.
Rationale: Responsible stewardship protects users, satisfies compliance, and preserves the app's reputation.

### Streamlined and Clean Codebase
- Maintaining support for previous versions or legacy code is not necessary; always prefer streamlined and clean code.
- Overengineering MUST be avoided: prefer simple, well-documented solutions that meet stated requirements. Complex or highly-architected designs require explicit justification in the plan, measurable tradeoffs, and a rollback path.
Rationale: Ensures the codebase remains maintainable, efficient, and focused on current needs without unnecessary complexity for legacy support.

### File Organization and Modularity
- UI modules MUST avoid excessive fragmentation; limit composable files to one per screen/component unless justified by reusability or >500 lines. Feature-specific data MUST reside in `feature/*/data/` for consistency.
- New abstractions or modules REQUIRE PR justification with measurable tradeoffs (e.g., "reduces duplication by X% but adds Y% maintenance cost"); simpler solutions preferred.
Rationale: Prevents over-engineering and ensures scalable, maintainable structure in a growing project.

### Documentation & Knowledge Sharing
- All public APIs, complex logic, and architectural decisions MUST be documented with KDoc comments including `@param`, `@return`, `@throws`, and examples; private functions require comments only if non-obvious.
- READMEs, design docs (in `docs/`), and wikis MUST be updated on changes; use Markdown with consistent headers (e.g., # for sections, ## for subsections) and include diagrams (e.g., Mermaid for architecture).
- Knowledge sharing MUST include bi-weekly sessions for major changes; PRs MUST link to updated docs or specs in `specs/`.
- API documentation MUST be auto-generated (e.g., via Dokka) and hosted in `docs/api/`; outdated docs flagged in CI via link checks.
- Constitution compliance MUST be verified via automated scripts in pre-commit hooks and CI, including checks for Kotlin purity, encryption usage, and performance regressions.
Rationale: Ensures documentation remains consistent, useful, and enforceable as the team scales; automates alignment with principles.

### Code Review & Collaboration
- All code changes MUST undergo peer review with at least one approver; reviews MUST verify constitution compliance and include constructive feedback.
- Team members MUST collaborate on design decisions; blocking issues MUST be escalated promptly with clear communication channels.
Rationale: Catches issues early, ensures quality, fosters team alignment, and builds collective ownership.

### Continuous Integration & Deployment
- CI MUST run on every PR with all quality gates passing; deployments MUST follow automated pipelines with rollback capabilities and environment parity.
- Release notes MUST document changes, breaking changes, migration guides, and performance impacts.
Rationale: Ensures reliability, quick feedback loops, and smooth, predictable releases.

## Platform Standards

- Kotlin DSL Gradle builds and the Android Gradle Plugin version MUST stay within one minor release of stable to receive security patches.
- Compile SDK MUST track the latest stable Android API within 30 days; minSdk changes require documented migration plans.
- Dependency injection MUST use a single framework (e.g., Hilt) with module scopes declared; alternative patterns require approval.
- Feature modules MUST expose contracts via interfaces; implementation details remain internal.
- Build variants MUST produce signed, reproducible artifacts with version codes incremented on every release branch.
- AI/ML Libraries: Use approved runtimes like MediaPipe for local inference; external APIs (e.g., for image gen) MUST be configurable via encrypted prefs without hardcoding keys.
- Model Management: Downloaded models MUST be stored in app-specific directories; support export/import for backups.

### Tooling and Automation Standards
- Static analysis (Detekt, Android Lint) MUST run in CI with zero ignored blockers; re-enable disabled checks when resolved.
- CI pipelines MUST enforce performance budgets (e.g., <1.5s cold start, <5% jank) and include lightweight security scans (e.g., OWASP Dependency Check).
Rationale: Prevents build/config inconsistencies and ensures automated quality in a complex app.

## Development Workflow & Quality Gates

1. Product discovery tickets MUST specify acceptance criteria mapped to the governing principles before development begins.
2. Every feature branch MUST start with tests or contract updates demonstrating the expected behavior.

**Version**: 1.5.0 | **Ratified**: 2025-10-02 | **Last Amended**: 2025-11-10
