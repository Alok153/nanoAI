plugins { id("com.vjaykrsna.nanoai.kotlin.library") }

dependencies {
  implementation(libs.detekt.api)
  implementation(libs.detekt.rules)

  testImplementation(kotlin("test"))
  testImplementation(libs.detekt.test)
}
