# Quickstart Validation: Offline Multimodal nanoAI Assistant

## Device & Build Setup
1. Use reference device: Pixel 7 (Android 14, 8 GB RAM) and budget device: Pixel 4a (Android 13, 6 GB RAM).
2. Install latest build from `app/build/outputs/apk/debug/app-debug.apk`.
3. Clear app data before each run to validate onboarding.

## Smoke Test Flow
1. Launch app → confirm first-launch responsibility notice is shown and can be dismissed.
2. From sidebar, open **Model Library** → download "Nano Phi-2 Lite" (MediaPipe bundle).
   - Verify progress bar, pause/resume, and completion state.
3. Return to chat view → ensure downloaded model is preselected; send "Summarize the benefits of offline AI." → response within 2 s median.
4. Toggle persona to "Creative Muse" → when prompted, choose **Continue in current thread**. Confirm persona switch log entry appears in debug overlay.
5. Disable local model via quick toggle → select OpenAI configuration → send prompt → verify cloud badge and quota info chips.
6. Open **Settings → Export & Backup** → run universal export and confirm warning about unencrypted archive.
7. Navigate to **Privacy Dashboard** → confirm consent timestamp and telemetry toggle (off by default).

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
