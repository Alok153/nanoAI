<!-- Sync Impact Report
Version change: 1.4.0 → 1.4.1
Modified principles: Streamlined and Clean Codebase (added anti-overengineering guidance)
Added sections: None
Removed sections: None
Templates requiring updates:
✅ .specify/templates/plan-template.md updated
✅ .specify/templates/spec-template.md updated
✅ .specify/templates/tasks-template.md updated
✅ Runtime docs (README.md, docs/) updated
Follow-up TODOs: Consider adding lightweight placeholder checks in CI and a README badge reflecting constitution version
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
- Composable functions MUST use PascalCase naming (e.g., `ThemeToggle`), while regular functions use camelCase. Lambda parameters in composables MUST be present tense (e.g., `onClick`, not `onClicked`). Composable parameters MUST be ordered as: modifiers first, then other parameters with defaults, then optional trailing lambda.
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
Rationale: Automation prevents regressions and keeps delivery velocity sustainable.

### Privacy & Data Stewardship
- Sensitive data at rest MUST use Android keystore-backed encryption; network calls MUST enforce TLS 1.3 and certificate pinning for first-party APIs.
Rationale: Responsible stewardship protects users, satisfies compliance, and preserves the app's reputation.

### AI Inference Integrity
- Local LLM and multimodal models MUST use validated runtimes (e.g., MediaPipe, ONNX, LEAP); online API fallbacks MUST include retry logic with exponential backoff (max 3 retries).
- Non-deterministic AI outputs MUST be mocked in unit tests; integration tests MUST validate fallback to online APIs when local models fail.
- AI features MUST be implemented as optional dynamic modules (e.g., via Play Feature Delivery) to keep base APK <100MB and enable on-demand loading.
Rationale: Ensures reliable, secure AI behavior across local/online modes, preventing degraded UX from model errors or network issues.

### Streamlined and Clean Codebase
- Maintaining support for previous versions or legacy code is not necessary; always prefer streamlined and clean code.
- Overengineering MUST be avoided: prefer simple, well-documented solutions that meet stated requirements. Complex or highly-architected designs require explicit justification in the plan, measurable tradeoffs, and a rollback path.
Rationale: Ensures the codebase remains maintainable, efficient, and focused on current needs without unnecessary complexity for legacy support.

### Documentation & Knowledge Sharing
- All public APIs, complex logic, and architectural decisions MUST be documented with KDoc comments; READMEs, design docs, and wikis MUST be updated on changes.
- Knowledge sharing sessions MUST be held for major changes; documentation reviews MUST be part of PR processes.
Rationale: Ensures maintainability, smooth onboarding, and prevents knowledge silos in a growing team.

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

## Development Workflow & Quality Gates

1. Product discovery tickets MUST specify acceptance criteria mapped to the governing principles before development begins.
2. Every feature branch MUST start with tests or contract updates demonstrating the expected behavior.

**Version**: 1.4.1 | **Ratified**: 2025-10-02 | **Last Amended**: 2025-10-12
