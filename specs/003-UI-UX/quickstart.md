# Quickstart — UI/UX — Polished Product-Grade Experience

## Prerequisites
- Android Studio Ladybug (2025.2) with Arctic Fox compatible Compose tooling
- Java 11 (corretto) installed and configured
- Pixel 7 / emulator API 36 image with hardware keyboard enabled
- Node of at least 8 GB RAM for macrobenchmark runs

## Environment Setup
1. Sync Gradle:
   ```bash
   ./gradlew tasks
   ```
2. Install git hooks & formatting:
   ```bash
   ./gradlew spotlessApply
   ```
3. Launch emulator or connect device (API 36+).

## Running Tests
- Unit + ViewModel tests (includes new shell specs):
  ```bash
  ./gradlew test
  ```
- Baseline profile smoke (validates hot startup routes):
  ```bash
  ./gradlew :app:testBaselineProfileUnitTest
  ```
- Compose UI tests (command palette, offline banners):
  ```bash
  ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.vjaykrsna.nanoai.shell.ShellUiTestSuite
  ```
- Macrobenchmark smoke (home hub launch + mode switch):
  ```bash
  ./gradlew :macrobenchmark:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.vjaykrsna.nanoai.macrobenchmark.NavigationBenchmarks
  ```

## Manual Verification Checklist
- [ ] Launch app → confirm Home Hub shows grid of mode cards (2/3/4 columns depending on width) and quick actions row.
- [ ] Toggle left sidebar via menu icon + keyboard shortcut — drawer animates within 100 ms and respects accessibility focus.
- [ ] Invoke command palette (`Ctrl+K`) — overlay opens instantly, typing filters to "Image" + "New Chat" actions.
- [ ] Start image generation while offline — job queued in progress center, offline banner surfaces with CTA.
- [ ] Reconnect network — queued job resumes automatically, banner transitions to "Syncing" then disappears.
- [ ] Open right contextual drawer for Chat — displays model selector + progress center stack, closing respects `Esc`.
- [ ] Switch to Settings — layout reuses shell, sections match table in spec with consistent breadcrumbs.
- [ ] Verify theme toggle toggles instantly and persists after process death.
- [ ] With TalkBack enabled, focus a progress job — screen reader announces job name, status (queued/running/done), and percent complete.
- [ ] Navigate the Home Hub headings — accessibility focus lands on section titles and chips read out selection state and hints.
- [ ] Toggle connectivity banner states — TalkBack announces offline reason and queued action counts when status flips.

## Monitoring & Telemetry
- Jank logging is enabled by default; run `adb logcat NanoAI-Jank:D *:S` while interacting with the shell to capture frame hitches over 32 ms.
- Compose's performance overlay can be toggled via developer options — the active shell mode and queued jobs flow through `PerformanceMetricsState` to annotate hotspots.
- Baseline profile entries live at `app/src/main/baseline-prof.txt`; regenerate with the Macrobenchmark suite if navigation flows change significantly.

## Troubleshooting
- Palette fails to open: ensure hardware keyboard is connected; fallback to search bar icon in top app bar.
- Tests timing out on CI: run `adb shell settings put global window_animation_scale 0.0` before instrumentation to reduce variance.
- Macrobenchmark requires Profile installer: first run debug build to warm caches (`./gradlew installBenchmark`).
- Missing jank logs: confirm device/emulator is API 31+ and that your `adb logcat` filter includes the `NanoAI-Jank` tag.
