package com.vjaykrsna.nanoai.core.data.usecase

import com.vjaykrsna.nanoai.BuildConfig
import com.vjaykrsna.nanoai.core.domain.usecase.FeatureFlagEnvironment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildConfigFeatureFlagEnvironment @Inject constructor() : FeatureFlagEnvironment {
  override val isDevelopmentBuild: Boolean = BuildConfig.DEBUG
}
