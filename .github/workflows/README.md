# GitHub Actions Workflows

This directory contains the modular CI/CD pipeline for the nanoAI Android project.

## Architecture

The pipeline is structured as a **modular, composable system** with the following components:

### Composite Actions (`.github/actions/`)
Reusable building blocks that encapsulate common setup logic:

- **`setup-android`**: Configures JDK, Gradle cache, and project permissions
- **`setup-emulator`**: Sets up KVM permissions and AVD caching for emulator jobs

### Component Workflows (`.github/workflows/components/`)
Focused, single-responsibility workflows that can be called independently:

- **`quality-checks.yml`**: Code quality gates (Spotless, Detekt, Android Lint)
- **`unit-tests.yml`**: JVM unit tests
- **`instrumented-tests.yml`**: Device tests across multiple API levels
- **`coverage.yml`**: Coverage analysis and reporting
- **`macrobenchmark.yml`**: Performance benchmarking

### Main Workflow (`.github/workflows/ci.yml`)
The orchestrating workflow that coordinates all components with proper dependencies and concurrency controls.

### Configuration (`.github/config/ci-config.json`)
Centralized configuration for API levels, timeouts, and emulator settings.

## Benefits

- **Modularity**: Each component has a single responsibility
- **Reusability**: Components can be used independently or combined
- **Maintainability**: Changes to setup logic only need to be made in one place
- **Flexibility**: Easy to add new workflows or modify existing ones
- **Parallelization**: Independent jobs run in parallel for faster CI
- **Configuration**: Centralized config reduces duplication

## Usage

### Running Individual Components
```yaml
jobs:
  my-quality-check:
    uses: ./.github/workflows/components/quality-checks.yml
    with:
      upload-reports: true
```

### Customizing Test Matrix
```yaml
jobs:
  my-instrumented-tests:
    uses: ./.github/workflows/components/instrumented-tests.yml
    with:
      api-levels: '[29, 31, 33]'
      targets: '[google_apis, default]'
```

## Migration from Monolithic Workflow

The previous `android-ci.yml` has been replaced with this modular structure. Key improvements:

1. **Reduced duplication**: Common setup steps are now in composite actions
2. **Better organization**: Related functionality is grouped together
3. **Easier maintenance**: Changes to emulator setup only need to be made once
4. **Improved readability**: Smaller, focused files are easier to understand
5. **Enhanced flexibility**: Components can be mixed and matched for different scenarios

## Adding New Components

1. Create a new workflow in `.github/workflows/components/`
2. Use composite actions for common setup
3. Add inputs for configurability
4. Update the main `ci.yml` to include the new component
5. Update configuration in `ci-config.json` if needed
