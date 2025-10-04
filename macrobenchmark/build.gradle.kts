plugins {
  alias(libs.plugins.android.test)
  alias(libs.plugins.kotlin.android)
}

android {
  namespace = "com.vjaykrsna.nanoai.macrobenchmark"
  compileSdk = 36

  defaultConfig {
    minSdk = 31
    targetSdk = 36

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    create("benchmark") {
      isDebuggable = true
      signingConfig = signingConfigs.getByName("debug")
      matchingFallbacks += listOf("release")
    }
  }

  targetProjectPath = ":app"
  experimentalProperties["android.experimental.self-instrumenting"] = true

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  kotlinOptions { jvmTarget = "11" }
}

dependencies {
  implementation(libs.androidx.test.runner)
  implementation(libs.androidx.junit)
  implementation(libs.androidx.uiautomator)
  implementation(libs.androidx.benchmark.macro)
  implementation(libs.truth)
}

androidComponents { beforeVariants(selector().all()) { it.enable = it.buildType == "benchmark" } }
