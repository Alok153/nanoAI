import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.withType
import org.gradle.process.CommandLineArgumentProvider

val coverageMetadataFile =
    rootProject.layout.projectDirectory.file("config/testing/coverage/coverage-metadata.json")

tasks.withType<JavaExec>()
    .matching { it.name == "verifyCoverageThresholds" }
    .configureEach {
        inputs.file(coverageMetadataFile)
        argumentProviders.add(
            CommandLineArgumentProvider {
                listOf("--coverage-metadata", coverageMetadataFile.asFile.absolutePath)
            }
        )
    }
