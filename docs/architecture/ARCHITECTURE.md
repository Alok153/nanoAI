# nanoAI Architecture

## System Overview

nanoAI follows clean architecture with unidirectional data flow from UI → Domain → Data layers. The codebase uses a primary app module with feature-based organization and performance benchmarking.

```
🎯 Application Module (:app)
├── MainActivity - single activity architecture
├── Feature orchestration (chat, library, settings, etc.)
└── Core systems integration

⚡ Benchmark Module (:macrobenchmark)
├── Performance testing for :app
├── Cold start measurements
├── Frame rate analysis & Jank detection
└── Memory profiling & baseline validation

Feature Organization (:app/feature/*)
├── 6 active features with clean architecture layers
├── data/ - repositories, DAOs, service interactions
├── domain/ - use cases, business models, validation
├── presentation/ - ViewModels, UI state, state holders
└── ui/ - Compose screens, components, theming

Core Infrastructure (:app/core/*)
├── common/ - shared utilities and extensions
├── data/ - persistence, network, configuration
├── device/ - hardware access, camera, sensors
├── di/ - dependency injection bindings
├── domain/ - cross-feature business logic
├── maintenance/ - migrations, cleanup operations
├── model/ - shared enums, types, constants
├── network/ - HTTP clients, interceptors, gateways
├── runtime/ - ML runtime management & backends
├── security/ - encryption, key management
└── telemetry/ - analytics, error reporting

Data Sources
├── Room DB (8 DAOs: ChatThread, Message, Persona, Model, Download, ApiConfig, etc.)
├── DataStore (preferences, privacy settings, UI state)
├── WorkManager (ModelDownloadWorker, background tasks)
└── File System (caches, downloads, persistent storage)

External Systems
├── MediaPipe (on-device inference, LoRA support)
├── Hugging Face Hub (model catalog, metadata)
├── Cloud APIs (OpenAI, Gemini, custom endpoints)
└── Device APIs (storage, networking, hardware)
```

## Key Data Flows

### Message Generation
```
User Input → ChatScreen → ChatViewModel → SendPromptAndPersonaUseCase
    ├── Save to Room DB (ConversationRepository)
    └── Route via InferenceOrchestrator
        ├── Local: MediaPipe runtime
        └── Cloud: Retrofit API calls
```

### Model Download
```
Download Tap → ModelLibraryScreen → ModelLibraryViewModel → ModelDownloadsAndExportUseCase
    ├── Queue in Room DB (DownloadManager)
    ├── Background download (WorkManager/ModelDownloadWorker)
    │   ├── Progress updates + checksum validation
    │   └── File storage to cache directory
    └── Update model status (ModelCatalogRepository)
```

### Profile Synchronization
```
App Launch → NavigationScaffold → ThemeViewModel + UIStateViewModel → ObserveUserProfileUseCase
    ├── Local: Room DB + DataStore (encrypted preferences)
    └── Remote: /user/profile API (when online)
        └── Merge flows → offline-first UI state

Theme Changes → ThemeViewModel → UpdateThemePreferenceUseCase
    ├── Encrypted DataStore writes
    ├── Room transaction for UI state
    └── Background sync when online (WorkManager)
```

### Privacy & Offline Support
- **Encrypted Storage**: Sensitive preferences in DataStore, telemetry redacted
- **Offline Continuity**: Room caches enable full functionality without network
- **Consent Gates**: All data sharing requires explicit user opt-in
- **Background Sync**: WorkManager handles reconciliation when connectivity returns

## Dependency Injection

Hilt provides clean separation with module-based configuration:

- **DatabaseModule**: NanoAIDatabase, 8 DAOs, type converters
- **NetworkModule**: Retrofit, OkHttpClient, CloudGateway
- **RepositoryModule**: All repository implementations
- **Use Cases**: Constructor-injected business logic
- **ViewModels**: @HiltViewModel with injected dependencies
- **UI**: hiltViewModel() for Compose injection

## Database Schema

Room database with 8 entities supporting offline-first functionality:

- **ChatThread**: Conversation threads with persona/model associations
- **Message**: Individual messages with role, content, and metadata
- **PersonaProfile**: AI personality configurations and preferences
- **PersonaSwitchLog**: Conversation persona change history
- **ModelPackage**: Downloadable AI models with metadata and install state
- **DownloadTask**: Background download progress and status tracking
- **ApiProviderConfig**: Cloud API endpoint configurations
- **UserProfile/UIState**: Cached personalization and layout preferences

All entities include proper foreign key relationships and indexing for performance.

## ViewModel Architecture

Distributed responsibility pattern ensures clean separation and testability across feature modules:

### Core Feature ViewModels
- **ChatViewModel**: Message state, sending prompts, persona switching, conversation management
- **ModelLibraryViewModel**: Model catalog browse, download management, progress tracking, Hugging Face integration
- **SettingsViewModel**: API configurations, export/import, privacy preferences, backup management

### Navigation & State ViewModels (Distributed)
- **NavigationScaffoldViewModel**: Route coordination between screens, drawer state, back stack management
- **ConnectivityViewModel**: Network reachability monitoring, offline banner display, sync status
- **ProgressViewModel**: Background operation tracking (downloads, exports), queue status, cancellation
- **ThemeViewModel**: Theme preferences, Material 3 theming, accessibility settings
- **UIStateViewModel**: Screen-specific preferences, layout caching, user personalization

### Architecture Benefits
- **Horizontal Scaling**: Feature ViewModels can evolve independently without merge conflicts
- **Test Isolation**: Each ViewModel testable in isolation (≥75% coverage)
- **Performance**: Smaller memory footprint, faster cold starts, on-demand loading
- **Maintainability**: Single responsibility pattern, safer refactoring, simpler debugging
- **Clean Architecture**: Clear separation between UI coordination and business logic

## Quality Standards

### UI & Accessibility
- **Accessibility**: WCAG AA compliance with 48dp touch targets, proper contrast ratios, semantic markup
- **Material Design 3**: Consistent theming, spacing, typography, and elevation
- **Performance**: <1.5s cold start, <5% frame drops, <500ms queue operations

### Code Quality
- **Linting**: Spotless formatting, Detekt static analysis, Android accessibility lint
- **Testing**: ≥75% ViewModel, ≥65% UI, ≥70% Data layer coverage
- **Manual Testing**: TalkBack accessibility validation

*See `docs/UI_COMPONENTS.md` for detailed UI implementation guidelines.*

## State Management

Reactive flows ensure unidirectional data flow and lifecycle awareness:

- **StateFlow**: UI state (messages, loading states) - survives config changes
- **Flow**: Repository data streams - lazy, cold observables
- **SharedFlow**: One-time events (errors, navigation) - hot, no initial value

UI collects flows via `.collectAsState()` for automatic recomposition and lifecycle management.

## Technical Foundations

### Thread Safety
All operations are main-thread safe:
- **Room**: Coroutine-based DAO operations
- **DataStore**: IO dispatcher for reads/writes
- **ViewModels**: viewModelScope lifecycle management
- **WorkManager**: Background thread execution

### Testing Strategy
Comprehensive test coverage across layers:
- **Unit Tests**: 126+ JVM tests (DAO, UseCase, ViewModel logic)
- **Instrumentation**: Device tests (Compose UI, Room operations, WorkManager)
- **Macrobenchmarks**: Performance validation (cold start <1.5s, frame drops <5%)

### Performance Monitoring
- **JankStats**: Frame hitch detection (>32ms logged for regression triage)
- **Baseline Profiles**: Optimized startup and navigation paths
- **Metrics State**: Lightweight performance overlays in debug builds

### Accessibility
Semantic markup ensures screen reader compatibility:
- Progress panels with state descriptions and range info
- Landmark navigation and heading hierarchy
- Status announcements for connectivity changes
- Predictive focus order for complex UIs

---

**Data Flow Legend**: `└─►` Primary flow, `├─►` Alternative path, `│` Dependencies
