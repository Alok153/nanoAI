# UI Testing Baselines

This directory contains baseline configurations and reference files for UI quality testing and accessibility compliance.

## Files

### `accessibility-baseline.xml`

- **Purpose**: Defines acceptable accessibility violations for Espresso accessibility testing
- **Usage**: Used by `androidx.test.espresso:espresso-accessibility` to establish baseline accessibility compliance
- **Maintenance**: Should remain empty - new accessibility issues should be fixed, not baselined

### `ui-quality-baseline.json`

- **Purpose**: Comprehensive UI quality metrics and thresholds
- **Contents**:
  - Accessibility requirements (touch targets, contrast ratios)
  - Performance benchmarks (frame drops, cold start time)
  - Material Design compliance rules
  - Layout performance guidelines
- **Usage**: Referenced by UI quality lint rules and automated testing

### `ui-test-baseline.properties`

- **Purpose**: Configuration file for UI testing infrastructure
- **Contents**:
  - Screenshot testing settings for visual regression
  - Accessibility testing parameters
  - Performance monitoring thresholds
  - Test execution configuration
- **Usage**: Loaded by UI test utilities and CI/CD pipelines

## Usage in Testing

### Accessibility Testing

```kotlin
// In instrumentation tests
AccessibilityChecks.enable()
    .setRunChecksFromRootView(true)
    .setSuppressingResultMatcher { violation ->
        // Check against baseline violations
        baselineChecker.isViolationAllowed(violation)
    }
```

### UI Quality Checks

```kotlin
// In UI tests or lint rules
val qualityConfig = UiQualityConfig.loadFromBaseline()
assertTrue(qualityConfig.validateComponent(component))
```

## Maintenance Guidelines

1. **Zero Baseline Policy**: These baselines should ideally remain empty
2. **Fix First**: Address UI quality and accessibility issues before adding to baseline
3. **Regular Review**: Periodically audit baseline files to remove outdated entries
4. **Documentation**: Update this README when adding new baseline files

## Related Files

- `config/detekt/baseline.xml` - Static analysis violations baseline
- `config/coverage/layer-map.json` - Test coverage layer classification
- `gradle/libs.versions.toml` - Dependency versions for testing libraries

## CI/CD Integration

These baseline files are referenced in:

- Instrumentation test suites (`androidTest/`)
- UI quality lint rules
- Performance monitoring tasks
- Accessibility compliance checks
