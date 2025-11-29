package com.vjaykrsna.nanoai.core.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("FeatureFlags")
class FeatureFlagsTest {

  @Nested
  @DisplayName("Production preset")
  inner class ProductionPreset {
    @Test
    fun `all experimental features are disabled`() {
      val flags = FeatureFlags.Production

      assertThat(flags.experimentalFeaturesEnabled).isFalse()
      assertThat(flags.audioFeatureEnabled).isFalse()
      assertThat(flags.imageGenerationEnabled).isFalse()
      assertThat(flags.codeFeatureEnabled).isFalse()
      assertThat(flags.translateFeatureEnabled).isFalse()
    }

    @Test
    fun `audio is not visible`() {
      assertThat(FeatureFlags.Production.isAudioVisible()).isFalse()
    }

    @Test
    fun `image generation is not visible`() {
      assertThat(FeatureFlags.Production.isImageGenerationVisible()).isFalse()
    }

    @Test
    fun `code is not visible`() {
      assertThat(FeatureFlags.Production.isCodeVisible()).isFalse()
    }

    @Test
    fun `translate is not visible`() {
      assertThat(FeatureFlags.Production.isTranslateVisible()).isFalse()
    }
  }

  @Nested
  @DisplayName("Development preset")
  inner class DevelopmentPreset {
    @Test
    fun `all experimental features are enabled`() {
      val flags = FeatureFlags.Development

      assertThat(flags.experimentalFeaturesEnabled).isTrue()
      assertThat(flags.audioFeatureEnabled).isTrue()
      assertThat(flags.imageGenerationEnabled).isTrue()
      assertThat(flags.codeFeatureEnabled).isTrue()
      assertThat(flags.translateFeatureEnabled).isTrue()
    }

    @Test
    fun `audio is visible`() {
      assertThat(FeatureFlags.Development.isAudioVisible()).isTrue()
    }

    @Test
    fun `image generation is visible`() {
      assertThat(FeatureFlags.Development.isImageGenerationVisible()).isTrue()
    }

    @Test
    fun `code is visible`() {
      assertThat(FeatureFlags.Development.isCodeVisible()).isTrue()
    }

    @Test
    fun `translate is visible`() {
      assertThat(FeatureFlags.Development.isTranslateVisible()).isTrue()
    }
  }

  @Nested
  @DisplayName("Preview preset")
  inner class PreviewPreset {
    @Test
    fun `only implemented features are enabled`() {
      val flags = FeatureFlags.Preview

      assertThat(flags.experimentalFeaturesEnabled).isTrue()
      assertThat(flags.audioFeatureEnabled).isTrue()
      assertThat(flags.imageGenerationEnabled).isTrue()
      assertThat(flags.codeFeatureEnabled).isFalse()
      assertThat(flags.translateFeatureEnabled).isFalse()
    }

    @Test
    fun `audio is visible`() {
      assertThat(FeatureFlags.Preview.isAudioVisible()).isTrue()
    }

    @Test
    fun `image generation is visible`() {
      assertThat(FeatureFlags.Preview.isImageGenerationVisible()).isTrue()
    }

    @Test
    fun `code is not visible`() {
      assertThat(FeatureFlags.Preview.isCodeVisible()).isFalse()
    }

    @Test
    fun `translate is not visible`() {
      assertThat(FeatureFlags.Preview.isTranslateVisible()).isFalse()
    }
  }

  @Nested
  @DisplayName("shouldShowFeature logic")
  inner class ShouldShowFeatureLogic {
    @Test
    fun `non-experimental feature shows regardless of master toggle`() {
      val flagsDisabled = FeatureFlags(experimentalFeaturesEnabled = false)
      val flagsEnabled = FeatureFlags(experimentalFeaturesEnabled = true)

      assertThat(flagsDisabled.shouldShowFeature(isExperimental = false, featureEnabled = true))
        .isTrue()
      assertThat(flagsEnabled.shouldShowFeature(isExperimental = false, featureEnabled = true))
        .isTrue()
    }

    @Test
    fun `experimental feature requires master toggle to be true`() {
      val flagsDisabled =
        FeatureFlags(experimentalFeaturesEnabled = false, audioFeatureEnabled = true)
      val flagsEnabled =
        FeatureFlags(experimentalFeaturesEnabled = true, audioFeatureEnabled = true)

      assertThat(flagsDisabled.shouldShowFeature(isExperimental = true, featureEnabled = true))
        .isFalse()
      assertThat(flagsEnabled.shouldShowFeature(isExperimental = true, featureEnabled = true))
        .isTrue()
    }

    @Test
    fun `experimental feature requires individual flag to be true`() {
      val flagsIndividualDisabled =
        FeatureFlags(experimentalFeaturesEnabled = true, audioFeatureEnabled = false)
      val flagsBothEnabled =
        FeatureFlags(experimentalFeaturesEnabled = true, audioFeatureEnabled = true)

      assertThat(
          flagsIndividualDisabled.shouldShowFeature(isExperimental = true, featureEnabled = false)
        )
        .isFalse()
      assertThat(flagsBothEnabled.shouldShowFeature(isExperimental = true, featureEnabled = true))
        .isTrue()
    }
  }
}
