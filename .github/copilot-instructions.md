`# nanoAI Development Guidelines

Auto-generated from all feature plans. Last updated: 2025-10-02

## Active Technologies
- **Kotlin 1.9.x (JDK 11 baseline)**: Primary language for Android development.
- **Jetpack Compose Material 3**: UI toolkit for modern Android interfaces.
- **Hilt**: Dependency injection framework.
- **WorkManager**: For scheduling background tasks.
- **Room**: SQLite database abstraction.
- **DataStore**: Preferences storage solution.
- **Retrofit + Kotlin Serialization**: For REST API calls with JSON handling.
- **OkHttp**: HTTP client.
- **MediaPipe Generative (LiteRT)**: AI/ML inference on-device.
- **Coil**: Image loading library.
- **Kotlin Coroutines**: Asynchronous programming (foundation spec: 001-foundation).

See `gradle/libs.versions.toml` for version details and updates.

## Project Structure
```
nanoAI/
├── build.gradle.kts
├── README.md
├── settings.gradle.kts
├── docs/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/vjaykrsna/nanoai/
│       │   └── res/
│       ├── androidTest/
│       └── test/
├── build/
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── macrobenchmark/
│   ├── build.gradle.kts
│   └── src/
├── config/
│   └── detekt/
├── scripts/
├── specs/
│   ├── 001-foundation/
│   ├── 002-disclaimer-and-fixes/
│   └── 003-UI-UX/
└── Personal/
```

## Commands
```
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew connectedAndroidTest   # Run instrumented tests on connected device
./gradlew test                   # Run unit tests
./gradlew clean                  # Clean build outputs
./gradlew ktlintCheck            # Check code formatting with ktlint
./gradlew detekt                 # Run static analysis with Detekt
```

## Testing
- Unit tests: Located in `app/src/test/`, run with `./gradlew test`.
- Instrumented tests: Located in `app/src/androidTest/`, run with `./gradlew connectedAndroidTest`.
- Macrobenchmarks: Located in `macrobenchmark/`, for performance testing.

## Code Style
- Kotlin 1.9.x (JDK 11 baseline): Follow the [official Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Use ktlint for code formatting and linting.
- Use Detekt for static code analysis; see `config/detekt/detekt.yml` for rules.

## Contributing
- Branching: Use feature branches (e.g., `003-UI-UX` for UI/UX work). Reference specs in `specs/` for new features.
- Pull Requests: Ensure tests pass and code style checks before submitting.
- Workflow: Follow the foundation spec in `specs/001-foundation/` for core guidelines.

## Build and Deployment
- Build files: `build.gradle.kts` (root and app module).
- ProGuard rules: `app/proguard-rules.pro` for obfuscation.
- Deployment: Use `./gradlew assembleRelease` for production APKs.

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
