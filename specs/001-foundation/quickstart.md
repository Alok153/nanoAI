# Quickstart Validation: Offline Multimodal nanoAI Assistant

## Setup
1. Use reference (e.g., Pixel 7) and budget (e.g., Pixel 4a) devices/emulators.
2. Install latest debug APK from `app/build/outputs/apk/debug/`.
3. Clear app data before each run.

## Core Flow
1. Launch app → verify first-launch disclaimer appears; acknowledge once.
2. Open Model Library → download a local model → see progress and completion.
3. Go to Chat → confirm downloaded model is selectable; send prompt → response is shown.
4. Switch persona from sidebar → confirm behavior matches spec (continue/split thread as configured).
5. Open Settings → run export → confirm archive created and warning shown.
6. Verify light/dark theme toggle and basic navigation (Home, Chat, Library, Settings).

## Offline Behavior
1. With at least one local model installed, enable airplane mode.
2. Open Chat → banner indicates offline.
3. Send prompt → response uses local model without crashes.

## Failure Handling
1. Start two model downloads with concurrency limit = 1 → second waits.
2. Disable network mid-download → verify graceful failure and retry.
3. Delete an in-use local model → active inference stops and user is notified.

## Performance & Quality
- Run `./gradlew testDebugUnitTest`.
- Run `./gradlew spotlessCheck detekt`.
- Run `./gradlew verifyCoverageThresholds`.

If these checks pass, the foundation implementation is considered healthy.
