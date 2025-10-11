import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.ksp)
  alias(libs.plugins.hilt)
  alias(libs.plugins.androidx.room)
  jacoco
}

android {
  namespace = "com.vjaykrsna.nanoai"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.vjaykrsna.nanoai"
    minSdk = 31
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    vectorDrawables { useSupportLibrary = true }

    val hfClientId = (project.findProperty("nanoai.hf.oauth.clientId") as? String)?.trim().orEmpty()
    val hfScope = (project.findProperty("nanoai.hf.oauth.scope") as? String)?.trim().orEmpty()
    val hfRedirectUri =
      (project.findProperty("nanoai.hf.oauth.redirectUri") as? String)?.trim().orEmpty()
    val quote: (String) -> String = { value -> "\"${value.replace("\"", "\\\"")}\"" }

    buildConfigField("String", "HF_OAUTH_CLIENT_ID", quote(hfClientId))
    buildConfigField(
      "String",
      "HF_OAUTH_SCOPE",
      quote(hfScope.ifBlank { "all offline_access" }),
    )
    buildConfigField(
      "String",
      "HF_OAUTH_REDIRECT_URI",
      quote(hfRedirectUri.ifBlank { "nanoai://auth/huggingface" }),
    )
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
      )
    }
    debug {
      enableAndroidTestCoverage = true
      enableUnitTestCoverage = true
    }
    create("baselineProfile") {
      initWith(buildTypes.getByName("release"))
      signingConfig = signingConfigs.getByName("debug")
      matchingFallbacks += listOf("release")
      isDebuggable = false
    }
    create("benchmark") {
      initWith(buildTypes.getByName("release"))
      signingConfig = signingConfigs.getByName("debug")
      matchingFallbacks += listOf("release")
      isDebuggable = false
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  kotlinOptions {
    jvmTarget = "11"
    val composeMetricsDir = project.layout.buildDirectory.dir("compose/metrics")
    val composeReportsDir = project.layout.buildDirectory.dir("compose/reports")
    freeCompilerArgs +=
      listOf(
        "-opt-in=kotlin.RequiresOptIn",
        "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        "-opt-in=kotlinx.coroutines.FlowPreview",
        "-P",
        "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=${composeMetricsDir.get().asFile.absolutePath}",
        "-P",
        "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=${composeReportsDir.get().asFile.absolutePath}",
      )
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  composeOptions { kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get() }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
      excludes += "/META-INF/LICENSE.md"
      excludes += "/META-INF/LICENSE-notice.md"
    }
  }

  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      isReturnDefaultValues = true
    }
  }

  sourceSets { getByName("test") { java.srcDir("src/test/contract") } }
  sourceSets { getByName("test") { resources.srcDir("$rootDir/config") } }
}

room { schemaDirectory("$projectDir/schemas") }

jacoco {
  toolVersion = "0.8.12"
  reportsDirectory.set(layout.buildDirectory.dir("reports/jacoco"))
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
  extensions.configure(JacocoTaskExtension::class.java) {
    isIncludeNoLocationClasses = true
    excludes = listOf("jdk.internal.*")
  }
}

configurations.configureEach {
  exclude(group = "org.mockito", module = "mockito-core")
  exclude(group = "org.mockito", module = "mockito-android")
}

// Collect common coverage inputs for JVM + instrumentation runs.
val coverageExecutionData =
  files(
    layout.buildDirectory.file("jacoco/testDebugUnitTest.exec"),
    layout.buildDirectory.file(
      "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"
    ),
    fileTree(layout.buildDirectory.dir("outputs/code_coverage").get().asFile) {
      include("**/*.ec")
    },
  )

val coverageClassDirectories =
  files(
    fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug").get()) {
      exclude("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*", "**/*Test*.*")
    },
    fileTree(layout.buildDirectory.dir("intermediates/javac/debug/classes").get()) {
      exclude("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*", "**/*Test*.*")
    },
  )

val coverageSourceDirectories = files("src/main/java", "src/main/kotlin")

tasks.register<JacocoReport>("jacocoFullReport") {
  group = "verification"
  description = "Generates a merged coverage report for unit and instrumentation tests."

  dependsOn("testDebugUnitTest")
  dependsOn("connectedDebugAndroidTest")

  classDirectories.setFrom(coverageClassDirectories)
  additionalClassDirs.setFrom(coverageClassDirectories)
  sourceDirectories.setFrom(coverageSourceDirectories)
  executionData.setFrom(coverageExecutionData)

  reports {
    xml.required.set(true)
    xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/full/jacocoFullReport.xml"))
    html.required.set(true)
    html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/full/html"))
    csv.required.set(false)
  }
}

val coverageClassDirectoriesUnit =
  files(
    fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debugUnitTest").get()) {
      exclude("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*", "**/*Test*.*")
    },
    fileTree(layout.buildDirectory.dir("intermediates/javac/debugUnitTest/classes").get()) {
      exclude("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*", "**/*Test*.*")
    },
  )

tasks.register<JacocoReport>("jacocoUnitReport") {
  group = "verification"
  description = "Generates a coverage report for unit tests only."

  dependsOn("testDebugUnitTest")

  classDirectories.setFrom(coverageClassDirectoriesUnit)
  additionalClassDirs.setFrom(coverageClassDirectoriesUnit)
  sourceDirectories.setFrom(coverageSourceDirectories)
  executionData.setFrom(
    layout.buildDirectory.file(
      "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"
    )
  )

  reports {
    xml.required.set(true)
    xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/unit/jacocoUnitReport.xml"))
    html.required.set(true)
    html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/unit/html"))
    csv.required.set(false)
  }
}

val coverageReportXml = layout.buildDirectory.file("reports/jacoco/full/jacocoFullReport.xml")
val layerMapFile = rootProject.layout.projectDirectory.file("config/coverage/layer-map.json")
val coverageGateMarkdown = layout.buildDirectory.file("coverage/thresholds.md")

tasks.register<JavaExec>("verifyCoverageThresholds") {
  group = "verification"
  description = "Verifies merged coverage meets minimum layer thresholds."

  dependsOn("compileDebugKotlin")
  dependsOn("jacocoFullReport")

  inputs.file(coverageReportXml)
  inputs.file(layerMapFile)
  outputs.file(coverageGateMarkdown)

  mainClass.set("com.vjaykrsna.nanoai.coverage.tasks.VerifyCoverageThresholdsTask")

  classpath(
    files(
      layout.buildDirectory.dir("tmp/kotlin-classes/debug"),
      layout.buildDirectory.dir("intermediates/javac/debug/classes"),
    ),
    configurations.getByName("debugRuntimeClasspath"),
    android.bootClasspath,
  )

  doFirst { coverageGateMarkdown.get().asFile.parentFile.mkdirs() }

  args(
    "--report-xml",
    coverageReportXml.get().asFile.absolutePath,
    "--layer-map",
    layerMapFile.asFile.absolutePath,
    "--markdown",
    coverageGateMarkdown.get().asFile.absolutePath,
    "--build-id",
    "jacocoFullReport",
  )
}

tasks.named("check") {
  dependsOn("jacocoFullReport")
  dependsOn("verifyCoverageThresholds")
}

tasks.register<Exec>("coverageMergeArtifacts") {
  group = "verification"
  description = "Runs helper script to trigger merged coverage generation."

  commandLine(
    "bash",
    "${rootDir}/scripts/coverage/merge-coverage.sh",
    layout.buildDirectory.dir("reports/jacoco/full").get().asFile.absolutePath
  )
}

tasks.register<Exec>("coverageMarkdownSummary") {
  group = "verification"
  description = "Generates markdown coverage summary from merged JaCoCo XML report."

  dependsOn("jacocoFullReport")

  val xmlReport = layout.buildDirectory.file("reports/jacoco/full/jacocoFullReport.xml")
  val markdownOutput = layout.buildDirectory.file("reports/jacoco/full/summary.md")

  inputs.file(xmlReport)
  outputs.file(markdownOutput)

  doFirst { markdownOutput.get().asFile.parentFile.mkdirs() }

  commandLine(
    "python3",
    "${rootDir}/scripts/coverage/generate-summary.py",
    xmlReport.get().asFile.absolutePath,
    markdownOutput.get().asFile.absolutePath,
  )
}

androidComponents {
  beforeVariants(selector().all()) { variant ->
    if (variant.buildType in listOf("benchmark", "baselineProfile")) {
      variant.enable = true
    }
  }
}

dependencies {
  // Core Android
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.material)

  // Compose
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material)
  implementation(libs.androidx.compose.material.iconsExtended)
  implementation(libs.androidx.compose.material3.windowSizeClass)
  implementation(libs.androidx.compose.runtime.tracing)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.window)
  debugImplementation(libs.androidx.compose.ui.test)
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // MediaPipe
  implementation(libs.mediapipe.tasks.genai)

  // Networking
  implementation(libs.retrofit)
  implementation(libs.retrofit.kotlin.serialization)
  implementation(libs.kotlinx.serialization.core)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.okhttp)
  implementation(libs.okhttp.logging.interceptor)

  // Database
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  // Coroutines
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.android)

  // Date and time utilities
  implementation(libs.kotlinx.datetime)

  // WorkManager
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.androidx.security.crypto)
  // Security
  implementation(libs.androidx.security.crypto)

  // DataStore
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.datastore)
  implementation(libs.androidx.datastore.core)
  implementation(libs.androidx.datastore.preferences.core)

  // Hilt
  implementation(libs.hilt.android)
  implementation(libs.hilt.navigation.compose)
  implementation(libs.hilt.work)
  ksp(libs.hilt.compiler)
  ksp(libs.androidx.hilt.compiler)

  // Image Loading
  implementation(libs.coil.compose)

  // ProfileInstaller for baseline profiles
  implementation(libs.androidx.profileinstaller)
  implementation(libs.androidx.metrics.performance)

  // Unit Testing
  testImplementation(kotlin("test-junit5"))
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter.params)
  testImplementation(libs.mockk)
  testImplementation(libs.turbine)
  testImplementation(libs.robolectric)
  testImplementation(libs.truth)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.androidx.room.testing)
  testImplementation(libs.networknt.json.schema.validator)
  testImplementation(libs.mockwebserver)
  testImplementation(libs.androidx.work.testing)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.androidx.test.core)
  testImplementation(libs.androidx.navigation.testing)
  testRuntimeOnly(libs.junit.jupiter.engine)
  testRuntimeOnly(libs.junit.platform.launcher)

  // Instrumentation Testing
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.compose.ui.test)
  androidTestImplementation(libs.mockk)
  androidTestImplementation(libs.truth)
  androidTestImplementation(libs.kotlinx.coroutines.test)
  androidTestImplementation(libs.androidx.room.testing)
  androidTestImplementation(libs.androidx.work.testing)
  androidTestImplementation(libs.mockwebserver)
  androidTestImplementation(libs.androidx.navigation.testing)
}
