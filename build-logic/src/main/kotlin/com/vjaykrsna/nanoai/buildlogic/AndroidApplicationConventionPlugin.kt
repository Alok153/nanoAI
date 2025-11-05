package com.vjaykrsna.nanoai.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import androidx.room.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("org.jetbrains.kotlin.android")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
        pluginManager.apply("com.google.devtools.ksp")
        pluginManager.apply("com.google.dagger.hilt.android")
        pluginManager.apply("androidx.room")
        pluginManager.apply("jacoco")

        extensions.configure<ApplicationExtension> {
            SharedConfiguration.configureApplicationExtension(target, this)
            SharedConfiguration.enableCompose(target, this)

            buildFeatures {
                buildConfig = true
            }

            buildTypes {
                named("debug") {
                    enableAndroidTestCoverage = true
                    enableUnitTestCoverage = true
                }
                named("release") {
                    isMinifyEnabled = true
                    isShrinkResources = true
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro",
                    )
                }
                maybeCreate("baselineProfile").apply {
                    initWith(getByName("release"))
                    matchingFallbacks += listOf("release")
                    isDebuggable = false
                    signingConfig = signingConfigs.getByName("debug")
                }
                maybeCreate("benchmark").apply {
                    initWith(getByName("release"))
                    matchingFallbacks += listOf("release")
                    isDebuggable = false
                    signingConfig = signingConfigs.getByName("debug")
                }
            }
        }

        extensions.configure<ApplicationAndroidComponentsExtension> {
            beforeVariants { variant ->
                if (variant.buildType in listOf("benchmark", "baselineProfile")) {
                    variant.enable = false
                }
            }
        }

        extensions.configure<RoomExtension> {
            schemaDirectory(target.layout.projectDirectory.dir("schemas"))
        }

        ConventionDependencies.configureApplication(this)
        SharedConfiguration.applyQualityTools(this)
        SharedConfiguration.wireQualityTasks(this)
        apply(mapOf("from" to rootProject.file("config/testing/tooling/roborazzi-config.gradle.kts")))
    }
}
