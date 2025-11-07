plugins { id("com.vjaykrsna.nanoai.android.testing") }

android {
  namespace = "com.vjaykrsna.nanoai.macrobenchmark"

  targetProjectPath = ":app"
  experimentalProperties["android.experimental.self-instrumenting"] = true
}

androidComponents { beforeVariants(selector().all()) { it.enable = it.buildType == "benchmark" } }

val macrobenchmarkResultsDir =
  layout.buildDirectory.dir("outputs/connected_android_test_additional_output/connected")
val macrobenchmarkReportDir = layout.buildDirectory.dir("reports/macrobenchmark")
val baselineFile = rootProject.file("config/testing/tooling/macrobenchmark-baselines.json")
val analyzeScript = rootProject.file("scripts/benchmark/analyze-results.sh")

tasks.register<Exec>("verifyMacrobenchmarkPerformance") {
  group = "verification"
  description = "Runs macrobenchmarks and validates performance budgets via analyze-results.sh."
  dependsOn("connectedCheck")
  inputs.file(baselineFile)
  inputs.dir(macrobenchmarkResultsDir)
  outputs.file(macrobenchmarkReportDir.map { it.file("summary.md") })
  outputs.file(macrobenchmarkReportDir.map { it.file("summary.json") })

  doFirst {
    if (!analyzeScript.canExecute()) {
      analyzeScript.setExecutable(true)
    }
  }

  val resultsPath = macrobenchmarkResultsDir.get().asFile.absolutePath
  val reportPath = macrobenchmarkReportDir.get().file("summary.md").asFile.absolutePath
  val jsonPath = macrobenchmarkReportDir.get().file("summary.json").asFile.absolutePath

  commandLine(
    analyzeScript.absolutePath,
    "--results",
    resultsPath,
    "--baseline",
    baselineFile.absolutePath,
    "--report",
    reportPath,
    "--json",
    jsonPath,
  )
}
