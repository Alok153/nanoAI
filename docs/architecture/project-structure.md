# nanoAI Project Structure

## Module Architecture

```
nanoAI/
├── app/                    # Main Android application (features + presentation)
├── core/common             # Shared utilities and annotations
├── core/domain             # Business logic and contracts
├── core/data               # Data implementations (Room, Retrofit, DataStore, Hilt bindings)
├── core/testing            # Test utilities and fakes
├── macrobenchmark/         # Performance testing module
└── config/quality/detekt/custom-rules # Detekt custom rules
```

**Active Modules:** `:app`, `:core:common`, `:core:domain`, `:core:data`, `:core:testing`, `:macrobenchmark`, `:config:quality:detekt:custom-rules`

## App Module Structure

### Core Infrastructure (`core/`)

| Package | Purpose |
|---------|---------|
| `common/` | Shared utilities, extensions, notifications |
| `data/` | Room DAOs, network services, repositories |
| `device/` | Camera, storage, hardware access |
| `di/` | Hilt modules & dependency bindings |
| `domain/` | Business logic & use cases |
| `model/` | Shared enums & type definitions |
| `network/` | HTTP clients & interceptors |
| `runtime/` | ML runtime management |
| `security/` | Encryption & hashing utilities |

### Features (`feature/`)

Features now own end-to-end slices (presentation + domain + data) while reusing shared
contracts from `:core`. Current layout:

```
feature/{name}/
├── domain/        # Feature UseCases + coordinators
├── data/          # Feature repositories + data sources
├── presentation/  # ViewModels & UI state containers
└── ui/            # Compose components & screen graphs
```

Hilt modules in `feature/di` bind feature repositories to their interfaces and wire the shared
dispatchers/runtime gateways provided by `:core`. This keeps the Clean Architecture chain
(`Composable → ViewModel → UseCase → Repository → DataSource`) inside each feature folder while
still sharing primitives (models, DB entities, runtime gateways) from the core modules.

**Active Features:** `audio/`, `chat/`, `image/`, `library/`, `settings/`, `uiux/`

## Configuration (`config/`)

| Directory | Purpose |
|-----------|---------|
| `quality/detekt/` | Static analysis rules |
| `quality/accessibility-baseline.xml` | WCAG compliance |
| `testing/coverage/` | Coverage thresholds & layer maps |

## Test Organization

| Type | Location | Command |
|------|----------|---------|
| Unit (JVM) | `app/src/test/` | `./gradlew testDebugUnitTest` |
| Instrumentation | `app/src/androidTest/` | `./gradlew connectedAndroidTest` |
| Benchmark | `macrobenchmark/src/main/` | `./gradlew :macrobenchmark:verifyMacrobenchmarkPerformance` |

## Quality Standards

- **Coverage:** 75%+ ViewModel, 70%+ Data, 65%+ UI
- **Cold Start:** <1.5s target
- **Frame Drops:** <5% jank rate
- **Accessibility:** WCAG AA compliance
