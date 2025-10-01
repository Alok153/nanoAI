// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    
    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.3.1")
        android.set(true)
        outputColorName.set("RED")
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("${rootProject.projectDir}/config/detekt/detekt.yml")
    baseline = file("${rootProject.projectDir}/config/detekt/baseline.xml")
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
