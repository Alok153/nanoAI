// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
    id("com.diffplug.spotless") version "6.25.0"
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("${rootProject.projectDir}/config/detekt/detekt.yml")
    baseline = file("${rootProject.projectDir}/config/detekt/baseline.xml")
    source.setFrom(
        "app/src/main/java",
        "app/src/test/java",
        "app/src/androidTest/java",
        "macrobenchmark/src/main/java",
    )
}

subprojects {
    listOf("org.jetbrains.kotlin.jvm", "org.jetbrains.kotlin.android").forEach { kotlinPluginId ->
        pluginManager.withPlugin(kotlinPluginId) {
            apply(plugin = "com.diffplug.spotless")
            configure<com.diffplug.gradle.spotless.SpotlessExtension> {
                kotlin {
                    target("**/*.kt")
                    // ktfmt auto-wraps at 100 chars, cannot be configured
                    ktfmt().googleStyle()
                }

                kotlinGradle {
                    target("**/*.gradle.kts")
                    ktfmt().googleStyle()
                }
            }
        }
    }
}

dependencies {
    detektPlugins("io.nlopez.compose.rules:detekt:0.4.9")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.7")
}
