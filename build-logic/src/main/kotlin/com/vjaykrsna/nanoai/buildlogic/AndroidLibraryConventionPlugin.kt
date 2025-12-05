package com.vjaykrsna.nanoai.buildlogic

import androidx.room.gradle.RoomExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("org.jetbrains.kotlin.android")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
        pluginManager.apply("com.google.devtools.ksp")
        pluginManager.apply("com.google.dagger.hilt.android")
        pluginManager.apply("androidx.room")
        pluginManager.apply("jacoco")

        extensions.configure<LibraryExtension> {
            SharedConfiguration.configureLibraryExtension(target, this)
            buildFeatures {
                buildConfig = true
            }
        }

        extensions.configure<RoomExtension> {
            schemaDirectory(target.layout.projectDirectory.dir("schemas"))
        }

        SharedConfiguration.applyQualityTools(this)
        SharedConfiguration.wireQualityTasks(this)
    }
}
