plugins { id("com.vjaykrsna.nanoai.kotlin.library") }

dependencies {
  api(project(":core:common"))
  api(project(":core:domain"))
  api(libs.junit.jupiter.api)

  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.test)
  implementation(libs.kotlinx.datetime)
  implementation(libs.truth)
  implementation(libs.javaxInjectLib)

  testImplementation(kotlin("test"))
}
