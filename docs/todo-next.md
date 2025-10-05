## nanoAI Next Priorities

_Last updated: 2025-10-05_

## High Priority (from Codebase Audit)

### Enhance Test Coverage
- **Write ViewModel Tests:** Add unit tests for all ViewModels to verify state management logic.
- **Write UI Tests:** Implement UI tests using the Jetpack Compose testing framework to validate UI behavior and prevent regressions.
- **Write Data Layer Tests:** Add instrumented tests for Room DAOs and unit tests for Repositories to ensure data integrity.
- **Implement Code Coverage:** Integrate a code coverage tool like JaCoCo to measure test coverage and identify untested code paths.

### Improve Code Quality
- **Remove Redundant Dependency:** Delete the duplicate `androidx.security.crypto` entry from `app/build.gradle.kts` to keep the dependency graph clean.
- **Refactor Complex Composables:** Break down the `ChatScreen` and `MessageInputArea` composables into smaller, more focused functions to reduce their complexity.
- **Refactor Large Classes:** Break down large classes like `ConversationRepositoryImpl` into smaller, more specialized repositories if possible.
- **Fix detekt Issues:** Refactor the `NanoAITheme` composable to resolve the `LongMethod` issue and ensure the `detekt` check passes.
- **Centralize Constants:** Move hardcoded constants to a central object or `build.gradle.kts` to improve configurability and avoid magic strings/numbers.
- **Improve Error Handling:** Implement a more structured error handling mechanism in the repositories to provide clearer and more consistent error signals to the domain layer.

### Strengthen UI/UX
- **Externalize Strings:** Move all user-facing strings to `res/values/strings.xml` and reference them using `stringResource()`.
- **Improve Accessibility:** Add meaningful `contentDescription` attributes to all interactive UI elements, especially `Icon`s and `IconButton`s.
- **Optimize `LazyColumn`:** Ensure that the data classes used in the `LazyColumn` are stable to prevent unnecessary recompositions.

### Automate CI/CD
- **Implement CI/CD:** Set up a CI/CD pipeline using a platform like GitHub Actions or GitLab CI.
- **Automate Checks:** The pipeline should automatically run `detekt`, `spotlessCheck`, and all unit and instrumented tests on every commit or pull request.
- **Automate Builds:** Configure the pipeline to automatically build and sign release artifacts.

### Refine Documentation
- **Update README.md:** Correct the `ktlintCheck` command to `spotlessCheck` to avoid confusion.
- **Add KDoc:** Add KDoc comments to all public classes, functions, and complex private functions to improve code comprehension and maintainability.

## Medium-Term Enhancements
- **Error Envelope Unification:** Align all telemetry publishers with the new `TelemetryReporter` contract and add dashboards for `RecoverableError` trends.
- **Offline Sync Roadmap:** Flesh out conflict resolution for queued actions, including retry jitter and WorkManager constraints.
- **Deep Link Coverage:** Expand `NavigationScaffold` to support deep links into persona and history routes and add regression tests.

## Artifacts & Ownership
- Owner: UI/UX Platform Working Group
- Weekly checkpoint: Review outstanding inconsistencies and close resolved items in `docs/inconsistencies.md`.
- Reporting: Summaries posted in `docs/changelog.md` (to be created) once major milestones are delivered.
