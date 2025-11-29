# nanoAI Project Structure

## Module Architecture

```
nanoAI/
├── app/                    # Main Android application
├── macrobenchmark/         # Performance testing module
└── [build infrastructure]  # Gradle, config, scripts
```

**Active Modules:** `:app`, `:macrobenchmark`

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

Features use clean architecture with 4 layers:

```
feature/{name}/
├── data/           # Feature repositories & DAOs
├── domain/         # Feature models & use cases
├── presentation/   # ViewModels & UI state
└── ui/             # Compose components & screens
```

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
