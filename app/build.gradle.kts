plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.ksp)
  alias(libs.plugins.hilt)
  alias(libs.plugins.androidx.room)
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
}

room { schemaDirectory("$projectDir/schemas") }

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

  // Unit Testing
  testImplementation(kotlin("test"))
  testImplementation(libs.junit)
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
