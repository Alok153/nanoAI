plugins { id("com.vjaykrsna.nanoai.kotlin.library") }

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.detekt.api)
  implementation(libs.detekt.rules)

  // Opt-in markers for experimental APIs analyzed by detekt
  compileOnly(libs.kotlinx.coroutines.core)
  compileOnly(libs.androidx.compose.material3)

  testImplementation(kotlin("test"))
  testImplementation(libs.detekt.test)
}
