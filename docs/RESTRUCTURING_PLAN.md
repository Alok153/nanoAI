# nanoAI Project Restructuring Plan

## Overview

This document outlines a comprehensive restructuring plan for the nanoAI Android project to establish consistent architecture patterns across all modules, features, and test suites. The current structure has grown organically and contains inconsistencies that make maintenance difficult.

## Current Issues Identified

1. **Inconsistent Feature Layering**: Some features have `data/` directories while others don't
2. **Mixed Concerns**: Core utilities mixed with feature-specific code
3. **Test Coverage Gaps**: Inconsistent test organization across features
4. **Benchmark Coverage**: Limited benchmark tests for performance-critical features
5. **Documentation Scatter**: Specs and docs not consistently organized by feature

## Proposed Architecture Principles

### Clean Architecture Layers
Each feature module follows a strict layered architecture:
```
feature/[name]/
├── data/           # Data access layer (repositories, data sources, DTOs)
├── domain/         # Business logic layer (use cases, entities, interfaces)
├── presentation/   # Presentation layer (ViewModels, UI state)
└── ui/             # UI layer (Composables, screens, components)
```

### Shared Modules
```
core/               # Cross-cutting concerns (common utilities, base classes)
├── common/         # Shared utilities and extensions
├── data/           # Shared data components (database, network clients)
├── device/         # Device-specific utilities
├── di/             # Dependency injection modules
├── domain/         # Shared domain models and interfaces
├── maintenance/    # Maintenance and migration utilities
├── model/          # Shared data models
├── network/        # Network utilities and interceptors
└── runtime/        # Runtime utilities (permissions, etc.)

shared/             # Feature-shared components
├── ui/             # Shared UI components and themes
├── model/          # Shared domain models
└── utils/          # Shared utilities
```

## Target Project Structure

```
nanoAI/
├── app/                           # Main application module
│   ├── src/main/java/com/vjaykrsna/nanoai/
│   │   ├── core/                  # Cross-cutting concerns
│   │   │   ├── common/            # Shared utilities
│   │   │   ├── data/              # Shared data layer
│   │   │   ├── device/            # Device utilities
│   │   │   ├── di/                # DI modules
│   │   │   ├── domain/            # Shared domain
│   │   │   ├── maintenance/       # Maintenance tools
│   │   │   ├── model/             # Shared models
│   │   │   ├── network/           # Network utilities
│   │   │   └── runtime/           # Runtime utilities
│   │   ├── feature/               # Feature modules
│   │   │   ├── audio/             # Audio processing feature
│   │   │   │   ├── data/
│   │   │   │   ├── domain/
│   │   │   │   ├── presentation/
│   │   │   │   └── ui/
│   │   │   ├── chat/              # Chat/AI conversation feature
│   │   │   │   ├── data/
│   │   │   │   ├── domain/
│   │   │   │   ├── presentation/
│   │   │   │   └── ui/
│   │   │   ├── image/             # Image processing feature
│   │   │   │   ├── data/
│   │   │   │   ├── domain/
│   │   │   │   ├── presentation/
│   │   │   │   └── ui/
│   │   │   ├── library/           # Model library management
│   │   │   │   ├── data/
│   │   │   │   ├── domain/
│   │   │   │   ├── presentation/
│   │   │   │   └── ui/
│   │   │   ├── settings/          # Settings and configuration
│   │   │   │   ├── data/
│   │   │   │   ├── domain/
│   │   │   │   ├── presentation/
│   │   │   │   └── ui/
│   │   │   └── uiux/              # UI/UX management
│   │   │       ├── data/
│   │   │       ├── domain/
│   │   │       ├── presentation/
│   │   │       └── ui/
│   │   ├── shared/                # Feature-shared components
│   │   │   ├── model/             # Shared domain models
│   │   │   ├── ui/                # Shared UI components
│   │   │   └── utils/             # Shared utilities
│   │   ├── MainActivity.kt        # Application entry point
│   │   └── NanoAIApplication.kt   # Application class
│   ├── src/test/java/com/vjaykrsna/nanoai/  # Unit tests
│   │   ├── core/
│   │   │   ├── common/
│   │   │   ├── data/
│   │   │   ├── device/
│   │   │   ├── di/
│   │   │   ├── domain/
│   │   │   ├── maintenance/
│   │   │   ├── model/
│   │   │   ├── network/
│   │   │   └── runtime/
│   │   ├── feature/
│   │   │   ├── audio/
│   │   │   │   ├── data/
│   │   │   │   ├── domain/
│   │   │   │   └── presentation/
│   │   │   ├── chat/
│   │   │   │   ├── data/
│   │   │   │   ├── domain/
│   │   │   │   └── presentation/
│   │   │   ├── image/
│   │   │   │   ├── data/
│   │   │   │   ├── domain/
│   │   │   │   └── presentation/
│   │   │   ├── library/
│   │   │   │   ├── data/
│   │   │   │   ├── domain/
│   │   │   │   └── presentation/
│   │   │   ├── settings/
│   │   │   │   ├── data/
│   │   │   │   ├── domain/
│   │   │   │   └── presentation/
│   │   │   └── uiux/
│   │   │       ├── data/
│   │   │       ├── domain/
│   │   │       └── presentation/
│   │   └── shared/
│   │       ├── model/
│   │       └── utils/
│   └── src/androidTest/java/com/vjaykrsna/nanoai/  # Integration tests
│       ├── core/
│       │   ├── data/
│       │   ├── device/
│       │   ├── network/
│       │   └── runtime/
│       ├── feature/
│       │   ├── audio/
│       │   │   └── ui/
│       │   ├── chat/
│       │   │   └── ui/
│       │   ├── image/
│       │   │   └── ui/
│       │   ├── library/
│       │   │   └── ui/
│       │   ├── settings/
│       │   │   └── ui/
│       │   └── uiux/
│       │       └── ui/
│       └── shared/
│           └── ui/
├── macrobenchmark/                # Performance benchmarks
│   ├── src/main/java/com/vjaykrsna/nanoai/
│   │   ├── core/
│   │   │   ├── data/
│   │   │   ├── network/
│   │   │   └── runtime/
│   │   ├── feature/
│   │   │   ├── audio/
│   │   │   ├── chat/
│   │   │   ├── image/
│   │   │   ├── library/
│   │   │   ├── settings/
│   │   │   └── uiux/
│   │   └── shared/
│   │       └── ui/
├── docs/                          # Documentation
│   ├── architecture/              # Architecture docs
│   ├── features/                  # Feature documentation
│   ├── development/               # Development guides
│   └── api/                       # API documentation
├── specs/                         # Feature specifications
│   ├── foundation/                # Foundation specs
│   ├── features/                  # Feature specs
│   └── maintenance/               # Maintenance specs
├── config/                        # Configuration files
│   ├── quality/                   # Code quality configs
│   ├── testing/                   # Test configurations
│   └── build/                     # Build configurations
├── scripts/                       # Build and utility scripts
├── gradle/                        # Gradle wrapper and configs
└── build/                         # Build outputs
```

## Migration Strategy

### Phase 1: Core Restructuring
1. **Create new directory structure** for all modules
2. **Move existing files** to appropriate locations
3. **Update import statements** and package declarations
4. **Create missing data/ directories** for features that lack them

### Phase 2: Test Alignment
1. **Standardize test structure** across all features
2. **Add missing test directories** and placeholder files
3. **Update test configurations** to match new structure
4. **Ensure benchmark coverage** for performance-critical features

### Phase 3: Documentation Reorganization
1. **Reorganize docs/** into logical subdirectories
2. **Move feature specs** to `specs/features/`
3. **Update documentation links** and references
4. **Create architecture documentation** for the new structure

### Phase 4: Build and Validation
1. **Update Gradle configurations** for new module structure
2. **Run all tests** to ensure nothing is broken
3. **Validate build process** and artifact generation
4. **Update CI/CD pipelines** if necessary

## Implementation Checklist

### Directory Structure
- [x] Create `shared/` module under `app/src/main/java/com/vjaykrsna/nanoai/`
- [x] Ensure all features have complete layer directories (data/, domain/, presentation/, ui/)
- [x] Reorganize `core/` subdirectories for clarity
- [x] Create consistent test directory structure
- [x] Add benchmark directories for all features

### File Migration
- [x] Move shared UI components to `shared/ui/`
- [x] Move shared models to `shared/model/`
- [x] Consolidate common utilities in `core/common/`
- [x] Update package declarations for moved files
- [x] Fix all import statements

### Test Coverage
- [x] Ensure unit tests exist for all layers
- [x] Add integration tests for data and network layers
- [x] Create UI tests for all screens
- [x] Add performance benchmarks for critical paths

### Documentation
- [x] Reorganize `docs/` into subdirectories
- [x] Move feature specs to `specs/features/` (reverted - specs/ kept as temporary directory)
- [x] Update all documentation references
- [x] Create migration guide for developers

### Build and Quality
- [x] Fix deprecated API usage (getBitmap replaced with ImageDecoder)
- [x] Resolve Detekt violations (magic numbers replaced with constants)
- [x] Fix configuration cache issues
- [x] Ensure all builds pass
- [x] All tests execute successfully

## Benefits of New Structure

1. **Consistency**: All features follow the same architectural pattern
2. **Maintainability**: Clear separation of concerns and responsibilities
3. **Testability**: Consistent test organization enables better coverage tracking
4. **Scalability**: Easy to add new features following established patterns
5. **Developer Experience**: Predictable structure reduces cognitive load

## Future Considerations

1. **Modularization**: Consider converting features to separate Gradle modules when they grow large
2. **Shared Libraries**: Extract common functionality into separate libraries if needed
3. **Build Optimization**: Use build variants for different feature sets if required
4. **Documentation Automation**: Consider generating architecture diagrams from code

## Validation Criteria

- [x] All Gradle builds pass
- [x] All tests execute successfully
- [x] Code coverage meets established thresholds (75% ViewModel, 65% UI, 70% Data)
- [x] Static analysis passes (Detekt, Spotless)
- [x] Benchmarks run without errors
- [x] Documentation is accessible and up-to-date
- [x] No code references files in temporary specs/ directory

---

## Implementation Summary

**Completed on:** October 25, 2025

### What Was Accomplished
1. **Complete Architecture Restructuring**: Established consistent clean architecture patterns across all features with proper data/domain/presentation/ui layers
2. **Shared Module Creation**: Introduced `shared/` module for cross-feature components (UI, models, utilities)
3. **Test Structure Standardization**: Aligned test and androidTest directories with main code structure
4. **Import and Package Updates**: Updated all import statements and package declarations for moved files
5. **Build Quality Improvements**: Fixed deprecated APIs, resolved Detekt violations, and addressed configuration cache issues
6. **Documentation Reorganization**: Reorganized docs/ into logical subdirectories (architecture/, features/, development/, api/)
7. **Specs Directory Management**: Reverted specs/ restructuring to maintain it as a temporary directory, ensuring no code dependencies

### Key Architectural Changes
- **Feature Layering**: All features now have complete data/domain/presentation/ui structure
- **Shared Components**: UI themes, navigation, and models moved to dedicated shared module
- **Clean Separation**: Strict adherence to clean architecture principles with proper dependency flow
- **Test Coverage**: Consistent test organization enabling better coverage tracking

### Quality Assurance
- ✅ All builds pass without warnings
- ✅ All unit tests execute successfully
- ✅ Static analysis (Detekt) passes
- ✅ Code formatting (Spotless) compliant
- ✅ No deprecated API usage
- ✅ Configuration cache compatible

### Files Moved/Created
- **UI Components**: Moved from `ui/` to `shared/ui/` (Theme.kt, NavigationScaffold.kt, etc.)
- **Models**: Moved from `model/` to `shared/model/` (catalog/, huggingface/, leap/)
- **Documentation**: Reorganized into `docs/architecture/`, `docs/features/`, etc.
- **Test Structure**: Aligned androidTest and test directories with main code structure

---

*This restructuring plan ensures nanoAI maintains clean architecture principles while providing a scalable foundation for future development.*
