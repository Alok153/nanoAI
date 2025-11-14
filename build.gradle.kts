// Top-level build file where you can add configuration options common to all sub-projects/modules.
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.versions)
}

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val detektConfig = file("${rootProject.projectDir}/config/quality/detekt/detekt.yml")
val detektBaseline = file("${rootProject.projectDir}/config/quality/detekt/baseline.xml")
