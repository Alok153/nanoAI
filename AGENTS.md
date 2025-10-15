# nanoAI Agent Rules & Wake-Up Calls

## 🚨 Critical Rules for AI Agents

### Architecture Guardians
**NEVER** bypass clean architecture layers. Always route through:
- `UseCase` → `Repository` → `DataSource` (Domain → Data flow)
- `Composable` → `ViewModel` → `UseCase` (UI → Domain flow)
- **Wake-up Call**: Mixing layers creates untestable code and violates the 75/65/70% coverage requirements.

### Testing Imperative
**EVERY** code change requires tests. Targets are non-negotiable:
- ViewModel: ≥75% coverage
- UI: ≥65% coverage
- Data: ≥70% coverage
- **Wake-up Call**: Untested code ships bugs that break offline functionality and accessibility compliance.

### Kotlin-First Purity
**ONLY** use Kotlin. No Java interop unless absolutely necessary.
- Use coroutines, not threads
- Use sealed classes, not enums for states
- Use data classes for immutable models
- **Wake-up Call**: Java patterns slow development and miss Kotlin's null-safety advantages.

### Security First
**ALWAYS** encrypt sensitive data:
- API keys: Use `EncryptedSecretStore`
- User preferences: Respect DataStore encryption
- Exports: Warn about unencrypted data via `notifyUnencryptedExport()`
- **Wake-up Call**: Unencrypted storage risks user privacy - the core value proposition.

### Performance Budgets
**RESPECT** targets:
- Cold start: <1.5s
- Jank: <5% frame drops
- Queue flush: <500ms
- **Wake-up Call**: Poor performance kills user adoption on lower-end Android devices.

## 💀 Common Agent Mistakes to Avoid

### 1. Skipping Use Cases
❌ Direct repository calls from ViewModels
✅ Always create and inject UseCases for business logic
**Why?** UseCases enforce testability and separation of concerns.

### 2. Ignoring Offline Scenarios
❌ Assuming always-online behavior
✅ Test with `TestEnvironmentRule` for offline fallbacks
**Why?** Users expect offline functionality after model downloads.

### 3. Breaking Material 3
❌ Custom styling without Material tokens
✅ Use `MaterialTheme` and semantic colors
**Why?** Inconsistent UX frustrates users and fails accessibility audits.

### 4. Deprecated Dependencies
❌ Using old libraries like RxJava or legacy support
✅ Check `gradle/libs.versions.toml` for current versions
**Why?** Deprecated code bloats APK and introduces security risks.

### 5. Inefficient State Management
❌ MutableState in composables
✅ StateFlow in ViewModels, collectAsState in UI
**Why?** Wrong state management causes UI glitches and memory leaks.

### 6. Blocking UI Thread
❌ Network calls on main thread
✅ Always use coroutines with IO dispatcher
**Why?** ANR crashes destroy user trust.

### 7. Incomplete Error Handling
❌ Silent failures
✅ Proper `NanoAIResult` usage with error propagation
**Why?** Poor errors hide bugs and confuse users.

## ⚡ Quick Action Rules

### When Adding Features
1. Create failing tests first (TDD)
2. Add to risk register if coverage impact
3. Update architecture diagram if changing data flow
4. Test offline + accessibility scenarios

### When Refactoring
1. Run full test suite before/after
2. Update any affected docs in `/docs`
3. Check coverage doesn't drop below targets
4. Add migration tests for schema changes

### When Debugging
## 📁 Project Structure

```
nanoAI/
├── app/                           # Main application module
│   ├── src/main/java/com/vjaykrsna/nanoai/
│   │   ├── core/                  # Core utilities and base classes
│   │   ├── di/                    # Dependency injection modules
│   │   ├── feature/               # Feature modules (chat, uiux, etc.)
│   │   ├── model/                 # Data models
│   │   ├── security/              # Security-related utilities
│   │   ├── telemetry/             # Analytics and logging
│   │   └── ui/                    # UI components and navigation
│   ├── src/test/java/com/vjaykrsna/nanoai/  # Unit tests (JVM)
│   │   ├── core/
│   │   ├── coverage/
│   │   └── feature/
│   └── src/androidTest/java/com/vjaykrsna/nanoai/  # Instrumentation tests
│       ├── testing/               # Test utilities and fakes
│       └── coverage/              # Coverage-specific UI tests
├── docs/                          # Documentation
├── specs/                         # Feature specifications
├── config/                        # Configuration files (detekt, coverage)
├── scripts/                       # Build and utility scripts
├── macrobenchmark/                # Performance benchmarks
├── build.gradle.kts               # Root build configuration
├── settings.gradle.kts            # Project settings
└── gradle/libs.versions.toml       # Approved dependency versions
```
1. Check logs with `ShellTelemetry`
2. Isolate layers (UI, Domain, Data)
3. Use `TestEnvironmentRule` for controlled testing
4. Verify on multiple screen sizes/densities

## 🚦 Code Quality Gates

**Must Pass Before Commit:**
- `./gradlew spotlessCheck` (Kotlin formatting)
- `./gradlew detekt` (Static analysis)
- `./gradlew testDebugUnitTest` (Unit tests)
- `./gradlew verifyCoverageThresholds` (Coverage gates)

**Must Pass Before PR:**
- `./gradlew check` (All quality gates)
- Accessibility audit with TalkBack
- Offline functionality verification

## 📚 Essential References

- `docs/ARCHITECTURE.md` - System design and data flows
- `docs/testing.md` - Coverage requirements and test strategy
- `specs/` - Feature specifications with acceptance criteria
- `gradle/libs.versions.toml` - Approved dependency versions
- `config/coverage/layer-map.json` - Coverage classification rules

## 🎯 Agent Success Metrics

- Code passes all `./gradlew check` gates
- Coverage targets maintained or improved
## 🛠️ Development Tools & Resources

### When Stuck or Implementing New Features
**USE Context7 MCP** to fetch up-to-date documentation and code examples from official sources:
- For unfamiliar libraries or AI runtimes
- To verify API changes or deprecations
- **Wake-up Call**: Don't guess - always check official docs to avoid deprecated patterns.
- No new accessibility or offline issues
- Architecture diagram remains accurate
- Documentation updated for changes

**Remember**: This is a privacy-first app. Every decision impacts user trust. Test thoroughly, respect performance budgets, and maintain clean architecture.
