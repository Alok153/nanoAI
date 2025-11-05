package com.vjaykrsna.nanoai.buildlogic

import com.android.build.api.dsl.TestExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidTestingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.test")
        pluginManager.apply("org.jetbrains.kotlin.android")

        extensions.configure<TestExtension> {
            SharedConfiguration.configureTestExtension(target, this)
            defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            buildFeatures.buildConfig = false
            buildTypes {
                maybeCreate("benchmark").apply {
                    isDebuggable = true
                    signingConfig = signingConfigs.getByName("debug")
                    matchingFallbacks += listOf("release")
                }
            }
        }

        ConventionDependencies.configureAndroidTesting(this)
        SharedConfiguration.applyQualityTools(this)
        SharedConfiguration.wireQualityTasks(this)
    }
}
