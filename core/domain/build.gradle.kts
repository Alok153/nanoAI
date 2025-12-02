plugins { id("com.vjaykrsna.nanoai.kotlin.library") }

dependencies {
  api(project(":core:common"))

  api(libs.kotlinx.coroutines.core)
  api(libs.kotlinx.datetime)
  api(libs.kotlinx.serialization.json)

  implementation(libs.javaxInjectLib)

  testImplementation(kotlin("test"))
  testImplementation(libs.mockk)
}
