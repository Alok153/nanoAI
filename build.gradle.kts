// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("com.diffplug.spotless") version "7.0.4"
    id("com.github.ben-manes.versions") version "0.53.0"
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
val detektConfig = file("${rootProject.projectDir}/config/quality/detekt/detekt.yml")
val detektBaseline = file("${rootProject.projectDir}/config/quality/detekt/baseline.xml")

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(detektConfig)
    baseline = detektBaseline
    source.setFrom(
        "app/src/main/java",
        "app/src/test/java",
        "app/src/androidTest/java",
        "macrobenchmark/src/main/java",
    )
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = libs.findVersion("kotlinJvmTarget").get().requiredVersion
    config.setFrom(detektConfig)
    baseline = detektBaseline
    parallel = true
}

dependencies {
    detektPlugins("io.nlopez.compose.rules:detekt:0.4.27")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")
    detektPlugins(project(":config:quality:detekt:custom-rules"))
}
