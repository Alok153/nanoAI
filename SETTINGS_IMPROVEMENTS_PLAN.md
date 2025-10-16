# Settings Improvements & Launch Performance Plan

**Created**: 2025-10-16  
**Status**: Planning Phase

## Overview

Comprehensive plan to enhance settings organization, add AMOLED theme support, implement startup screen selection, and optimize app launch performance.

---

## 1. AMOLED Theme Support

### Current State
- Theme options: Light, Dark, System
- Uses Material 3 `darkColorScheme()` for dark mode
- Dynamic color support on Android 12+

### Proposed Changes

#### 1.1 Add AMOLED Theme Enum
**File**: `core/domain/model/uiux/UiUxEnums.kt`

```kotlin
enum class ThemePreference {
  LIGHT,
  DARK,
  AMOLED,  // TODO: Add AMOLED/pitch black theme
  SYSTEM,
}
```

#### 1.2 Create AMOLED Color Scheme
**File**: `ui/theme/Color.kt`

```kotlin
// AMOLED colors - pure black backgrounds for power savings
private val AmoledBackground = Color(0xFF000000)  // Pure black
private val AmoledSurface = Color(0xFF000000)
private val AmoledSurfaceVariant = Color(0xFF0A0A0A)  // Slight elevation
```

**File**: `ui/theme/Theme.kt`

```kotlin
private val AmoledColorScheme = darkColorScheme(
  background = Color(0xFF000000),  // Pure black
  surface = Color(0xFF000000),
  surfaceVariant = Color(0xFF0A0A0A),
  // ... other colors same as DarkColorScheme
)

// Update rememberNanoAIColorScheme
@Composable
private fun rememberNanoAIColorScheme(
  themePreference: ThemePreference,
  systemDarkTheme: Boolean,
  dynamicColor: Boolean,
): ColorScheme {
  val darkTheme = resolveDarkTheme(themePreference, systemDarkTheme)
  val isAmoled = themePreference == ThemePreference.AMOLED
  
  return when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isAmoled -> {
      // Dynamic colors (skip for AMOLED to maintain pure black)
    }
    isAmoled -> AmoledColorScheme
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }
}
```

#### 1.3 Update Theme Selector UI
**File**: `feature/uiux/ui/components/ThemeDensitySelectors.kt`

```kotlin
private fun themeLabel(theme: ThemePreference): String =
  when (theme) {
    ThemePreference.LIGHT -> "Light"
    ThemePreference.DARK -> "Dark"
    ThemePreference.AMOLED -> "AMOLED"
    ThemePreference.SYSTEM -> "System"
  }
```

#### 1.4 Update Theme Section Description
**File**: `feature/settings/ui/AppearanceSettingsSections.kt`

```kotlin
Text(
  text = "Switch between light, dark, AMOLED (pitch black), or follow the system theme.",
  style = MaterialTheme.typography.bodySmall,
  color = MaterialTheme.colorScheme.onSurfaceVariant,
)
```

### Benefits
- **Power savings** on OLED/AMOLED displays (pure black pixels are off)
- **Reduced eye strain** in complete darkness
- **User choice** for preferred dark mode intensity

---

## 2. Startup Screen Selection

### Current State
- App always opens to HOME mode
- No user preference for startup behavior
- Session restoration not implemented

### Proposed Changes

#### 2.1 Add Startup Preference to DataStore
**File**: `core/data/preferences/UiUxPreferences.kt`

```kotlin
// Add to preferences
data class StartupPreferences(
  val startupScreen: ScreenType = ScreenType.HOME,
  val restoreLastSession: Boolean = false,
)
```

#### 2.2 Create Startup Screen Selector UI
**File**: `feature/settings/ui/BehaviorSettingsSections.kt` (NEW)

```kotlin
@Composable
internal fun StartupScreenSection(
  selectedScreen: ScreenType,
  onScreenSelect: (ScreenType) -> Unit,
  modifier: Modifier = Modifier,
) {
  SettingsSection(title = "Startup & Home", modifier = modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(
        text = "Choose which screen nanoAI opens to on launch.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      
      // Radio buttons or chips for screen selection
      val screens = listOf(
        ScreenType.HOME to "Home",
        ScreenType.CHAT to "Last Chat",  // TODO: Add CHAT enum
        ScreenType.LIBRARY to "Model Library",  // TODO: Add LIBRARY enum
      )
      
      screens.forEach { (screen, label) ->
        FilterChip(
          selected = selectedScreen == screen,
          onClick = { onScreenSelect(screen) },
          label = { Text(label) },
        )
      }
    }
  }
}
```

#### 2.3 Update Shell Navigation Logic
**File**: `feature/uiux/presentation/ShellViewModel.kt`

```kotlin
// On app launch, read startup preference and navigate accordingly
init {
  viewModelScope.launch {
    val startupScreen = preferencesRepository.getStartupScreen()
    when (startupScreen) {
      ScreenType.HOME -> openMode(ModeId.HOME)
      ScreenType.CHAT -> openMode(ModeId.CHAT)
      // ... handle other screens
      else -> openMode(ModeId.HOME)
    }
  }
}
```

### Benefits
- **User control** over app entry point
- **Faster access** to frequently used features
- **Productivity boost** for power users

---

## 3. Launch Performance Optimization

### Problem Analysis

**Symptoms:**
- Jittery/stuttering app launch
- Items render after splash screen
- Not smooth/prerendered homepage

**Likely Causes:**
1. **Hydration delay** (`appUiState.isHydrating`)
2. **Heavy initialization** in MainActivity onCreate
3. **Missing baseline profile** optimization
4. **Synchronous DataStore reads** blocking UI
5. **Shell/AppViewModel** doing too much work on init

### Proposed Solutions

#### 3.1 Optimize Hydration Flow

**File**: `feature/uiux/presentation/AppViewModel.kt`

```kotlin
// OPTIMIZE: Move hydration to background, show skeleton UI immediately
init {
  viewModelScope.launch(Dispatchers.IO) {
    // Load preferences in background
    val preferences = async { loadPreferences() }
    val theme = async { loadTheme() }
    
    // Switch to Main thread for UI update
    withContext(Dispatchers.Main) {
      _uiState.update {
        it.copy(
          isHydrating = false,
          themePreference = theme.await(),
          // ... other preferences
        )
      }
    }
  }
}
```

#### 3.2 Add Skeleton/Splash Content

**File**: `MainActivity.kt`

```kotlin
// OPTIMIZE: Show skeleton instead of loading spinner during hydration
if (appUiState.isHydrating) {
  // Instead of loading spinner, show skeleton of homepage
  HomeScreenSkeleton()  // Placeholder UI that matches real homepage layout
} else {
  NavigationScaffold(...)
}
```

#### 3.3 Lazy Initialization

**File**: `feature/uiux/presentation/ShellViewModel.kt`

```kotlin
// OPTIMIZE: Defer non-critical initialization
init {
  // Critical path only - load active mode
  viewModelScope.launch {
    val startupMode = getStartupMode()
    openMode(startupMode)
  }
  
  // OPTIMIZE: Defer model catalog loading
  viewModelScope.launch {
    delay(500)  // Wait for first frame
    loadModelCatalog()
  }
}
```

#### 3.4 Update Baseline Profile

**File**: `app/src/main/baseline-prof.txt`

```
# OPTIMIZE: Add startup critical paths to baseline profile
HSPLcom/vjaykrsna/nanoai/MainActivity;->onCreate(Landroid/os/Bundle;)V
HSPLcom/vjaykrsna/nanoai/feature/uiux/presentation/AppViewModel;-><init>()V
HSPLcom/vjaykrsna/nanoai/feature/uiux/presentation/ShellViewModel;-><init>()V
HSPLcom/vjaykrsna/nanoai/ui/navigation/NavigationScaffold;->NavigationScaffold()V
# ... add more hot paths
```

Run baseline profile generation:
```bash
./gradlew :macrobenchmark:pixel6Api31BenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=BaselineProfile
```

#### 3.5 Use Startup Tracing

**File**: `MainActivity.kt`

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
  // NOTE: Trace startup for performance profiling
  Trace.beginSection("MainActivity.onCreate")
  super.onCreate(savedInstanceState)
  
  Trace.beginSection("enableEdgeToEdge")
  enableEdgeToEdge()
  Trace.endSection()
  
  Trace.beginSection("setupBackPressedCallback")
  setupBackPressedCallback()
  Trace.endSection()
  
  Trace.beginSection("setContent")
  setContent { ... }
  Trace.endSection()
  
  Trace.endSection()
}
```

### Performance Targets
- **Cold start**: <1.5s to first frame
- **Warm start**: <500ms
- **Hot start**: <200ms
- **Time to interactive**: <2s
- **Jank**: <5% frame drops

---

## 4. Implementation Order

### Phase 1: Theme Improvements (Quick Wins)
1. ✅ Remove Accent Colors placeholder
2. ⬜ Add AMOLED theme enum
3. ⬜ Create AMOLED color scheme
4. ⬜ Update theme selector UI
5. ⬜ Update theme description text
6. ⬜ Test theme switching behavior

### Phase 2: Startup Screen Selection
1. ⬜ Add startup preferences to DataStore
2. ⬜ Create startup screen selector UI
3. ⬜ Wire up preference save/load
4. ⬜ Update Shell navigation logic
5. ⬜ Test startup behavior with different selections

### Phase 3: Launch Performance (Complex)
1. ⬜ Add startup tracing
2. ⬜ Profile current launch performance
3. ⬜ Optimize hydration flow
4. ⬜ Add skeleton UI during load
5. ⬜ Defer non-critical initialization
6. ⬜ Generate baseline profile
7. ⬜ Measure improvements
8. ⬜ Iterate on bottlenecks

---

## 5. Testing Requirements

### Unit Tests
- `ThemePreferenceTest` - AMOLED theme resolution
- `StartupPreferencesTest` - Screen selection persistence
- `ShellViewModelTest` - Startup navigation logic

### Instrumentation Tests
- `ThemeToggleTest` - AMOLED theme visual verification
- `StartupScreenTest` - Launch behavior validation
- `LaunchPerformanceTest` - Cold/warm/hot start metrics

### Manual Testing
- AMOLED theme on physical OLED device (power consumption)
- Startup screen selection with app restart
- Launch performance on low-end devices (API 26-28)

---

## 6. Risks & Mitigation

### Risk: AMOLED theme breaks dynamic colors
**Mitigation**: Disable dynamic colors when AMOLED selected

### Risk: Startup screen selection causes crashes
**Mitigation**: Fallback to HOME on any navigation errors

### Risk: Performance optimization introduces regressions
**Mitigation**: Comprehensive macrobenchmark suite before/after

---

## 7. Success Criteria

- [ ] AMOLED theme shows pure black backgrounds
- [ ] Theme switches smoothly without flicker
- [ ] Startup screen preference persists across app restarts
- [ ] App launches to selected screen correctly
- [ ] Cold start <1.5s on Pixel 6 (baseline)
- [ ] No jank frames during launch sequence
- [ ] All tests pass with >75% coverage

---

## Notes

- **Priority**: Phase 1 (theme) is easiest and highest user value
- **Complexity**: Phase 3 (performance) needs careful profiling and iteration
- **Dependencies**: None - all phases can proceed independently
- **Timeline**: Phase 1 (1 day), Phase 2 (2 days), Phase 3 (3-5 days)
