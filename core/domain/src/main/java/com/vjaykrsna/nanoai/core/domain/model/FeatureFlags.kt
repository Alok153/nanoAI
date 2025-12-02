package com.vjaykrsna.nanoai.core.domain.model

/**
 * Domain model for feature flags controlling experimental and in-development features.
 *
 * Features marked as experimental have simulated or placeholder backends and should only be enabled
 * in development or preview builds. This model is used to filter UI navigation options so users
 * don't encounter non-functional features.
 *
 * @property audioFeatureEnabled Whether the audio/voice feature is visible in navigation. Currently
 *   experimental with simulated waveform generation.
 * @property imageGenerationEnabled Whether the image generation feature is visible in navigation.
 *   Currently experimental with simulated image generation (no actual AI model).
 * @property codeFeatureEnabled Whether the code workspace feature is visible in navigation.
 *   Currently a placeholder with no implementation.
 * @property translateFeatureEnabled Whether the translation feature is visible in navigation.
 *   Currently a placeholder with no implementation.
 * @property experimentalFeaturesEnabled Master toggle to show/hide all experimental features. When
 *   false, overrides individual feature flags.
 * @see [AGENTS.md] for production readiness criteria
 */
data class FeatureFlags(
  val audioFeatureEnabled: Boolean = false,
  val imageGenerationEnabled: Boolean = false,
  val codeFeatureEnabled: Boolean = false,
  val translateFeatureEnabled: Boolean = false,
  val experimentalFeaturesEnabled: Boolean = false,
) {
  /**
   * Returns true if a feature should be shown based on its experimental status and enabled state.
   *
   * @param isExperimental Whether the feature is experimental/in-development.
   * @param featureEnabled Whether the specific feature flag is enabled.
   */
  fun shouldShowFeature(isExperimental: Boolean, featureEnabled: Boolean): Boolean {
    return if (isExperimental) {
      experimentalFeaturesEnabled && featureEnabled
    } else {
      featureEnabled
    }
  }

  /** Returns true if the audio feature should be visible in navigation. */
  fun isAudioVisible(): Boolean = shouldShowFeature(isExperimental = true, audioFeatureEnabled)

  /** Returns true if the image generation feature should be visible in navigation. */
  fun isImageGenerationVisible(): Boolean =
    shouldShowFeature(isExperimental = true, imageGenerationEnabled)

  /** Returns true if the code feature should be visible in navigation. */
  fun isCodeVisible(): Boolean = shouldShowFeature(isExperimental = true, codeFeatureEnabled)

  /** Returns true if the translation feature should be visible in navigation. */
  fun isTranslateVisible(): Boolean =
    shouldShowFeature(isExperimental = true, translateFeatureEnabled)

  companion object {
    /** Default production configuration with all experimental features disabled. */
    val Production = FeatureFlags()

    /**
     * Development configuration with all experimental features enabled for testing. Use this in
     * debug builds or when the "Developer Options" setting is enabled.
     */
    val Development =
      FeatureFlags(
        audioFeatureEnabled = true,
        imageGenerationEnabled = true,
        codeFeatureEnabled = true,
        translateFeatureEnabled = true,
        experimentalFeaturesEnabled = true,
      )

    /**
     * Preview configuration enabling only features with some backend implementation. Audio and
     * Image have simulated backends; Code and Translate are placeholders.
     */
    val Preview =
      FeatureFlags(
        audioFeatureEnabled = true,
        imageGenerationEnabled = true,
        codeFeatureEnabled = false,
        translateFeatureEnabled = false,
        experimentalFeaturesEnabled = true,
      )
  }
}
