# nanoAI Project Structure

This document outlines the nanoAI project's directory structure, based on active modules and real codebase patterns.

## 📦 Module Architecture

nanoAI uses a main app module with a performance benchmark module:

```
nanoAI/
├── app/                    # 📱 Main Android application
├── macrobenchmark/         # ⚡ Performance testing module
└── [build infrastructure]  # Gradle, config, scripts
```

**Active Modules (settings.gradle.kts):**
- `:app` - Main application with all features
- `:macrobenchmark` - Cold start & frame rate testing

**Inactive Directories:**
- `library/` - Model management (not built)
- `network/` - HTTP abstractions (not built)

## 📁 App Module Structure

### Core Infrastructure (`app/src/main/java/com/vjaykrsna.nanoai/`)

```
app/core/
├── common/                 # Shared utilities & extensions
├── coverage/               # Test coverage reporting & analysis
├── data/                   # Room DAOs, network services, repositories
├── device/                 # Camera, storage, hardware access
├── di/                     # Hilt modules & dependency bindings
├── domain/                 # Business logic & use cases
├── maintenance/            # DB migrations, cleanup jobs
├── model/                  # Shared enums & type definitions
├── network/                # HTTP clients & interceptors
├── runtime/                # ML runtime management
├── security/               # Encryption & hashing utilities
└── telemetry/              # Analytics & error reporting
```

### Feature Organization (`app/src/main/java/com/vjaykrsna.nanoai/feature/`)

Features use clean architecture with 4 consistent layers:

```
feature/{name}/
├── data/                   # Feature repositories & DAOs
├── domain/                 # Feature models & use cases
├── presentation/           # ViewModels & UI state
└── ui/                     # Compose components & screens
```

**Active Features:**
- `audio/` - Audio processing & playback
- `chat/` - Conversation management
- `image/` - Image operations & display
- `library/` - Model catalog & downloads
- `settings/` - Configuration & backup
- `uiux/` - Shared UI components

## 🛠️ Build & Configuration

### Build Scripts (`gradle/`, `config/`, `scripts/`)

```
gradle/
├── build.gradle.kts        # Root build script
└── libs.versions.toml      # Version catalog (one source of truth)

config/                     # Shared build configuration
├── build/                  # Gradle convention plugins
├── quality/                # Code analysis & linting
│   ├── detekt/             # Static analysis rules
│   ├── accessibility/      # UI accessibility baselines
│   └── ui-quality/         # Design system compliance
└── testing/                # Test coverage & resources

scripts/                    # Build & development helpers
├── accessibility-notes.md
├── capture-screenshots.sh
└── coverage/
```

**Quality Gates:**
- Detekt (static analysis)
- Accessibility Lint (WCAG compliance)
- UI Quality (Material Design validation)
- Coverage Reports (layer-specific thresholds)

### Test Organization

**Unit Tests:** `src/test/java/` (JVM tests, no Android runtime)
**Instrumentation:** `src/androidTest/java/` (device/emulator tests)
**Benchmark:** `src/benchmark/` (performance suites)

```
src/test/resources/          # JVM test resources
├── contracts/              # API contract specs (.yaml, .json)
├── schemas/                # Validation schemas
└── robolectric.properties  # Robolectric config

app/assets/                 # Embedded runtime assets
├── coverage/               # Coverage baseline data
├── fonts/                  # Custom typography
└── models/                 # ML model manifests
```

## 📚 Documentation & Specs

```
docs/                       # Project documentation
├── api/                    # API specs (broken down)
├── architecture/           # System architecture
├── development/            # Dev guidelines & guides
├── features/               # Feature-specific docs
└── ui/                     # UI component docs

specs/                      # Formal specifications
├── 001-foundation/         # Base system contracts
├── 003-ui-ux/             # UI/UX specifications
└── 005-test-coverage/     # Coverage requirements
```

## 🔧 Development Tools & IDE

### Excluded Directories (Local Setup)
These directories exist locally but are excluded from version control:

```
.cursor/                     # Cursor AI editor extensions
.idea/                      # IntelliJ IDEA settings
.specify/                   # Design specification tools
.trunk/                     # Trunk.io code quality
.vscode/                    # VS Code configuration
```

**Purpose:** Standardized local development environment across team members.



## 📋 Naming & File Conventions

### Package Structure
```
com.vjaykrsna.nanoai.{module}.{layer}.{feature}
```

**Examples:**
- `com.vjaykrsna.nanoai.app.feature.chat.data` - Chat repository
- `com.vjaykrsna.nanoai.core.domain` - Core business logic
- `com.vjaykrsna.nanoai.core.data.network` - HTTP clients

### Class Naming Patterns

**Architecture Layer Suffixes:**
- Models: `*Model`, `*Dto`, `*Entity`
- ViewModels: `*ViewModel`, `*StateHolder`
- Use Cases: `*UseCase`, `*Orchestrator`
- Repositories: `*Repository`, `*DataSource`
- Services: `*Service`, `*Manager`, `*Client`

**File Organization:**
```
FeatureScreen.kt           # Main screen composable
FeatureComponents.kt       # Feature-specific components
FeatureViewModel.kt        # ViewModel implementation
FeatureUiState.kt          # UI state data classes
FeatureEvents.kt           # User actions (sealed classes)
FeatureRepository.kt       # Repository interface
FeatureRepositoryImpl.kt   # Repository implementation
```

### Resource Naming
- **Strings:** `feature_action_label`
- **Drawables:** `IcFeatureAction.xml`, `BgFeatureBubble.xml`
- **Ids:** Lowercase with underscores: `feature_input_field`

## 🚀 Development Workflow

### Feature Development Lifecycle
1. **Spec:** Define in `specs/` directory
2. **Implement:** Follow `feature/*/data|domain|ui` pattern
3. **Test:** Unit tests (≥75% coverage) + Instrumentation
4. **Review:** Detekt + Accessibility + Code style checks
5. **Merge:** Coverage validation + Benchmarks

### Code Quality Pipeline
```
PR → Static Analysis → UI Checks → Benchmarks → Publish
     ↓               ↓            ↓          ↓
  Detekt       Accessibility  Cold Start  Frame Drops
  Formatting   Material Design Memory    Jank Stats
```

## 📊 Quality & Performance

### Project Standards
- **Cold Start:** <1.5s target, 3s failure threshold
- **Frame Drops:** <5% jank rate acceptable
- **Accessibility:** WCAG AA compliance required
- **Test Coverage:** 75%+ ViewModel, 70%+ Data, 60%+ UI

### Build Variants
- **Debug:** Testing and development
- **Release:** Production builds with R8/ProGuard
- **Benchmark:** Performance measurement builds

This structure supports nanoAI's multi-module architecture while maintaining clean separation of concerns and consistent development practices across the engineering team.
