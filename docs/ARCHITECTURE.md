# nanoAI Architecture Diagram

## System Overview

```
┌───────────────────────────────────────────────────────────────────────┐
│                          Android Application                          │
└───────────────────────────────────────────────────────────────────────┘
                                    │
                ┌───────────────────┼───────────────────┐
                │                   │                   │
                ▼                   ▼                   ▼
┌───────────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│   UI Layer (Compose)  │ │  Feature Modules │ │  Navigation      │
│  - ChatScreen         │ │  - chat/         │ │  - Scaffold      │
│  - ModelLibraryScreen │ │  - library/      │ │  - Drawer        │
│  - SettingsScreen     │ │  - settings/     │ │  - BottomNav     │
│  - NavigationScaffold │ │  - sidebar/      │ │  - Screen Routes │
└───────────────────────┘ └──────────────────┘ └──────────────────┘
                │                   │                   │
                └───────────────────┼───────────────────┘
                                    │
                                    ▼
                        ┌───────────────────────┐
                        │   Presentation Layer  │
                        │   (ViewModels)        │
                        └───────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
        ▼                           ▼                           ▼
┌──────────────────┐    ┌──────────────────────┐    ┌──────────────────┐
│  ChatViewModel   │    │ ModelLibraryViewModel│    │ SettingsViewModel│
│  - messages      │    │ - allModels          │    │ - apiProviders   │
│  - sendMessage   │    │ - downloadModel      │    │ - exportBackup   │
│  - switchPersona │    │ - pauseDownload      │    │ - privacy prefs  │
└──────────────────┘    └──────────────────────┘    └──────────────────┘
        │                           │                           │
        └───────────────────────────┼───────────────────────────┘
                                    │
                                    ▼
                        ┌───────────────────────┐
                        │    Domain Layer       │
                        │    (Use Cases)        │
                        └───────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
        ▼                           ▼                           ▼
┌──────────────────────┐  ┌────────────────────────┐  ┌──────────────────┐
│SendPromptAndPersona  │  │ModelDownloadsAndExport │  │InferenceOrchestra│
│UseCase               │  │UseCase                 │  │tor               │
│- sendPrompt()        │  │- downloadModel()       │  │- route inference │
│- switchPersona()     │  │- exportBackup()        │  │- local vs cloud  │
└──────────────────────┘  └────────────────────────┘  └──────────────────┘
        │                           │                           │
        └───────────────────────────┼───────────────────────────┘
                                    │
                                    ▼
                        ┌───────────────────────┐
                        │    Data Layer         │
                        │    (Repositories)     │
                        └───────────────────────┘
                                    │
    ┌───────────────┬───────────────┼───────────────┬───────────────┐
    │               │               │               │               │
    ▼               ▼               ▼               ▼               ▼
┌─────────┐  ┌──────────┐  ┌──────────────┐  ┌─────────-─┐  ┌──────────┐
│Conversa-│  │Persona   │  │ModelCatalog  │  │ApiProvider│  │Download  │
│tion Repo│  │Repo      │  │Repo          │  │ConfigRepo │  │Manager   │
└─────────┘  └──────────┘  └──────────────┘  └─────────-─┘  └──────────┘
    │               │               │               │               │
    └───────────────┴───────────────┼───────────────┴───────────────┘
                                    │
                                    ▼
                        ┌───────────────────────┐
                        │   Data Sources        │
                        └───────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────────┐
        │                           │                               │
        ▼                           ▼                               ▼
┌──────────────┐        ┌────────────────────┐          ┌──────────────────┐
│  Room DB     │        │  DataStore         │          │  WorkManager     │
│  (SQLite)    │        │  (Preferences)     │          │  (Background)    │
└──────────────┘        └────────────────────┘          └──────────────────┘
        │                           │                             │
        ▼                           ▼                             ▼
┌──────────────┐        ┌────────────────────┐          ┌───────────────────┐
│  7 DAOs      │        │ PrivacyPreference  │          │ModelDownloadWorker│
│  - ChatThread│        │ Store              │          │ - progress track  │
│  - Message   │        │ - telemetry opt-in │          │ - checksum verify │
│  - Persona   │        │ - retention policy │          │ - queue mgmt      │
│  - Model     │        │ - consent timestamp│          │                   │
│  - Download  │        └────────────────────┘          └───────────────────┘
│  - ApiConfig │
│  - SwitchLog │
└──────────────┘
        │
        ▼
┌───────────────────────────────────────────────────────────────┐
│              External Systems & Services                      │
├───────────────────────────────────────────────────────────────┤
│  📱 MediaPipe (Local Inference)                               │
│     - LiteRT runtime                                          │
│     - On-device model execution                               │
│     - LoRA adapter support                                    │
│                                                               │
│  ☁️  Cloud APIs (Optional Fallback)                           │
│     - OpenAI (GPT models)                                     │
│     - Google Gemini                                           │
│     - Custom endpoints                                        │
│                                                               │
│  💾 Device Storage                                            │
│     - App private storage (Room DB)                           │
│     - Cache directory (downloaded models)                     │
│     - Downloads folder (exports)                              │
└──────────────────────────────────────────────────────────-────┘
```

## Data Flow: Send Message

```
1. User types message
   └─► ChatScreen (UI)
       └─► ChatViewModel.sendMessage()
           └─► SendPromptAndPersonaUseCase.execute()
               ├─► ConversationRepository.saveMessage()
               │   └─► MessageDao.insert()
               │       └─► Room Database
               │
               └─► InferenceOrchestrator.generate()
                   ├─► [Local] MediaPipeLocalModelRuntime
                   │   └─► On-device inference
                   │
                   └─► [Cloud] CloudGatewayClient
                       └─► Retrofit → API call
```

## Data Flow: Download Model

```
1. User taps download
   └─► ModelLibraryScreen (UI)
       └─► ModelLibraryViewModel.downloadModel()
           └─► ModelDownloadsAndExportUseCase.queueDownload()
               ├─► DownloadManager.enqueue()
               │   ├─► DownloadTaskDao.insert()
               │   │   └─► Room Database
               │   │
               │   └─► WorkManager.enqueue()
               │       └─► ModelDownloadWorker
               │           ├─► HTTP download (OkHttp)
               │           ├─► Progress updates
               │           ├─► Checksum validation
               │           └─► File storage
               │
               └─► ModelCatalogRepository.updateModel()
                   └─► ModelPackageDao.update()
                       └─► Room Database
```

## Data Flow: UI/UX Profile Hydration

```
1. App launch
   └─► `NavigationScaffold`
       ├─► `AppViewModel` (global theme/offline state)
       └─► `ShellViewModel` → `HomeViewModel`
           └─► `ObserveUserProfileUseCase`
               ├─► `UserProfileRepository.observe()`
               │   ├─► `UserProfileLocalDataSource`
               │   │   ├─► `UserProfileDao.observe()` *(Room cached snapshot)*
               │   │   └─► `UiPreferencesStore.read()` *(DataStore theme + density prefs)*
               │   └─► `UserProfileRemoteDataSource.fetch()` *(Retrofit GET /user/profile)*
               └─► Merge flows → `UserProfile` domain model
                   └─► Emit `HomeUiState`
```

```
2. User toggles theme or compact mode
   └─► UI events (ThemeToggle, Settings)
       └─► `UpdateThemePreferenceUseCase` / `ToggleCompactModeUseCase`
           ├─► `UiPreferencesStore.update()` *(writes to encrypted DataStore)*
           ├─► `UserProfileRepository.syncLocal()` *(Room transaction for Layout/UI state)*
           └─► `SyncUiStateWorker.enqueue()` *(WorkManager for remote reconciliation when online)*
```

```
3. Offline session recovery
   └─► `SyncUiStateWorker` detects connectivity restored
       ├─► Flush queued actions from Room (UIStateSnapshot + LayoutSnapshot)
       └─► Retry `/user/profile` via `UserProfileRemoteDataSource`
           └─► Merge remote changes → `UserProfileRepository`
               └─► Emits refreshed Flow → ViewModels update Compose UI
```

### UI/UX Caching & Privacy Guardrails

- **Room** stores `UserProfileEntity`, `LayoutSnapshotEntity`, and `UIStateSnapshotEntity` with encrypted pinned tools and tooltip dismissals.
- **DataStore** keeps lightweight preferences: theme, density, and dismissed tips (legacy onboarding flag retained for analytics gating only). Writes occur on background dispatcher.
- **WorkManager** batches sync to avoid exposing UI metadata when the user has opted out of telemetry.
- **Privacy Hooks**: `UserProfileRepository` redacts display names and pinned tool identifiers before telemetry, and `UiPreferencesStore` enforces consent gates prior to sharing personalization metadata.

## Dependency Injection (Hilt)

```
┌────────────────────────────────────────┐
│         @HiltAndroidApp                │
│         NanoAIApplication              │
└────────────────────────────────────────┘
                  │
    ┌─────────────┴─────────────┐
    │                           │
    ▼                           ▼
┌─────────────────┐   ┌─────────────────┐
│ DatabaseModule  │   │ NetworkModule   │
│ @InstallIn      │   │ @InstallIn      │
│ SingletonComp.  │   │ SingletonComp.  │
├─────────────────┤   ├─────────────────┤
│ @Provides       │   │ @Provides       │
│ - NanoAIDatabase│   │ - Retrofit      │
│ - All DAOs      │   │ - OkHttpClient  │
│ - TypeConverters│   │ - CloudGateway  │
└─────────────────┘   └─────────────────┘
    │                           │
    └─────────────┬─────────────┘
                  │
                  ▼
        ┌─────────────────────┐
        │  RepositoryModule   │
        │  @InstallIn         │
        │  SingletonComp.     │
        ├─────────────────────┤
        │  @Binds             │
        │  - All Repositories │
        └─────────────────────┘
                  │
                  ▼
        ┌─────────────────────┐
        │    Use Cases        │
        │    @Inject          │
        │    constructor      │
        └─────────────────────┘
                  │
                  ▼
        ┌─────────────────────┐
        │    ViewModels       │
        │    @HiltViewModel   │
        │    @Inject          │
        │    constructor      │
        └─────────────────────┘
                  │
                  ▼
        ┌─────────────────────┐
        │    Composables      │
        │    hiltViewModel()  │
        └─────────────────────┘
```

## Database Schema (Room)

```
┌──────────────────────────────────────────────────────────────┐
│                    NanoAIDatabase v1                         │
└──────────────────────────────────────────────────────────────┘
    │
    ├─► ChatThread
    │   ├─ threadId: String (PK, UUID)
    │   ├─ title: String?
    │   ├─ personaId: String? (FK → PersonaProfile)
    │   ├─ activeModelId: String
    │   ├─ createdAt: Instant
    │   ├─ updatedAt: Instant
    │   └─ isArchived: Boolean
    │
    ├─► Message
    │   ├─ messageId: String (PK, UUID)
    │   ├─ threadId: String (FK → ChatThread, CASCADE)
    │   ├─ role: Role (USER/ASSISTANT/SYSTEM)
    │   ├─ text: String?
    │   ├─ audioUri: String?
    │   ├─ imageUri: String?
    │   ├─ source: MessageSource (LOCAL_MODEL/CLOUD_API)
    │   ├─ latencyMs: Long?
    │   ├─ createdAt: Instant
    │   ├─ errorCode: String?
    │   └─ INDEX(threadId, createdAt)
    │
    ├─► PersonaProfile
    │   ├─ personaId: String (PK, UUID)
    │   ├─ name: String
    │   ├─ description: String
    │   ├─ systemPrompt: String
    │   ├─ defaultModelPreference: String?
    │   ├─ temperature: Float
    │   ├─ topP: Float
    │   ├─ defaultVoice: String?
    │   ├─ defaultImageStyle: String?
    │   ├─ createdAt: Instant
    │   └─ updatedAt: Instant
    │
    ├─► PersonaSwitchLog
    │   ├─ logId: String (PK, UUID)
    │   ├─ threadId: String (FK → ChatThread, CASCADE)
    │   ├─ previousPersonaId: String?
    │   ├─ newPersonaId: String
    │   ├─ actionTaken: PersonaSwitchAction (CONTINUE/START_NEW)
    │   └─ createdAt: Instant
    │
    ├─► ModelPackage
    │   ├─ modelId: String
    │   ├─ displayName: String
    │   ├─ version: String
    │   ├─ providerType: ProviderType
    │   ├─ deliveryType: DeliveryType
    │   ├─ minAppVersion: Int
    │   ├─ sizeBytes: Long
    │   ├─ capabilities: Set<String>
    │   ├─ installState: InstallState
    │   ├─ downloadTaskId: UUID?
    │   ├─ manifestUrl: String
    │   ├─ checksumSha256: String?
    │   ├─ signature: String?
    │   ├─ createdAt: Instant
    │   └─ updatedAt: Instant
    │
    ├─► DownloadTask
    │   ├─ taskId: UUID
    │   ├─ modelId: String
    │   ├─ progress: Float
    │   ├─ status: DownloadStatus
    │   ├─ bytesDownloaded: Long
    │   ├─ startedAt: Instant?
    │   ├─ finishedAt: Instant?
    │   └─ errorMessage: String?
    │
    └─► ApiProviderConfig
        ├─ providerId: String (PK)
        ├─ providerName: String
        ├─ baseUrl: String
        ├─ apiKey: String
        ├─ apiType: APIType
        ├─ isEnabled: Boolean
        ├─ quotaResetAt: Instant?
        └─ lastStatus: ProviderStatus
```

## State Management (Reactive Flows)

```
ViewModel Layer:
  ┌──────────────────────────────────────────┐
  │  StateFlow<T>        (Hot, Stateful)     │
  │  - UI state representation               │
  │  - Always has current value              │
  │  - Survives config changes               │
  │  - Example: messages, isLoading          │
  └──────────────────────────────────────────┘
           ▲
           │ .stateIn(viewModelScope)
           │
  ┌──────────────────────────────────────────┐
  │  Flow<T>             (Cold, Stateless)   │
  │  - Repository/DAO emissions              │
  │  - Lazy evaluation                       │
  │  - Example: getAllThreadsFlow()          │
  └──────────────────────────────────────────┘

  ┌──────────────────────────────────────────┐
  │  SharedFlow<T>       (Hot, Events)       │
  │  - One-time events                       │
  │  - No initial value                      │
  │  - Example: errorEvents, navigation      │
  └──────────────────────────────────────────┘

UI Layer:
  ┌──────────────────────────────────────────┐
  │  .collectAsState()                       │
  │  - Converts Flow to Compose State        │
  │  - Triggers recomposition                │
  │  - Lifecycle-aware collection            │
  └──────────────────────────────────────────┘
           │
           ▼
  ┌──────────────────────────────────────────┐
  │  @Composable UI                          │
  │  - Renders current state                 │
  │  - Calls ViewModel methods on events     │
  └──────────────────────────────────────────┘
```

### Accessibility Semantics

- **Progress Center Panel** exposes `stateDescription` and `RangeInfo` so TalkBack announces queue position, percent complete, and retry status for each job.
- **Home Hub Sections** mark headings with semantic roles and provide concise hints describing active quick actions, chip selections, and recent activity timestamps.
- **Connectivity Banner** announces offline/online transitions with status copy that references queued uploads or sync work to keep screen reader users oriented.
- **Sidebar Navigation** designates drawers and panels as landmarks, ensuring focus order is predictable when opening the model selector or settings stacks.

## Thread Safety

All data operations are thread-safe:

- **Room**: All DAO operations are main-safe (use coroutines internally)
- **DataStore**: All reads/writes are main-safe (backed by Dispatchers.IO)
- **ViewModels**: Use `viewModelScope` for coroutine lifecycle management
- **Repositories**: Expose Flow/suspend functions only
- **WorkManager**: Executes on background threads automatically

## Testing Strategy

```
┌──────────────────────────────────────────────────────-------───┐
│  Unit Tests (JVM)           126+ tests                         │
├──────────────────────────────────────────────────────-------───┤
│  - Contract validation (OpenAPI schemas)                       │
│  - DAO tests (in-memory Room)                                  │
│  - Use case tests (with fakes)                                 │
│  - ViewModel tests (with TestDispatcher)                       │
│  - Baseline profile smoke (`:app:testBaselineProfileUnitTest`) │
└────────────────────────────────────────────────────-------─────┘


## Performance Monitoring

- **MainActivity** wires `JankStats` so every frame hitch over 32 ms is logged under the `NanoAI-Jank` tag with surface name and duration to aid regression triage.
- **NavigationScaffold** publishes a `PerformanceMetricsState` containing the active shell mode and queued job counts, enabling Compose to surface lightweight perf overlays when developer options are enabled.
- **Baseline Profiles** live in `app/src/main/baseline-prof.txt` and include hot startup and navigation routes (Home Hub, model library, progress drawer) to keep launch times consistent across releases.
┌─────────────────────────────────────────────────────────┐
│  Instrumented Tests (Device)                            │
├─────────────────────────────────────────────────────────┤
│  - Compose UI tests (semantics + interactions)          │
│  - Full user flow validation                            │
│  - Real Room database operations                        │
│  - WorkManager integration tests                        │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  Benchmark Tests (Macrobenchmark)                       │
├─────────────────────────────────────────────────────────┤
│  - Cold start measurement (<1.5s target)                │
│  - Scroll jank detection                                │
│  - Baseline profile generation                          │
└─────────────────────────────────────────────────────────┘
```

---

**Legend**:
- `└─►` Data flow
- `├─►` Alternative path
- `│` Dependency relationship
- `(PK)` Primary Key
- `(FK)` Foreign Key
- `CASCADE` Delete cascade
