package com.vjaykrsna.nanoai.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal object ConventionDependencies {
    fun configureApplication(project: Project) {
        project.addPlatform("implementation", "androidx-compose-bom")
        project.addImplementation(
            "androidx-core-ktx",
            "androidx-appcompat",
            "material",
            "androidx-compose-ui",
            "androidx-compose-ui-graphics",
            "androidx-compose-ui-tooling-preview",
            "androidx-compose-material3",
            "androidx-compose-material",
            "androidx-compose-material-iconsExtended",
            "androidx-compose-material3-windowSizeClass",
            "androidx-activity-compose",
            "androidx-navigation-compose",
            "androidx-lifecycle-runtime-compose",
            "androidx-lifecycle-viewmodel-compose",
            "androidx-window",
        )

        project.addPlatform("debugImplementation", "androidx-compose-bom")
        project.addToConfiguration(
            "debugImplementation",
            "androidx-compose-ui-test",
            "androidx-compose-ui-tooling",
            "androidx-compose-ui-test-manifest",
        )

        project.addPlatform("androidTestImplementation", "androidx-compose-bom")
        project.addToConfiguration(
            "androidTestImplementation",
            "androidx-compose-runtime",
            "androidx-compose-ui",
            "androidx-compose-ui-test",
            "androidx-compose-ui-test-junit4",
            "androidx-junit",
            "androidx-espresso-core",
            "androidx-test-runner",
        )

        project.addToConfiguration(
            "testImplementation",
            "turbine",
            "kotlinx-coroutines-test",
            "junit-jupiter-api",
            "roborazzi-core",
            "roborazzi-compose",
            "roborazzi-junit",
        )
        project.addToConfiguration(
            "androidTestImplementation",
            "kotlinx-coroutines-test",
            "turbine",
        )
    }

    fun configureFeature(project: Project) {
        project.addPlatform("implementation", "androidx-compose-bom")
        project.addImplementation(
            "androidx-core-ktx",
            "androidx-compose-ui",
            "androidx-compose-ui-graphics",
            "androidx-compose-ui-tooling-preview",
            "androidx-compose-material3",
            "androidx-compose-material",
            "androidx-compose-material-iconsExtended",
            "androidx-activity-compose",
            "androidx-navigation-compose",
            "androidx-lifecycle-runtime-compose",
            "androidx-lifecycle-viewmodel-compose",
        )

        project.addToConfiguration(
            "testImplementation",
            "turbine",
            "kotlinx-coroutines-test",
            "junit-jupiter-api",
        )
    }

    fun configureComposeLibrary(project: Project) {
        project.addPlatform("implementation", "androidx-compose-bom")
        project.addImplementation(
            "androidx-compose-ui",
            "androidx-compose-ui-graphics",
            "androidx-compose-ui-tooling-preview",
            "androidx-compose-material3",
            "androidx-compose-material-iconsExtended",
        )

        project.addToConfiguration(
            "testImplementation",
            "turbine",
            "kotlinx-coroutines-test",
        )
    }

    fun configureAndroidTesting(project: Project) {
        project.addToConfiguration(
            "implementation",
            "androidx-test-runner",
            "androidx-junit",
            "androidx-uiautomator",
            "androidx-benchmark-macro",
            "truth",
            "junit-jupiter-api",
        )
    }

    fun configureKotlinLibrary(project: Project) {
        project.addToConfiguration(
            "testImplementation",
            "junit-jupiter-api",
            "kotlinx-coroutines-test",
            "turbine",
            "truth",
        )
        project.dependencies.add("testRuntimeOnly", project.libs.findLibrary("junit-jupiter-engine").get())
    }
}

private fun Project.addImplementation(vararg aliases: String) {
    addToConfiguration("implementation", *aliases)
}

private fun Project.addPlatform(configurationName: String, alias: String) {
    val dependencyProvider = libs.findLibrary(alias).orElseThrow {
        IllegalStateException("Missing dependency '$alias' in libs.versions.toml")
    }
    val platformDependency = dependencies.platform(dependencyProvider.get())
    dependencies.add(configurationName, platformDependency)
}

private fun Project.addToConfiguration(configurationName: String, vararg aliases: String) {
    aliases.forEach { alias ->
        libs.findLibrary(alias).ifPresent { dependency ->
            dependencies.add(configurationName, dependency.get())
        }
    }
}
