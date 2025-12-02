import com.android.build.api.dsl.ManagedVirtualDevice
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins { id("com.vjaykrsna.nanoai.android.application") }

val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun version(alias: String): String =
  libsCatalog
    .findVersion(alias)
    .orElseThrow { IllegalStateException("Missing version '$alias' in libs.versions.toml") }
    .requiredVersion

configurations.configureEach {
  resolutionStrategy.eachDependency {
    when ("${requested.group}:${requested.name}") {
      "androidx.test.espresso:espresso-core",
      "androidx.test.espresso:espresso-idling-resource" -> {
        useVersion(version("espressoCore"))
        because("Align Espresso with Compose ui-test stack")
      }
      "androidx.test:runner" -> {
        useVersion(version("runner"))
        because("Ensure consistent AndroidX Test runner across configurations")
      }
      "androidx.test:core" -> {
        useVersion(version("androidxTestCore"))
        because("Ensure consistent AndroidX Test core across configurations")
      }
      "androidx.test.ext:junit" -> {
        useVersion(version("junitVersion"))
        because("Ensure consistent AndroidX Test JUnit extensions across configurations")
      }
      "com.google.guava:guava" -> {
        useVersion(version("guavaAndroid"))
        because("Align Guava across MediaPipe runtime and test libraries")
      }
    }
  }
}

fun createQuotedStringBuildConfigField(
  defaultConfig: com.android.build.api.dsl.DefaultConfig,
  name: String,
  propertyName: String,
  defaultValue: String,
) {
  val propertyValue = (project.findProperty(propertyName) as? String)?.trim()
  val finalValue = propertyValue.takeIf { !it.isNullOrBlank() } ?: defaultValue
  val quotedValue = "\"${finalValue.replace("\"", "\\\"")}\""
  defaultConfig.buildConfigField("String", name, quotedValue)
}

android {
  namespace = "com.vjaykrsna.nanoai"

  defaultConfig {
    applicationId = "com.vjaykrsna.nanoai"
    versionCode = 1
    versionName = "1.0"

    createQuotedStringBuildConfigField(this, "HF_OAUTH_CLIENT_ID", "nanoai.hf.oauth.clientId", "")
    createQuotedStringBuildConfigField(
      this,
      "HF_OAUTH_SCOPE",
      "nanoai.hf.oauth.scope",
      "all offline_access",
    )
    createQuotedStringBuildConfigField(
      this,
      "HF_OAUTH_REDIRECT_URI",
      "nanoai.hf.oauth.redirectUri",
      "nanoai://auth/huggingface",
    )
  }

  testOptions {
    managedDevices {
      val pixel6 = allDevices.create("pixel6Api34", ManagedVirtualDevice::class.java)
      pixel6.device = "Pixel 6"
      pixel6.apiLevel = 34
      pixel6.systemImageSource = "aosp-atd"
      pixel6.testedAbi = "x86_64"
      groups.create("ci") { targetDevices.add(pixel6) }
    }
  }

  sourceSets {
    getByName("test") {
      java.srcDir("src/test/contract")
      java.srcDir("src/test/java")
      resources.srcDir(fileTree("$rootDir/config") { exclude("**/build/**") })
    }
  }
}

dependencies {
  implementation(project(":core:common"))
  implementation(project(":core:domain"))

  // MediaPipe
  implementation(libs.mediapipe.tasks.genai)
  implementation(libs.mediapipe.tasks.vision)

  // Leap
  implementation(libs.leap.sdk)

  // Networking
  implementation(libs.retrofit)
  implementation(libs.retrofit.kotlin.serialization)
  implementation(libs.kotlinx.serialization.core)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.okhttp)

  // Database
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  // Coroutines
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.collections.immutable)

  // Date and time utilities
  implementation(libs.kotlinx.datetime)

  // WorkManager
  implementation(libs.androidx.work.runtime.ktx)

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
  implementation(libs.hilt.lifecycle.viewmodel.compose)
  implementation(libs.hilt.work)
  ksp(libs.hilt.compiler)
  ksp(libs.androidx.hilt.compiler)
  kspTest(libs.hilt.compiler)

  // Image Loading
  implementation(libs.coil.compose)

  // ProfileInstaller for baseline profiles
  implementation(libs.androidx.profileinstaller)
  implementation(libs.androidx.metrics.performance)

  // Unit Testing
  testImplementation(kotlin("test-junit5"))
  testImplementation(kotlin("reflect"))
  testImplementation(libs.junit.jupiter.params)
  testImplementation(libs.mockk)
  testImplementation(libs.robolectric)
  testImplementation(libs.truth)
  testImplementation(libs.androidx.room.testing)
  testImplementation(libs.networknt.json.schema.validator)
  testImplementation(libs.mockwebserver)
  testImplementation(libs.androidx.work.testing)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.androidx.test.core)
  testImplementation(libs.androidx.navigation.testing)
  testImplementation(libs.hilt.android.testing)
  testRuntimeOnly(libs.junit.jupiter.engine)
  testRuntimeOnly(libs.junit.platform.launcher)
  testRuntimeOnly(libs.junit.vintage.engine)

  // Instrumentation Testing
  androidTestImplementation(libs.androidx.uiautomator)
  androidTestImplementation(kotlin("test-junit5"))
  androidTestImplementation(libs.junit.jupiter.api)
  androidTestImplementation(libs.junit.jupiter.params)
  androidTestImplementation(libs.junit.vintage.engine)
  androidTestImplementation(libs.mockk.android)
  androidTestImplementation(libs.truth)
  androidTestImplementation(libs.guava.android)
  androidTestImplementation(libs.androidx.room.testing)
  androidTestImplementation(libs.androidx.work.testing)
  androidTestImplementation(libs.mockwebserver)
  androidTestImplementation(libs.androidx.navigation.testing)
  androidTestImplementation(libs.hilt.android.testing)
  kspAndroidTest(libs.hilt.compiler)
}

apply(from = "coverage.gradle.kts")

// TODO: Re-enable lint when newer versions fix the FIR symbol resolution bug
afterEvaluate {
  listOf(
      "lintAnalyzeDebug",
      "lintAnalyzeDebugAndroidTest",
      "lintAnalyzeRelease",
      "lintAnalyzeReleaseAndroidTest",
      "lintVitalAnalyzeRelease",
    )
    .forEach { taskName -> tasks.findByName(taskName)?.enabled = false }
}
