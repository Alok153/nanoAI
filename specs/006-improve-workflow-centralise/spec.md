# Feature Specification: Improve Workflow - Centralize Build Logic

**Feature Branch**: `006-improve-workflow-centralise`  
**Created**: November 5, 2025  
**Status**: Draft  
**Input**: User description: "improve workflow - We will centralise build logic, enhance static analysis, and testing strategy to simplify headache as a developer"

## Clarifications

### Session 2025-11-05

- Q: How should legacy code that doesn't meet quality standards be handled? → A: Remediate all existing code to meet new quality standards before enforcing gates
- Q: For urgent hotfixes, how can quality gates be bypassed? → A: Allow bypass with explicit approval or justification
- Q: How are quality gates enforced in CI/CD when developers skip local checks? → A: CI/CD runs all checks regardless of local execution

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Centralize Build Logic (Priority: P1)

As a developer, I want build configuration logic to be centralized so that I can maintain a single source of truth for all build configurations rather than duplicating build logic across multiple Gradle files. This reduces inconsistencies and makes updates faster.

**Why this priority**: Build consistency directly impacts all developers daily. Centralizing build logic is foundational to improving the entire developer experience and prevents build configuration drift across modules.

**Independent Test**: Can be fully tested by verifying that all module build.gradle.kts files reference centralized conventions and that building any module produces consistent, reproducible outputs without manual configuration duplication.

**Acceptance Scenarios**:

1. **Given** a developer needs to add a new dependency version, **When** they update `gradle/libs.versions.toml`, **Then** the change is automatically available across all modules without modifying individual build files
2. **Given** a new module is created, **When** it applies standard conventions via plugin references, **Then** it inherits all standard configurations (test setup, coverage, spotless, detekt) without manual setup
3. **Given** build conventions need to be updated, **When** a developer modifies the centralized convention plugin, **Then** all modules automatically use the updated configuration on next build without individual file changes

---

### User Story 2 - Enhance Static Analysis Tooling (Priority: P1)

As a developer, I want static analysis tools (Detekt, Spotless) to be comprehensively configured and easy to run so that I catch code quality issues early and spend less time on manual code reviews and formatting.

**Why this priority**: Static analysis failures block CI/CD pipelines today. Enhancing this capability prevents build failures during development and reduces review cycles, directly improving developer velocity.

**Independent Test**: Can be fully tested by running static analysis tools on the codebase and verifying that configuration is complete, rules are documented, and issues are clearly reported with actionable fixes.

**Acceptance Scenarios**:

1. **Given** a developer commits code with quality issues, **When** they run `./gradlew detekt`, **Then** issues are reported with clear descriptions, severity levels, and locations without requiring tool documentation lookup
2. **Given** formatting violations exist in code, **When** a developer runs `./gradlew spotlessApply`, **Then** all formatting is corrected automatically and consistently across the codebase
3. **Given** detekt configuration needs updates, **When** a developer modifies centralized static analysis config, **Then** changes apply across all modules with clear explanation of which rules changed

---

### User Story 3 - Streamline Testing Strategy (Priority: P1)

As a developer, I want a clear, enforced testing strategy with automated coverage validation so that I can confidently write testable code and know which tests to run for different scenarios (unit, integration, performance).

**Why this priority**: Testing strategy confusion leads to inadequate coverage, failed CI/CD checks, and production bugs. Streamlining this removes friction and ensures consistent test quality across the codebase.

**Independent Test**: Can be fully tested by running the test suite with different scenarios (unit only, with coverage gates, performance tests) and verifying clear output about what passed, what failed, coverage percentages, and which tests to fix.

**Acceptance Scenarios**:

1. **Given** a developer completes a feature, **When** they run `./gradlew testDebugUnitTest`, **Then** unit tests execute quickly with clear pass/fail output and coverage summary
2. **Given** code is ready for CI/CD, **When** they run `./gradlew verifyCoverageThresholds`, **Then** coverage for ViewModel (75%), UI (65%), and Data (70%) layers is validated with clear pass/fail status and affected files
3. **Given** a developer wants to run integration tests, **When** they run the appropriate gradle task, **Then** they know exactly which tests run, how long they take, and whether they passed without ambiguity about test scope

---

### User Story 4 - Developer Documentation & Onboarding (Priority: P2)

As a new developer, I want clear, accessible documentation about build commands, testing strategy, and quality gates so that I can set up my environment and contribute effectively without extensive manual onboarding.

**Why this priority**: Reduces onboarding time and prevents experienced developers from spending time answering repetitive setup questions. Improves team velocity once multiple developers adopt the workflow.

**Independent Test**: Can be fully tested by having a developer new to the project follow documentation to set up their environment and run a complete build/test cycle successfully.

**Acceptance Scenarios**:

1. **Given** a developer reads build documentation, **When** they execute the provided commands, **Then** they understand what each step does and what to expect
2. **Given** a developer needs to understand code quality gates, **When** they reference the testing documentation, **Then** coverage requirements, test layers, and verification commands are clearly documented with examples

---

### User Story 5 - Automated Quality Gate Enforcement (Priority: P2)

As a developer, I want quality gates to be automatically enforced before code is committed so that code quality standards are maintained consistently and I get fast feedback about violations.

**Why this priority**: Manual enforcement is error-prone. Automation ensures all developers follow the same standards and catches issues early in development rather than in code review or CI/CD.

**Independent Test**: Can be fully tested by attempting to commit code that violates quality gates and verifying the commit is blocked with clear error messages and remediation instructions.

**Acceptance Scenarios**:

1. **Given** a developer's code violates Detekt rules, **When** pre-commit hooks are configured, **Then** the commit is prevented with clear guidance on which rules were violated
2. **Given** test coverage falls below thresholds, **When** coverage verification runs, **Then** the build fails with specific layer(s) that need coverage improvements

---

### Edge Cases

- When developers skip local quality checks, CI/CD runs all checks regardless of local execution
- Legacy code that doesn't meet quality standards will be remediated to meet new standards before enforcing gates
- For urgent hotfixes, quality gates can be bypassed with explicit approval or justification
- How are quality gate rules documented so new developers understand the rationale?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST centralize all Gradle build configurations in convention plugins and `gradle/libs.versions.toml` so modules reference conventions rather than duplicating build logic
- **FR-002**: System MUST provide comprehensive Detekt configuration covering code quality, complexity, style, and performance rules with documented rationale for each rule
- **FR-003**: System MUST provide comprehensive Spotless configuration for automatic code formatting (Kotlin, XML, JSON, Markdown) with consistent formatting rules
- **FR-004**: System MUST define and enforce layer-specific coverage thresholds (ViewModel ≥75%, UI ≥65%, Data ≥70%) via automated gradle tasks
- **FR-005**: System MUST provide clear test execution commands that distinguish between unit tests, integration tests (physical parameter for running on connected physical device), and performance tests
- **FR-006**: System MUST generate test reports that clearly show pass/fail status, execution time, and coverage metrics per layer
- **FR-007**: System MUST provide documented pre-commit hooks that enforce static analysis checks locally before allowing commits
- **FR-008**: System MUST provide updated developer documentation including build commands, testing strategy, quality gates, and troubleshooting guide
- **FR-009**: System MUST make Gradle commands cacheable and incremental to minimize build times
- **FR-010**: System MUST document all quality gate rules with explanations of why each rule exists and how to comply

### Key Entities

- **Build Convention Plugins**: Reusable Gradle plugins that encapsulate build configuration for consistency
- **Version Catalog**: Centralized dependency versions in `gradle/libs.versions.toml`
- **Detekt Configuration**: Static analysis rules for code quality, complexity, style, and performance
- **Coverage Report**: Generated test coverage metrics broken down by layer (ViewModel, UI, Data)
- **Quality Gate Rules**: Enforcement rules that block builds when violated (detekt, coverage, formatting)

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All modules must use centralized build conventions (0% duplicated build configuration across module build files)
- **SC-002**: Build consistency is verified: any two builds of the same code produce identical outputs (bit-for-bit reproducibility)
- **SC-003**: Static analysis execution time is <60 seconds for full codebase (`./gradlew detekt`)
- **SC-004**: Coverage verification completes in <180 seconds (`./gradlew verifyCoverageThresholds`)
- **SC-005**: Developers report ≥90% confidence in understanding build commands and testing strategy after reading documentation
- **SC-006**: Zero quality gate violations in CI/CD that could have been caught locally (pre-commit hooks catch 100% of simple violations)
- **SC-007**: Build cache hit rate improves by ≥40% (measure build times before/after with cache optimization)
- **SC-008**: Code review cycle time reduces by ≥25% due to fewer quality-related comments (measure comment count before/after)
- **SC-009**: New developer onboarding time reduces from current baseline to <2 hours for environment setup and first successful build

### Qualitative Outcomes

- **SC-010**: Developers report improved confidence in code quality due to automated enforcement
- **SC-011**: Static analysis reports are considered "actionable" by ≥90% of developers (clear, fixable issues)
- **SC-012**: Documentation is considered "clear and comprehensive" by ≥85% of developers surveyed

## Assumptions

1. Current project structure supports Gradle convention plugins (Gradle 8.0+)
2. Team is comfortable with automated enforcement blocking commits/CI pipelines
3. Existing code can be remediated to meet new quality standards within feature timeline
4. Pre-commit hooks are acceptable for local development (not all developers may use them)
5. Static analysis tools (Detekt, Spotless) adequately cover project quality needs

## Dependencies & Constraints

- Depends on: Gradle 8.0+, Detekt latest stable, Spotless latest stable
- Must maintain backward compatibility with existing module build files during transition
- Coverage thresholds (75/65/70) already documented in AGENTS.md - should not change without stakeholder agreement
- Pre-commit hook implementation must work across different developer environments (macOS, Linux, Windows)
