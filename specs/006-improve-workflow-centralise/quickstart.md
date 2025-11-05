# Quickstart: Centralised Build Workflow

1. **Bootstrap convention plugins**
   - Run `./gradlew :build-logic:publishToMavenLocal` after the new `build-logic` composite build lands.
   - In each module, replace existing plugin blocks with the appropriate convention plugin (e.g., `plugins { id("com.vjaykrsna.nanoai.android.feature") }`).

2. **Install quality gate hooks**
   - Execute `./scripts/hooks/install-hooks.sh` to symlink the `pre-commit` script.
   - Verify local hooks by running `git commit --allow-empty -m "hook check"` and ensure Detekt/Spotless/coverage tasks execute.

3. **Run static analysis**
   - Execute `./gradlew detekt detektMain detektTest` to validate custom rule wiring.
   - For type resolution debugging, inspect generated reports under `build/reports/detekt`.

4. **Validate coverage thresholds**
   - Run `./gradlew testDebugUnitTest verifyCoverageThresholds`.
   - Open `build/reports/coverage/summary.html` to confirm UI/ViewModel/Data percentages meet thresholds.

5. **Capture build metrics**
   - Trigger a build scan with `./gradlew assembleDebug --scan` and share the URL in the PR description.
   - Ensure custom tags include branch name, CI job URL, and slow tasks (<500ms queue flush target).

6. **Adopt new testing utilities**
   - For Flow tests, import `com.vjaykrsna.nanoai.shared.testing.flowTest` helper to wrap Turbine usage.
   - For screenshot tests, add Roborazzi dependencies and run `./gradlew roboScreenshotDebug` to generate baselines stored under `app/src/test/screenshots`.

7. **Update documentation**
   - When touching build or test configuration, update `docs/development/BUILD_WORKFLOW.md` and `QUALITY_GATES.md` with rationale and command references.
