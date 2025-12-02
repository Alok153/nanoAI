package com.vjaykrsna.nanoai.core.domain.usecase

/**
 * Provides build-time or runtime context needed to evaluate feature flags without leaking
 * platform-specific implementations (e.g., BuildConfig) into the domain layer.
 */
interface FeatureFlagEnvironment {
  val isDevelopmentBuild: Boolean
}
