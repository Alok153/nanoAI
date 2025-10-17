`# nanoAI Development Guidelines

Auto-generated from all feature plans. Last updated: 2025-10-02

## Active Technologies (main)
- Kotlin 1.9.x (JDK 17 baseline), Jetpack Compose Material 3, Hilt, WorkManager, Room (SQLite database), DataStore (preferences), Retrofit + Kotlin Serialization, OkHttp, MediaPipe Generative (LiteRT), Coil, Kotlin Coroutines, Junit5

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
./gradlew test                   # Run unit tests
./gradlew spotlessApply          # Check code formatting with ktlint
./gradlew detekt                 # Run static analysis with Detekt
```

<!-- MANUAL ADDITIONS START -->

- When implementing new features or working with unfamiliar technologies (e.g., libraries or AI runtimes), use the Context7 MCP tool to retrieve up-to-date documentation from official sources.
- Don't use something that's deprecated and prefer to cleanup if something is found.
- Avoid keeping deprecated files/code in the codebase don't maintain legacy support encourage migration and clean what's not required.
- Use standard comment markers (TODO, FIXME, HACK, NOTE, OPTIMIZE) to keep things planned and avoid missing future tasks.

<!-- MANUAL ADDITIONS END -->
