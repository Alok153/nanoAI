# nanoAI – AI Assistant for Android

## Project Overview

nanoAI is a privacy-first Android application that brings powerful AI capabilities directly to your device. The app enables users to chat with AI, generate images, process audio, get coding help, and translate languages – all while keeping data private and secure. The app is built with a local-first architecture that runs small LLMs on-device for privacy and speed, with cloud fallbacks when needed or for larger models.

### Key Features
- **Privacy by Design** – Conversations, personal data, and AI models stay on your device
- **Offline Capabilities** – Works without internet for most features once models are downloaded
- **Multimodal AI** – Chat, image generation, audio processing, code assistance, and translation
- **Beautiful & Accessible** – Clean, intuitive interface with full TalkBack support and Material 3 design
- **Flexible & Extensible** – Add cloud AI providers (OpenAI, Gemini, custom endpoints) or use local models
- **Responsible AI** – No automated content filters; users are responsible for generated content

### Technology Stack
- **Language**: Kotlin 1.9.x
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: Clean architecture with domain, data, and presentation layers
- **Dependency Injection**: Hilt
- **Database**: Room with SQLite
- **Preferences**: DataStore
- **Networking**: Retrofit with OkHttp
- **Background Tasks**: WorkManager
- **Coroutines**: For asynchronous operations
- **MediaPipe**: For on-device AI inference
- **Testing**: JUnit 5, MockK, Compose UI Tests, Robolectric

## Architecture

The project follows clean architecture principles with the following layers:

### UI Layer (Compose)
- Compose-based UI with Material 3 components
- Navigation using Jetpack Compose Navigation
- Accessible components with full TalkBack support

### Presentation Layer (ViewModels)
- HiltViewModel annotated ViewModels
- State management using StateFlow and SharedFlow
- Lifecycle-aware with viewModelScope

### Domain Layer (Use Cases)
- Business logic encapsulated in use cases
- Repository pattern for data abstraction
- Platform-agnostic Kotlin code

### Data Layer (Repositories)
- Data access through repositories
- Multiple data sources (Room DB, DataStore, Network)
- Background processing with WorkManager

### External Systems
- MediaPipe for on-device inference
- Cloud APIs (OpenAI, Google Gemini, custom endpoints) as optional fallback
- Device storage for models and user data

## Building and Running

### Prerequisites
- Android Studio (latest version)
- Android SDK with API level 31+
- Android SDK Build Tools
- Android NDK (for native libraries, if needed)

### Setup Instructions
```bash
# Clone the repository
git clone https://github.com/vjaykrsna/nanoAI.git
cd nanoAI

# Build the project
./gradlew build

# Install debug version on connected device/emulator
./gradlew installDebug

# Run unit tests
./gradlew testDebugUnitTest

# Run instrumentation tests
./gradlew connectedDebugAndroidTest

# Generate full coverage report
./gradlew jacocoFullReport

# Run full check (build, tests, lint, coverage verification)
./gradlew check
```

### Development Commands
```bash
# Run only unit tests
./gradlew testDebugUnitTest

# Run specific test class
./gradlew testDebugUnitTest --tests "com.vjaykrsna.nanoai.*ViewModelTest"

# Run instrumentation tests with managed device (CI-like setup)
./gradlew ciManagedDeviceDebugAndroidTest

# Generate and view coverage report
./gradlew jacocoFullReport
# Report available at: app/build/reports/jacoco/full/html/index.html

# Run coverage verification against thresholds
./gradlew verifyCoverageThresholds

# Generate markdown coverage summary
./gradlew coverageMarkdownSummary
```

## Development Conventions

### Code Style
- Follow official Kotlin coding conventions
- Use KTFmt with Google style formatting (enforced by Spotless)
- Use descriptive variable and function names
- Prefer immutable data structures when possible
- Use coroutines for asynchronous operations
- Apply `@OptIn` annotations for experimental APIs

### Testing Strategy
- **Unit Tests**: JVM-based tests for ViewModels, Use Cases, Repositories (target ≥75% coverage)
- **Instrumentation Tests**: UI and integration tests on real devices/emulators (target ≥65% coverage)
- **DAO Tests**: Database operations with in-memory Room
- **Contract Tests**: API schema validation
- **Macrobenchmarks**: Performance tests for startup and UI jank

### Quality Gates
- Code formatting enforced by Spotless
- Static analysis by Detekt
- Test coverage thresholds: ViewModel (75%), UI (65%), Data layer (70%)
- All tests must pass before merging

### Architecture Patterns
- **MVVM**: Model-View-ViewModel pattern with Jetpack Compose
- **Repository Pattern**: Abstract data sources behind repository interfaces
- **Use Cases**: Encapsulate business logic in single-responsibility classes
- **Dependency Injection**: Use Hilt for dependency management
- **State Management**: Use StateFlow, SharedFlow, and Flow for reactive programming
- **Offline-First**: Design for offline capability with sync when online

### Security and Privacy
- All personal data stored locally using Room database
- API keys stored securely (not in version control)
- Encryption for sensitive data storage
- Privacy-first design with user consent for any data sharing
- No PII in telemetry by default

### Accessibility
- Full TalkBack support for visually impaired users
- Semantic properties for screen readers
- Sufficient color contrast ratios
- Support for various font sizes and display preferences
- Material 3 design principles for consistent UX

## Project Structure
```
nanoAI/
├── app/                    # Main application module
│   ├── src/main/java/      # Kotlin source code
│   │   └── com.vjaykrsna.nanoai/
│   │       ├── coverage/   # Coverage reporting utilities
│   │       ├── data/       # Data layer (DAOs, repositories, data sources)
│   │       ├── domain/     # Domain layer (use cases, models)
│   │       ├── feature/    # Feature modules (chat, library, settings)
│   │       ├── telemetry/  # Telemetry and analytics
│   │       └── ui/         # UI layer (screens, composables)
│   ├── src/test/           # Unit tests
│   └── src/androidTest/    # Instrumentation tests
├── docs/                   # Documentation
├── config/                 # Configuration files (detekt, coverage, etc.)
├── scripts/                # Build and utility scripts
├── openspec/               # Specification-driven development files
└── macrobenchmark/         # Performance benchmark module
```

## Testing Guidelines

### Unit Tests
- Located in `app/src/test/java`
- Test ViewModels, Use Cases, and Repository logic
- Use MockK for mocking dependencies
- Use kotlinx-coroutines-test for coroutine testing
- Use Truth for assertions

### Instrumentation Tests
- Located in `app/src/androidTest/java`
- Test UI interactions and end-to-end flows
- Use Compose UI testing for declarative UI
- Test accessibility and offline scenarios

### Coverage Requirements
- ViewModel layer: ≥75% coverage
- UI layer: ≥65% coverage
- Data layer: ≥70% coverage
- Coverage verification is part of the CI check process

## API Integration

### Cloud Provider Support
- OpenAI API compatibility
- Google Gemini API compatibility
- Custom endpoint support
- Automatic fallback between local and cloud models

### Model Management
- Download and manage AI models optimized for device
- Checksum validation for downloaded models
- Progress tracking and pause/resume functionality
- Local model execution using MediaPipe

## Contributing

### Development Workflow
1. Fork and clone the repository
2. Create a feature branch from the main branch
3. Make changes following the coding conventions
4. Write tests for new functionality
5. Run the full test suite with `./gradlew check`
6. Submit a pull request with a clear description

### Quality Checks
Before submitting changes, ensure:
- All tests pass (`./gradlew check`)
- Code formatting is correct (`./gradlew spotlessApply`)
- Coverage thresholds are met
- Static analysis passes (Detekt)
- Accessibility is maintained

### Documentation
- Update relevant documentation in the `docs/` directory
- Add or update API documentation in `docs/API.md`
- Update architecture diagrams if necessary
- Include examples and usage instructions

## Special Considerations

### Performance
- Use baseline profiles to optimize startup time
- Monitor for UI jank with JankStats
- Optimize for different device capabilities
- Efficient data loading and caching strategies

### Privacy and Security
- All data processing happens on-device by default
- Clear data retention policies
- Secure storage for API keys and sensitive data
- Explicit user consent for any data sharing

### Accessibility
- Full TalkBack compatibility
- Semantic properties for all interactive elements
- Support for various accessibility settings
- Regular accessibility testing

This AGENTS.md provides a comprehensive overview of the nanoAI project to help AI assistants understand the codebase, development practices, and project conventions for effective collaboration and development.