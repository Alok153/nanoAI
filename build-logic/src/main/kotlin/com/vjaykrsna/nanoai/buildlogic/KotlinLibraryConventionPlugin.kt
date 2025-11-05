package com.vjaykrsna.nanoai.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.JavaVersion
import org.gradle.kotlin.dsl.getByType

class KotlinLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        pluginManager.apply("java-library")

        extensions.getByType<JavaPluginExtension>().apply {
            val javaVersion = JavaVersion.toVersion(libs.versionString("javaTarget"))
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
        }

        SharedConfiguration.configureKotlinCompilation(this)
        ConventionDependencies.configureKotlinLibrary(this)
        SharedConfiguration.applyQualityTools(this)
        SharedConfiguration.wireQualityTasks(this)
    }
}
