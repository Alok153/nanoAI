// Common exclusion patterns for Jacoco reports to avoid repetition.
val jacocoExclusionPatterns =
  listOf(
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test*.*",
    // Hilt weaves this receiver at runtime causing JaCoCo class-data mismatches.
    "**/DownloadNotificationReceiver.*",
    // Room generates DAO implementations at compile time; their stubs inflate coverage noise.
    "**/core/data/db/daos/**",
    "**/core/data/library/**/dao/**",
    "**/core/data/image/db/**",
    // Generated Room implementation and adapter classes provide no executable business logic.
    "**/*Dao_Impl*",
    "**/*Database_Impl*",
    // DI modules are configuration, not business logic.
    "**/core/di/**",
    // NotificationHelper is tested but coverage not captured due to module separation.
    "**/core/common/NotificationHelper*",
    // VerifyCoverageThresholdsTask is a Gradle task with private nested classes that use
    // exitProcess(), making it impractical to unit test. This is build-time code not Android
    // runtime.
    "**/core/coverage/tasks/VerifyCoverageThresholdsTask*",
  )

apply(from = rootProject.file("config/testing/coverage/coverage-thresholds.gradle.kts"))

// Helper function to generate class directories for Jacoco, reducing repetition.
fun jacocoClassDirectories(variant: String) =
  files(
    fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/$variant").get()) {
      exclude(jacocoExclusionPatterns)
    },
    fileTree(layout.buildDirectory.dir("intermediates/javac/$variant/classes").get()) {
      exclude(jacocoExclusionPatterns)
    },
  )

val isCiEnvironment = System.getenv("CI")?.equals("true", ignoreCase = true) == true
val usePhysicalDeviceProperty =
  (project.findProperty("nanoai.usePhysicalDevice") as? String)?.toBoolean() ?: false
val skipInstrumentation =
  (project.findProperty("nanoai.skipInstrumentation") as? String)?.toBoolean() ?: false
val useManagedDeviceForInstrumentation =
  !skipInstrumentation && (isCiEnvironment || !usePhysicalDeviceProperty)
// Pixel 6 API 34 managed virtual device ships an x86_64-only system image starting in
// Android 14. Lock the ABI so CI and local runs share the same emulator bits.
val managedDeviceName = "pixel6Api34"
val managedDeviceTaskName = "${managedDeviceName}DebugAndroidTest"

// Collect common coverage inputs for JVM + instrumentation runs.
val coverageExecutionData =
  files(
    layout.buildDirectory.file("jacoco/testDebugUnitTest.exec"),
    layout.buildDirectory.file(
      "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"
    ),
    fileTree(layout.buildDirectory.dir("outputs/code_coverage").get().asFile) {
      include("**/*.ec")
    },
    fileTree(layout.buildDirectory.dir("outputs/managed_device_code_coverage").get().asFile) {
      include("**/*.ec")
    },
  )

val coverageClassDirectories = jacocoClassDirectories("debug")
val coverageSourceDirectories = files("src/main/java", "src/main/kotlin")

tasks.register<JacocoReport>("jacocoFullReport") {
  group = "verification"
  description = "Generates a merged coverage report for unit and instrumentation tests."
  notCompatibleWithConfigurationCache("Uses file trees and execution data files")

  dependsOn(tasks.named("testDebugUnitTest"))
  if (!skipInstrumentation) {
    dependsOn(tasks.named("connectedDebugAndroidTest"))
  }

  classDirectories.setFrom(coverageClassDirectories)
  additionalClassDirs.setFrom(coverageClassDirectories)
  sourceDirectories.setFrom(coverageSourceDirectories)
  executionData.setFrom(coverageExecutionData)

  reports {
    xml.required.set(true)
    xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/full/jacocoFullReport.xml"))
    html.required.set(true)
    html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/full/html"))
    csv.required.set(false)
  }
}

tasks.register("ciManagedDeviceDebugAndroidTest") {
  group = "verification"
  description = "Runs instrumentation tests on the CI managed Pixel 6 API 34 virtual device."

  dependsOn(tasks.named(managedDeviceTaskName))
  onlyIf { !skipInstrumentation }
  doFirst { logger.lifecycle("Executing managed-device instrumentation on $managedDeviceName") }
}

if (useManagedDeviceForInstrumentation) {
  tasks
    .matching { it.name == "connectedDebugAndroidTest" }
    .configureEach {
      dependsOn(managedDeviceTaskName)
      // Skip the device-provider task when using the managed virtual device to avoid requiring
      // a physical emulator in headless environments.
      onlyIf { false }
    }
}

tasks.register<JacocoReport>("jacocoUnitReport") {
  group = "verification"
  description = "Generates a coverage report for unit tests only."
  notCompatibleWithConfigurationCache("Uses file trees and execution data files")

  dependsOn(tasks.named("testDebugUnitTest"))

  val coverageClassDirectoriesUnit = jacocoClassDirectories("debugUnitTest")
  classDirectories.setFrom(coverageClassDirectoriesUnit)
  additionalClassDirs.setFrom(coverageClassDirectoriesUnit)
  sourceDirectories.setFrom(coverageSourceDirectories)
  executionData.setFrom(
    layout.buildDirectory.file(
      "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"
    )
  )

  reports {
    xml.required.set(true)
    xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/unit/jacocoUnitReport.xml"))
    html.required.set(true)
    html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/unit/html"))
    csv.required.set(false)
  }
}

val coverageReportXml = layout.buildDirectory.file("reports/jacoco/full/jacocoFullReport.xml")
val layerMapFile =
  rootProject.layout.projectDirectory.file("config/testing/coverage/layer-map.json")
val coverageGateMarkdown = layout.buildDirectory.file("coverage/thresholds.md")
val coverageGateJson = layout.buildDirectory.file("coverage/thresholds.json")

tasks.register<JavaExec>("verifyCoverageThresholds") {
  group = "verification"
  description = "Verifies merged coverage meets minimum layer thresholds."
  notCompatibleWithConfigurationCache("Uses Gradle script objects")

  dependsOn(tasks.named("compileDebugKotlin"))
  dependsOn(tasks.named("jacocoFullReport"))

  inputs.file(coverageReportXml)
  inputs.file(layerMapFile)
  outputs.file(coverageGateMarkdown)
  outputs.file(coverageGateJson)

  mainClass.set("com.vjaykrsna.nanoai.core.coverage.tasks.VerifyCoverageThresholdsTask")

  classpath(
    layout.buildDirectory.dir("tmp/kotlin-classes/debug"),
    layout.buildDirectory.dir("intermediates/javac/debug/classes"),
    // Limit to class artifacts to avoid AGP/Gradle variant ambiguity when resolving
    configurations.named("debugRuntimeClasspath").map { configuration ->
      configuration.incoming
        .artifactView {
          attributes {
            attribute(
              org.gradle.api.attributes.Attribute.of("artifactType", String::class.java),
              "android-classes-jar",
            )
          }
        }
        .files
    },
  )

  doFirst {
    coverageGateMarkdown.get().asFile.parentFile.mkdirs()
    coverageGateJson.get().asFile.parentFile.mkdirs()
  }

  args(
    "--report-xml",
    coverageReportXml.get().asFile.absolutePath,
    "--layer-map",
    layerMapFile.asFile.absolutePath,
    "--markdown",
    coverageGateMarkdown.get().asFile.absolutePath,
    "--build-id",
    "jacocoFullReport",
    "--json",
    coverageGateJson.get().asFile.absolutePath,
  )
}

tasks.named("check") {
  dependsOn(tasks.named("jacocoFullReport"))
  dependsOn(tasks.named("verifyCoverageThresholds"))
}

tasks.register<Exec>("coverageMergeArtifacts") {
  group = "verification"
  description = "Runs helper script to trigger merged coverage generation."

  commandLine(
    "bash",
    "${rootDir}/scripts/coverage/merge-coverage.sh",
    layout.buildDirectory.dir("reports/jacoco/full").get().asFile.absolutePath,
  )
}

val coverageSummaryMarkdown = layout.buildDirectory.file("coverage/summary.md")
val coverageSummaryJson = layout.buildDirectory.file("coverage/summary.json")
val legacyCoverageMarkdown = layout.buildDirectory.file("reports/jacoco/full/summary.md")

tasks.register<Exec>("coverageMarkdownSummary") {
  group = "verification"
  description = "Generates markdown coverage summary from merged JaCoCo XML report."
  notCompatibleWithConfigurationCache("Runs external python script that inspects project layout")

  dependsOn(tasks.named("jacocoFullReport"))

  val xmlReport = layout.buildDirectory.file("reports/jacoco/full/jacocoFullReport.xml")
  val layerMap = rootProject.layout.projectDirectory.file("config/testing/coverage/layer-map.json")

  inputs.file(xmlReport)
  inputs.file(layerMap)
  outputs.file(coverageSummaryMarkdown)
  outputs.file(coverageSummaryJson)
  outputs.file(legacyCoverageMarkdown)

  doFirst { coverageSummaryMarkdown.get().asFile.parentFile.mkdirs() }

  commandLine(
    "python3",
    "${rootDir}/scripts/coverage/generate-summary.py",
    xmlReport.get().asFile.absolutePath,
    coverageSummaryMarkdown.get().asFile.absolutePath,
    "--json-output",
    coverageSummaryJson.get().asFile.absolutePath,
    "--layer-map",
    layerMap.asFile.absolutePath,
  )

  doLast {
    legacyCoverageMarkdown.get().asFile.parentFile.mkdirs()
    coverageSummaryMarkdown
      .get()
      .asFile
      .copyTo(target = legacyCoverageMarkdown.get().asFile, overwrite = true)
  }
}
