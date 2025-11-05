import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

val screenshotOutputDir =
    layout.projectDirectory.dir("src/test/screenshots").asFile.absolutePath

plugins.withId("com.android.application") {
    configureRoborazziTasks()
}
plugins.withId("com.android.library") {
    configureRoborazziTasks()
}

fun Project.configureRoborazziTasks() {
    tasks.withType<Test>()
        .matching { it.name == "testDebugUnitTest" }
        .configureEach {
            systemProperty("roborazzi.output-dir", screenshotOutputDir)
            systemProperties.putIfAbsent("roborazzi.record", "false")
        }

    val debugUnitTest = tasks.namedOrNull("testDebugUnitTest", Test::class.java) ?: return

    tasks.register<Test>("roboScreenshotDebug") {
        group = "verification"
        description = "Records Roborazzi screenshot baselines for debug JVM tests."

        val debugTestClasses = debugUnitTest.map { it.testClassesDirs }
        val debugClasspath = debugUnitTest.map { it.classpath }

        testClassesDirs = this@configureRoborazziTasks.objects
            .fileCollection()
            .from(debugTestClasses)
        classpath = this@configureRoborazziTasks.objects
            .fileCollection()
            .from(debugClasspath)
        binaryResultsDirectory.set(layout.buildDirectory.dir("test-results/roboScreenshotDebug"))
        reports.junitXml.required.set(false)
        reports.html.required.set(false)
        systemProperty("roborazzi.record", "true")
        systemProperty("roborazzi.output-dir", screenshotOutputDir)
        shouldRunAfter(debugUnitTest)
    }
}

private fun <T : Task> TaskContainer.namedOrNull(name: String, type: Class<T>): TaskProvider<T>? =
    try {
        named(name, type)
    } catch (_: UnknownTaskException) {
        null
    }
