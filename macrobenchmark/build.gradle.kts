plugins { id("com.vjaykrsna.nanoai.android.testing") }

android {
  namespace = "com.vjaykrsna.nanoai.macrobenchmark"

  targetProjectPath = ":app"
  experimentalProperties["android.experimental.self-instrumenting"] = true
}

androidComponents { beforeVariants(selector().all()) { it.enable = it.buildType == "benchmark" } }
