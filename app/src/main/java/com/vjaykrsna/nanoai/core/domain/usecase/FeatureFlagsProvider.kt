package com.vjaykrsna.nanoai.core.domain.usecase

import com.vjaykrsna.nanoai.BuildConfig
import com.vjaykrsna.nanoai.core.domain.model.FeatureFlags
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides feature flag configuration based on build type and user preferences.
 *
 * In debug builds, experimental features are enabled by default for development testing. In release
 * builds, experimental features are hidden to prevent users from encountering non-functional or
 * simulated UI.
 *
 * Future enhancement: Allow user opt-in to experimental features via developer settings.
 */
@Singleton
class FeatureFlagsProvider @Inject constructor() {

  /**
   * Returns the current feature flags configuration.
   *
   * In debug builds: All experimental features visible (Development mode) In release builds: Only
   * production-ready features visible (Production mode)
   */
  fun getFeatureFlags(): FeatureFlags {
    return if (BuildConfig.DEBUG) {
      FeatureFlags.Development
    } else {
      FeatureFlags.Production
    }
  }

  /**
   * Returns true if a feature with the given experimental status should be visible.
   *
   * @param isExperimental Whether the feature is marked as experimental.
   * @return True if the feature should be shown in navigation.
   */
  fun shouldShowExperimentalFeature(isExperimental: Boolean): Boolean {
    return if (isExperimental) {
      getFeatureFlags().experimentalFeaturesEnabled
    } else {
      true
    }
  }
}
