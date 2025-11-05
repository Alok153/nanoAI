package com.vjaykrsna.nanoai.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.TestExtension
import io.gitlab.arturbosch.detekt.Detekt
import java.util.Locale
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal object SharedConfiguration {
    private val QUALITY_TASKS = listOf("detekt", "spotlessCheck")

    fun configureApplicationExtension(project: Project, extension: ApplicationExtension) {
        extension.applyCommonAndroidDefaults(project)
        configureKotlinCompilation(project)
    }

    fun configureLibraryExtension(project: Project, extension: LibraryExtension) {
        extension.applyCommonAndroidDefaults(project)
        configureKotlinCompilation(project)
    }

    fun configureTestExtension(project: Project, extension: TestExtension) {
        extension.applyTestDefaults(project)
        configureKotlinCompilation(project)
    }

    fun enableCompose(project: Project, extension: ApplicationExtension) {
        extension.enableComposeDefaults(project)
    }

    fun enableCompose(project: Project, extension: LibraryExtension) {
        extension.enableComposeDefaults(project)
    }

    fun configureKotlinCompilation(project: Project) {
        val libs = project.libs
        val jvmTarget = libs.versionString("kotlinJvmTarget")
        val compilerArgs = libs
            .versionString("kotlinCompilerArgs")
            .split('|')
            .map(String::trim)
            .filter(String::isNotEmpty)

        project.plugins.withId("org.jetbrains.kotlin.android") {
            project.extensions.configure<KotlinAndroidProjectExtension> {
                compilerOptions.applyJvmOptions(jvmTarget, compilerArgs)
            }
        }

        project.plugins.withId("org.jetbrains.kotlin.jvm") {
            project.extensions.configure<KotlinJvmProjectExtension> {
                compilerOptions.applyJvmOptions(jvmTarget, compilerArgs)
            }
        }

        val javaVersion = jvmTargetToJavaVersion(jvmTarget)
        project.tasks.withType<JavaCompile>().configureEach {
            sourceCompatibility = javaVersion.toString()
            targetCompatibility = javaVersion.toString()
        }

        project.tasks.withType<KotlinCompile>().configureEach {
            compilerOptions.applyJvmOptions(jvmTarget, compilerArgs)
        }

        project.tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }

    fun applyQualityTools(project: Project) {
        val pluginManager = project.pluginManager
        if (!pluginManager.hasPlugin("io.gitlab.arturbosch.detekt")) {
            pluginManager.apply("io.gitlab.arturbosch.detekt")
        }
        if (!pluginManager.hasPlugin("com.diffplug.spotless")) {
            pluginManager.apply("com.diffplug.spotless")
        }

        val spotlessDir = project.rootProject.file("config/quality/spotless")
        val kotlinScript = spotlessDir.resolve("spotless.kotlin.gradle")
        val miscScript = spotlessDir.resolve("spotless.misc.gradle")
        if (kotlinScript.exists()) {
            project.apply(mapOf("from" to kotlinScript))
        }
        if (miscScript.exists()) {
            project.apply(mapOf("from" to miscScript))
        }

        val detektConfig = project.rootProject.file("config/quality/detekt/detekt.yml")
        val detektBaseline = project.rootProject.file("config/quality/detekt/baseline.xml")

        project.configurations.maybeCreate("detektPlugins")
        project.dependencies.add(
            "detektPlugins",
            project.dependencies.project(mapOf("path" to ":config:quality:detekt:custom-rules")),
        )
        project.dependencies.add("detektPlugins", "io.nlopez.compose.rules:detekt:0.4.9")
        project.dependencies.add("detektPlugins", "io.gitlab.arturbosch.detekt:detekt-formatting:1.23.7")

        project.tasks.withType<Detekt>().configureEach {
            buildUponDefaultConfig = true
            config.setFrom(detektConfig)
            if (detektBaseline.exists()) {
                baseline.set(detektBaseline)
            }
            jvmTarget = project.libs.versionString("kotlinJvmTarget")
        }
    }

    fun wireQualityTasks(project: Project) {
        val checkTasks = project.tasks.matching { task ->
            task.name.equals("check", ignoreCase = true) ||
                task.name.equals("connectedCheck", ignoreCase = true)
        }

        QUALITY_TASKS.forEach { qualityTaskName ->
            checkTasks.configureEach {
                dependsOn(qualityTaskName)
            }
        }
    }

    private fun ApplicationExtension.applyCommonAndroidDefaults(project: Project) {
        val libs = project.libs
        val javaVersion = libs.javaVersion("javaTarget")

        compileSdk = libs.versionInt("androidCompileSdk")
        defaultConfig {
            minSdk = libs.versionInt("androidMinSdk")
            targetSdk = libs.versionInt("androidTargetSdk")
            testInstrumentationRunner =
                "com.vjaykrsna.nanoai.shared.testing.NanoAIHiltTestRunner"
            vectorDrawables.useSupportLibrary = true
        }
        compileOptions {
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
        }
        packaging {
            resources.excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/LICENSE.md",
                "/META-INF/LICENSE-notice.md",
            )
        }
        testOptions {
            unitTests.isIncludeAndroidResources = true
            unitTests.isReturnDefaultValues = true
        }
    }

    private fun LibraryExtension.applyCommonAndroidDefaults(project: Project) {
        val libs = project.libs
        val javaVersion = libs.javaVersion("javaTarget")

        compileSdk = libs.versionInt("androidCompileSdk")
        defaultConfig {
            minSdk = libs.versionInt("androidMinSdk")
            targetSdk = libs.versionInt("androidTargetSdk")
            testInstrumentationRunner =
                "com.vjaykrsna.nanoai.shared.testing.NanoAIHiltTestRunner"
            vectorDrawables.useSupportLibrary = true
        }
        compileOptions {
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
        }
        packaging {
            resources.excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/LICENSE.md",
                "/META-INF/LICENSE-notice.md",
            )
        }
        testOptions {
            unitTests.isIncludeAndroidResources = true
            unitTests.isReturnDefaultValues = true
        }
    }

    private fun TestExtension.applyTestDefaults(project: Project) {
        val libs = project.libs
        val javaVersion = libs.javaVersion("javaTarget")

        compileSdk = libs.versionInt("androidCompileSdk")
        defaultConfig {
            minSdk = libs.versionInt("androidMinSdk")
            targetSdk = libs.versionInt("androidTargetSdk")
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        compileOptions {
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
        }
        packaging {
            resources.excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/LICENSE.md",
                "/META-INF/LICENSE-notice.md",
            )
        }
        testOptions {
            unitTests.isIncludeAndroidResources = true
            unitTests.isReturnDefaultValues = true
        }
    }

    private fun ApplicationExtension.enableComposeDefaults(project: Project) {
        enableComposeCommon(project)
    }

    private fun LibraryExtension.enableComposeDefaults(project: Project) {
        enableComposeCommon(project)
    }

    private fun ApplicationExtension.enableComposeCommon(project: Project) {
        buildFeatures.compose = true
        composeOptions.kotlinCompilerExtensionVersion = project.libs.versionString("composeCompiler")
        project.configureComposeCompilerReports()
    }

    private fun LibraryExtension.enableComposeCommon(project: Project) {
        buildFeatures.compose = true
        composeOptions.kotlinCompilerExtensionVersion = project.libs.versionString("composeCompiler")
        project.configureComposeCompilerReports()
    }

    private fun Project.configureComposeCompilerReports() {
        val metricsDir = layout.buildDirectory.dir("compose/metrics")
        val reportsDir = layout.buildDirectory.dir("compose/reports")

        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions.freeCompilerArgs.addAll(
                listOf(
                    "-P",
                    "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=${metricsDir.get().asFile.absolutePath}",
                    "-P",
                    "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=${reportsDir.get().asFile.absolutePath}",
                ),
            )
        }
    }

    private fun KotlinJvmCompilerOptions.applyJvmOptions(
        jvmTarget: String,
        compilerArgs: List<String>,
    ) {
        this.jvmTarget.set(JvmTarget.fromTarget(jvmTarget))
        freeCompilerArgs.addAll(compilerArgs)
    }

    private fun jvmTargetToJavaVersion(target: String): JavaVersion {
        return JavaVersion.toVersion(target.lowercase(Locale.ROOT).removePrefix("1."))
    }
}

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.versionInt(alias: String): Int =
    findVersion(alias).orElseThrow {
        IllegalStateException("Missing version '$alias' in libs.versions.toml")
    }.requiredVersion.toInt()

internal fun VersionCatalog.versionString(alias: String): String =
    findVersion(alias).orElseThrow {
        IllegalStateException("Missing version '$alias' in libs.versions.toml")
    }.requiredVersion

private fun VersionCatalog.javaVersion(alias: String): JavaVersion =
    JavaVersion.toVersion(versionString(alias))
