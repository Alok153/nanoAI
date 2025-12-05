plugins { id("com.vjaykrsna.nanoai.android.library") }

android {
  namespace = "com.vjaykrsna.nanoai.core.data"

  defaultConfig {
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    buildConfigField("String", "MODEL_CATALOG_BASE_URL", "\"https://api.nanoai.app/\"")
    buildConfigField("String", "HUGGING_FACE_BASE_URL", "\"https://huggingface.co/\"")
  }
}

ksp { arg("room.generateKotlin", "true") }

dependencies {
  implementation(project(":core:common"))
  implementation(project(":core:domain"))

  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.datetime)

  implementation(libs.retrofit)
  implementation(libs.retrofit.kotlin.serialization)
  implementation(libs.kotlinx.serialization.core)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.okhttp)

  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.androidx.metrics.performance)

  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.datastore)
  implementation(libs.androidx.datastore.core)
  implementation(libs.androidx.datastore.preferences.core)

  implementation(libs.androidx.security.crypto)

  implementation(libs.hilt.android)
  implementation(libs.hilt.work)
  ksp(libs.hilt.compiler)
  ksp(libs.androidx.hilt.compiler)

  testImplementation(kotlin("test"))
  testImplementation(kotlin("test-junit5"))
  testImplementation(kotlin("reflect"))
  testImplementation(project(":core:testing"))
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter.params)
  testImplementation(libs.mockk)
  testImplementation(libs.truth)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.androidx.room.testing)
  testImplementation(libs.androidx.work.testing)
  testImplementation(libs.mockwebserver)
  testImplementation(libs.turbine)
  testImplementation(libs.robolectric)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.androidx.test.core)
  testRuntimeOnly(libs.junit.jupiter.engine)
  testRuntimeOnly(libs.junit.platform.launcher)

  androidTestImplementation(kotlin("test"))
  androidTestImplementation(libs.junit.jupiter.api)
  androidTestImplementation(libs.junit.jupiter.params)
  androidTestImplementation(libs.junit.vintage.engine)
  androidTestImplementation(libs.mockk.android)
  androidTestImplementation(libs.truth)
  androidTestImplementation(libs.androidx.room.testing)
  androidTestImplementation(libs.androidx.work.testing)
  androidTestImplementation(libs.mockwebserver)
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.hilt.android.testing)
  kspAndroidTest(libs.hilt.compiler)
}
