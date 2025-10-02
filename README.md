# nanoAI - Offline Multimodal AI Assistant

A privacy-first, offline-capable Android AI assistant powered by on-device inference with MediaPipe and cloud fallback support.

## Features

### 🔒 Privacy First
- **100% Local Processing**: All conversations stay on your device
- **No Cloud Required**: Works completely offline with local models
- **Optional Cloud Fallback**: Connect to OpenAI/Gemini APIs when needed
- **Granular Controls**: Choose data retention policies and telemetry preferences

### 🤖 Multimodal AI
- **Multiple Personas**: Switch between different AI personalities with custom system prompts
- **Model Management**: Download and manage AI models for offline use
- **Flexible Routing**: Automatically route between local inference and cloud APIs
- **MediaPipe Integration**: Leverages Google's MediaPipe v0.10.14 for on-device inference

### 💬 Conversation Management
- **Thread Organization**: Organize conversations into threads with titles
- **Archive & Search**: Archive old threads and search across conversations
- **Persona Switching**: Change personas mid-conversation with logging
- **Export Backup**: Export all your data for backup or migration

### ⚙️ Advanced Configuration
- **API Provider Management**: Configure multiple cloud API providers (OpenAI, Gemini, custom)
- **Model Library**: Browse and download AI models from a catalog
- **Download Queue**: Manage concurrent downloads with pause/resume/cancel support
- **Background Processing**: Downloads continue in the background using WorkManager

## Architecture

### Clean Architecture Layers

```
UI Layer (Compose)
    ↓
ViewModels (StateFlow)
    ↓
Use Cases (Domain Logic)
    ↓
Repositories (Data Orchestration)
    ↓
Data Sources (Room + DataStore + WorkManager)
```

### Key Components

**Presentation Layer**
- `ChatViewModel` - Message flows, persona switching, thread management
- `ModelLibraryViewModel` - Model catalog, downloads, filtering
- `SettingsViewModel` - API provider CRUD, export, privacy settings
- `SidebarViewModel` - Thread list, search, archive management

**Domain Layer**
- `SendPromptAndPersonaUseCase` - Inference routing and persona management
- `ModelDownloadsAndExportUseCase` - Download orchestration and backup export
- `InferenceOrchestrator` - Routes between local and cloud inference

**Data Layer**
- `ConversationRepository` - Chat threads and messages
- `PersonaRepository` - Persona profiles and configurations
- `ModelCatalogRepository` - Model package metadata
- `ApiProviderConfigRepository` - Cloud API configurations
- `DownloadManager` - Background download orchestration

**Database Schema (Room)**
- `ChatThread` - Conversation threads with metadata
- `Message` - Individual messages with role, content, latency
- `PersonaProfile` - AI persona configurations
- `PersonaSwitchLog` - Audit log of persona changes
- `ModelPackage` - AI model metadata and paths
- `DownloadTask` - Download queue with progress tracking
- `ApiProviderConfig` - Cloud API endpoint configurations

## Tech Stack

- **Language**: Kotlin 1.9.x
- **UI**: Jetpack Compose + Material 3
- **DI**: Hilt
- **Database**: Room
- **Preferences**: DataStore
- **Background Work**: WorkManager
- **Networking**: Retrofit + OkHttp + Kotlin Serialization
- **AI Runtime**: MediaPipe Generative v0.10.14 (LiteRT)
- **Image Loading**: Coil
- **Async**: Kotlin Coroutines + Flow
- **DateTime**: kotlinx-datetime

## Installation

### Prerequisites
- Android Studio Hedgehog | 2023.1.1 or later
- JDK 11 or later
- Android SDK with API 31+ (minSdk 31, compileSdk 36)
- Physical device or emulator running Android 12+ (API 31+)

### Build Instructions

1. **Clone the repository**
```bash
git clone https://github.com/vjaykrsna/nanoAI.git
cd nanoAI
```

2. **Open in Android Studio**
```bash
studio .
```

3. **Build the project**
```bash
./gradlew assembleDebug
```

4. **Run tests**
```bash
# Unit tests
./gradlew testDebugUnitTest

# Instrumented tests (requires connected device/emulator)
./gradlew connectedDebugAndroidTest
```

5. **Install on device**
```bash
./gradlew installDebug
```

## Project Structure

```
nanoAI/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/vjaykrsna/nanoai/
│   │   │   │   ├── core/              # Core framework components
│   │   │   │   │   ├── data/          # Data layer (repositories, DAOs, entities)
│   │   │   │   │   ├── domain/        # Domain models
│   │   │   │   │   ├── network/       # Network clients (Retrofit)
│   │   │   │   │   ├── runtime/       # AI inference runtime
│   │   │   │   │   └── di/            # Dependency injection modules
│   │   │   │   ├── feature/           # Feature modules
│   │   │   │   │   ├── chat/          # Chat conversation feature
│   │   │   │   │   ├── library/       # Model library feature
│   │   │   │   │   ├── settings/      # Settings feature
│   │   │   │   │   └── sidebar/       # Navigation sidebar
│   │   │   │   └── ui/                # Shared UI components
│   │   │   │       └── navigation/    # Navigation scaffold
│   │   │   └── res/                   # Android resources
│   │   ├── androidTest/               # Instrumented tests
│   │   └── test/                      # Unit tests
│   ├── build.gradle.kts
│   └── schemas/                       # Room database schemas
├── specs/                             # Design specifications
│   └── 001-foundation/
│       ├── plan.md                    # Implementation plan
│       ├── tasks.md                   # Task breakdown
│       ├── data-model.md              # Data model spec
│       ├── research.md                # Research notes
│       └── contracts/                 # API contracts
├── gradle/
│   └── libs.versions.toml             # Dependency version catalog
└── build.gradle.kts
```

## Testing

### Test Coverage

The project follows Test-Driven Development (TDD) principles with comprehensive test coverage:

- **126+ Unit Tests**: Domain logic, use cases, repositories
- **Contract Tests**: API schema validation against OpenAPI specs
- **Instrumented Tests**: Full user flow validation on real devices
- **Benchmark Tests**: Performance profiling and baseline profiles

### Running Tests

```bash
# All unit tests
./gradlew test

# Specific test suite
./gradlew :app:testDebugUnitTest

# Contract validation tests
./gradlew :app:test --tests "*ContractTest"

# Instrumented tests (requires device)
./gradlew connectedAndroidTest

# Benchmarks (requires release build)
./gradlew :macrobenchmark:connectedBenchmarkReleaseAndroidTest

# CI parity checks
./gradlew ktlintCheck detekt lintDebug testDebugUnitTest
```

> **Note:** The `ImportExportContractTest` assertions intentionally fail until the export payload fully matches the published OpenAPI contract. Use the reports emitted under `app/build/reports/tests/testDebugUnitTest` to track progress while implementing the remaining export changes.

## Usage

### First Launch

1. **Grant Permissions**: Allow storage access for model downloads
2. **Review Disclaimer**: On the first run, acknowledge the safety disclaimer to record consent (dismiss to review the copy again later)
3. **Configure API (Optional)**: Add cloud API providers in Settings if desired
4. **Download Models**: Visit Model Library to download AI models for offline use
5. **Start Chatting**: Create a new conversation and select a persona

### Validating the First-launch Disclaimer

1. Clear prior state to simulate a new install: `adb shell pm clear com.vjaykrsna.nanoai`
2. Launch the app and confirm the disclaimer dialog appears with **Acknowledge** and **Remind Me Later** actions.
3. Tap **Acknowledge** to persist consent; relaunching the app should no longer show the dialog.
4. Repeat the clear-data command and tap **Remind Me Later** to verify the dialog returns on the next launch.

### Sidebar Inference Toggle

1. Open the navigation sidebar and locate the **Inference Mode** toggle.
2. Switch between **Local** and **Cloud**; the selection is saved via DataStore.
3. Close and relaunch the app (or run `adb shell am force-stop com.vjaykrsna.nanoai`) to confirm the toggle restores your last choice.

### Managing Personas

1. Navigate to Settings
2. Create custom personas with system prompts
3. Configure temperature, top-p, and model preferences
4. Switch personas during conversations via dropdown

### Downloading Models

1. Navigate to Model Library
2. Browse available models by capability
3. Tap download icon to queue downloads
4. Monitor progress in download queue section
5. Pause/resume/cancel downloads as needed

### Exporting Data

1. Navigate to Settings → Data Management
2. Tap "Export Backup"
3. Review export contents
4. Backup saved to Downloads folder as JSON

## Privacy & Data

### Data Storage

All data is stored locally on your device:
- **Database**: Room SQLite database in app private storage
- **Preferences**: DataStore encrypted preferences
- **Models**: Downloaded models in app cache directory
- **Exports**: Backup files in public Downloads folder

### No Cloud Sync

- No automatic cloud synchronization
- No telemetry or analytics by default
- No external data sharing without explicit user action
- All inference happens on-device unless cloud API configured

### Telemetry Controls

Users can opt-in to anonymous usage analytics in Settings → Privacy. This includes:
- App crashes and errors
- Feature usage statistics
- Performance metrics

**Telemetry is disabled by default.**

## Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Write tests for new functionality
4. Ensure all tests pass (`./gradlew test connectedAndroidTest`)
5. Run code quality checks (`./gradlew ktlint detekt`)
6. Commit your changes (`git commit -m 'Add amazing feature'`)
7. Push to the branch (`git push origin feature/amazing-feature`)
8. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- **MediaPipe Team**: For the excellent on-device ML framework
- **Google Gemini**: For cloud inference API
- **OpenAI**: For ChatGPT API compatibility
- **Android Team**: For Jetpack Compose and modern Android libraries

## Contact

**Vijay Krishna** - [@vjaykrsna](https://github.com/vjaykrsna)

Project Link: [https://github.com/vjaykrsna/nanoAI](https://github.com/vjaykrsna/nanoAI)

---

**Built with ❤️ for privacy-conscious AI enthusiasts**
