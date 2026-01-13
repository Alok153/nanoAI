# Quickstart Validation: Offline Multimodal nanoAI Assistant

## Setup
1. Use a reference device (Pixel 7) and budget device (Pixel 4a).
2. Install the latest debug APK from `app/build/outputs/apk/debug/` and clear app data.
3. Ensure at least one local model is downloaded once so offline flows can be exercised.

## Core Online Flow
1. Launch → acknowledge the first-launch disclaimer; Home Hub shows quick actions and no progress jobs.
2. Open Model Library → start a local download → verify progress, pause/resume, and completion.
3. Navigate to Chat → select the downloaded model when prompted → send a prompt and confirm a response.
4. Open the persona switcher → switch personas → thread context updates without losing history.
5. Go to Settings → trigger export → archive is created and the encryption warning is shown.

## Offline & Recovery
1. Enable airplane mode with a downloaded model available.
2. Chat shows the offline banner and local inference indicator; send a prompt without crashes.
3. Start a model download while offline → job remains queued; reconnect → download resumes.
4. Delete an in-use model → inference stops and a warning/toast surfaces; composer stays responsive.

## Failure Handling
1. Kick off two model downloads (concurrency = 1) → second request queues until the first finishes.
2. Drop connectivity mid-download → failure surfaces with retry; resuming keeps prior progress.
3. Persona switch failure surfaces a snackbar and does not change the active thread/persona.

## Performance, Coverage & Visual Regression
- Refresh screenshot baselines for shell and chat offline states via `./gradlew :app:roboScreenshotDebug` (artifacts under `app/src/test/screenshots`).
- Verify cold start + primary navigation budgets with `./gradlew :macrobenchmark:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.vjaykrsna.nanoai.macrobenchmark.StartupBenchmark`.
- Run `./gradlew spotlessCheck detekt testDebugUnitTest verifyCoverageThresholds`.

## Acceptance Traceability
- Shell navigation, banners, and visuals: [ShellNavigationTest](app/src/androidTest/java/com/vjaykrsna/nanoai/feature/uiux/ShellNavigationTest.kt) and [ShellScreenshotsTest](app/src/test/java/com/vjaykrsna/nanoai/feature/uiux/presentation/ShellScreenshotsTest.kt).
- Chat offline readiness: [ChatScreenTest](app/src/androidTest/java/com/vjaykrsna/nanoai/feature/chat/ui/ChatScreenTest.kt) and [ChatScreenScreenshotTest](app/src/test/java/com/vjaykrsna/nanoai/feature/chat/ui/ChatScreenScreenshotTest.kt).
- Model library flows: [ModelLibraryScreenStructureTest](app/src/androidTest/java/com/vjaykrsna/nanoai/feature/library/ui/ModelLibraryScreenStructureTest.kt) and [ModelLibraryViewModelTest](app/src/test/java/com/vjaykrsna/nanoai/feature/library/presentation/ModelLibraryViewModelTest.kt).
- Startup + navigation budgets: [StartupBenchmark.kt](macrobenchmark/src/main/java/com/vjaykrsna/nanoai/macrobenchmark/StartupBenchmark.kt) alongside [NavigationBenchmarks](macrobenchmark/src/main/java/com/vjaykrsna/nanoai/macrobenchmark/NavigationBenchmarks.kt).
