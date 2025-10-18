# Quickstart Validation: Offline Multimodal nanoAI Assistant

## Device & Build Setup
1. Use reference device: Pixel 7 (Android 14, 8 GB RAM) and budget device: Pixel 4a (Android 13, 6 GB RAM).
2. Install latest build from `app/build/outputs/apk/debug/app-debug.apk`.
3. Clear app data before each run to validate onboarding.

## Smoke Test Flow (Foundation + Consolidated Features)
1. Launch app → confirm first-launch disclaimer dialog appears with responsibility notice and acknowledge button.
2. From sidebar (left drawer), navigate to **Model Library** → download "Nano Phi-2 Lite" (MediaPipe bundle).
   - Verify progress bar, pause/resume controls, and completion state.
3. Return to chat view → ensure downloaded model is preselected; send "Summarize the benefits of offline AI." → response within 2 s median.
4. Toggle persona to "Creative Muse" → when prompted, choose **Continue in current thread**. Confirm persona switch log entry.
5. Use sidebar quick toggles to switch between local/cloud inference modes and clear conversation context.
6. Test command palette (Ctrl+K / Cmd+K) → search for "New Chat" and verify navigation works.
7. Open **Settings → Export & Backup** → run universal export and confirm warning about unencrypted JSON archive.
8. Navigate to **Privacy Dashboard** → confirm consent timestamp, disclaimer acknowledgment count, and telemetry toggle (off by default).
9. Test Material 3 theming → toggle between light/dark modes and verify instant theme switching.
10. Verify home hub grid layout → confirm mode cards (Chat, Image, Audio, Code, Translate) with proper icons and accessibility labels.

## Offline Readiness
1. Enable airplane mode with local model installed.
2. Send audio prompt stub (future scope) via text entry referencing audio → ensures graceful message noting audio support pending.
3. Submit text prompt → confirm offline banner and successful local response.

## Failure Handling
1. In model library, queue two downloads with max concurrency = 1 → second shows queued.
2. Force-stop network → resume download → verify failure message and retry option.
3. Delete active model from settings → confirm running session stops and user notified.

## Performance Checks
- Capture frame timeline using `adb shell dumpsys gfxinfo` while scrolling chat list; ensure dropped frames <5%.
- Run macrobenchmark suite `:macrobenchmark:coldStart` and assert cold start <1.5 s.

## Regression Hooks
- Run unit tests: `./gradlew testDebugUnitTest`.
- Run instrumentation tests: `./gradlew connectedDebugAndroidTest` (requires emulator with API 33+).
- Validate baseline profile generation: `./gradlew :app:generateBaselineProfile`.
