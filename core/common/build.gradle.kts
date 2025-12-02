plugins { id("com.vjaykrsna.nanoai.kotlin.library") }

dependencies {
  implementation(libs.javaxInjectLib)

  testImplementation(kotlin("test"))
}
