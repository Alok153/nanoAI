package com.vjaykrsna.nanoai.core.data.config

import com.vjaykrsna.nanoai.core.data.BuildConfig
import com.vjaykrsna.nanoai.core.domain.usecase.FeatureFlagEnvironment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildConfigFeatureFlagEnvironment @Inject constructor() : FeatureFlagEnvironment {
  override val isDevelopmentBuild: Boolean = BuildConfig.DEBUG
}
