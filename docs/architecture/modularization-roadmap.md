# nanoAI Modularization Roadmap

_Last updated: 2025-12-01_

## Why modularize now?
- **Architecture enforcement:** Isolate `core` infrastructure from feature UI so the clean layer rules (UseCase → Repository → DataSource) stay enforceable at build time.
- **Faster iteration:** Smaller Gradle modules enable parallel compilation, targeted tests, and incremental adoption of new features without re-building the world.
- **Safer releases:** Module-specific quality gates (Detekt, coverage) catch regressions before they reach the monolithic `:app` module.

## Current snapshot
- Active modules: `:app`, `:macrobenchmark`.
- All core, feature, and shared code is compiled inside `:app`, so every change invalidates the same gigantic artifact.
- Features contain `presentation` and `ui` packages only; their domain/data logic lives under `core/`.

## Target end state
```
nanoAI
├── core:common         # extensions, dispatchers, error envelopes
├── core:data           # Room DB, Retrofit gateways, repositories
├── core:domain         # use cases, models, platform-agnostic logic
├── feature:chat        # chat domain facade + presentation/ui
├── feature:audio       # audio domain facade + presentation/ui
├── feature:image       # image domain facade + presentation/ui
├── feature:library     # catalog domain facade + presentation/ui
├── feature:settings    # preferences domain facade + presentation/ui
├── feature:uiux        # shared composables, theming
├── app                 # Android entry point wiring everything together
└── macrobenchmark      # existing perf module
```

## Phased execution plan

### Phase 0 – Pre-flight (in-progress)
1. Align documentation with the actual structure (done via `project-structure.md`).
2. Record decisions + risks in this roadmap.
3. Tag current package owners and ensure coverage baselines stay green when modules split.

### Phase 1 – Extract shared `core` modules
_Status: `core:common` and `core:domain` were extracted from `:app` on 2025-12-01; `core:data` is next._
1. **Create `core:common`**: move dispatcher qualifiers, `NanoAIResult`, logging helpers; update imports.
2. **Create `core:domain`**: relocate `core/domain/**`, keeping package names so existing callers only update Gradle deps.
3. **Create `core:data`**: move repositories, Room DAOs, Retrofit services, WorkManager jobs.
4. Update Hilt modules so feature code depends on `core:domain` only; `core:data` implements the repositories and binds via Hilt entry points.
5. Add module-specific `detekt` + `spotless` configs via existing build-logic plugins.

### Phase 2 – Feature slices
1. Introduce `feature:{name}` modules starting with `chat` (highest coupling).
2. Move `feature/chat/presentation` + `ui` into the new module unchanged.
3. Add a thin `feature/chat/domain` package that aggregates `core:domain` use cases into feature-focused facades so composables only touch feature APIs.
4. Repeat for audio, image, library, settings, uiux.
5. Wire each module into `app` through Hilt entry points and navigation graph registrations.

### Phase 3 – Optional dynamic delivery
1. Evaluate turning `feature:audio` and `feature:image` into Play Feature Delivery modules if APK size or confidentiality requires it.
2. Gate load-time dependencies via the feature module, not the base app.

## Implementation guardrails
- **Tests per phase:** Duplicate existing JVM tests into the new modules first, then delete the originals to keep coverage intact.
- **CI configuration:** Update `settings.gradle.kts` + quality gates so every new module participates in `spotlessCheck`, `detekt`, `testDebugUnitTest`, and `verifyCoverageThresholds`.
- **Dependency direction:**
  - `feature:*` → `core:domain` (never the reverse).
  - `core:domain` → `core:common` only.
  - `core:data` → `core:domain` (implementing repositories), but never `feature:*`.
- **Offline + accessibility:** Re-run `TestEnvironmentRule` scenarios and accessibility snapshots whenever a module boundary changes to avoid regressions.

## Next actionable steps
1. Draft Gradle module stubs for `core:common`, `core:domain`, `core:data` using build-logic templates.
2. Move the chat domain/data packages behind those modules to validate the dependency graph.
3. Update `docs/architecture/ARCHITECTURE.md` once the first module extraction lands.
4. Track progress in `Personal/findings.md` so the historical context stays discoverable.
