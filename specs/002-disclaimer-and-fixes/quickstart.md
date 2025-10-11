# Quickstart: First-launch Disclaimer and Fixes

This quickstart guides developers and QA through verifying the feature locally.

Prerequisites
- Android emulator or device
- Debuggable build of the app from branch `002-disclaimer-and-fixes`

Verification Steps
1. Clean app data and install the debug APK.
2. Launch the app.
   - Expectation: A non-blocking disclaimer dialog appears explaining user responsibility for generated content with buttons `Acknowledge` and `Dismiss`.
3. Tap `Dismiss` and relaunch the app.
   - Expectation: Dialog reappears.
4. Tap `Acknowledge` and relaunch app.
   - Expectation: Dialog does not appear again; `PrivacyPreference.consentAcknowledgedAt` is set.
5. Test import:
   - Go to Settings → Data Management → Import.
   - Provide a sample backup JSON file (see `specs/002-disclaimer-and-fixes/contracts/sample-backup.json`).
   - Expectation: Personas and API providers restored; success snackbar shown.
6. Sidebar toggle:
   - Open sidebar and toggle Local/Cloud inference mode. Confirm visual state and that `InferenceOrchestrator` uses the selected mode on next request.
7. Run lint and unit tests locally to confirm fixes:
    - `./gradlew spotlessApply detekt` (project-configured tasks)
    - `./gradlew testDebugUnitTest` (expect compile success)

Notes
- If tests fail due to missing runtime model binaries, stub the model catalog or run with `INCLUDE_LOCAL_MODELS=false` in the test environment.
- For import testing, the sample JSON will contain minimal personas and providers to avoid side effects.
