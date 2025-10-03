`# nanoAI Development Guidelines

Auto-generated from all feature plans. Last updated: 2025-10-02

## Active Technologies (main, 004-fixes-and-inconsistencies)
- Kotlin 1.9.x (JDK 11 baseline), Jetpack Compose Material 3, Hilt, WorkManager, Room (SQLite database), DataStore (preferences), Retrofit + Kotlin Serialization, OkHttp, MediaPipe Generative (LiteRT), Coil, Kotlin Coroutines

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

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
