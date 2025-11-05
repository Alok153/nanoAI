package com.vjaykrsna.nanoai.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("org.jetbrains.kotlin.android")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        pluginManager.apply("com.google.devtools.ksp")
        pluginManager.apply("com.google.dagger.hilt.android")
        pluginManager.apply("jacoco")

        extensions.configure<LibraryExtension> {
            SharedConfiguration.configureLibraryExtension(target, this)
            SharedConfiguration.enableCompose(target, this)

            buildFeatures {
                buildConfig = true
            }
        }

        ConventionDependencies.configureFeature(this)
        SharedConfiguration.applyQualityTools(this)
        SharedConfiguration.wireQualityTasks(this)
    }
}
