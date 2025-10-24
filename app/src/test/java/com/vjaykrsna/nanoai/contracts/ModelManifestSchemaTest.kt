package com.vjaykrsna.nanoai.contracts

import com.google.common.truth.Truth.assertThat
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.networknt.schema.ValidationMessage
import java.io.File
import org.junit.Before
import org.junit.Test

/**
 * JSON Schema validation test for model-manifest.json. Ensures downloadable model manifests conform
 * to the defined schema.
 *
 * TDD: This test validates the manifest schema BEFORE model downloads are implemented. Expected to
 * FAIL if schema file is missing or malformed.
 */
class ModelManifestSchemaTest {
  private lateinit var schemaFile: File

  @Before
  fun setup() {
    // Schema is in test resources
    val classLoader = javaClass.classLoader
    assertThat(classLoader).isNotNull()
    val schemaUrl = classLoader!!.getResource("contracts/model-manifest.json")
    assertThat(schemaUrl).isNotNull()
    schemaFile = File(schemaUrl!!.file)

    // Verify schema file exists (will fail if not found - expected in TDD)
    assertThat(schemaFile.exists()).isTrue()
  }

  @Test
  fun `valid manifest should pass schema validation`() {
    // Arrange: Create a valid manifest
    val validManifest =
      """
            {
                "model_id": "gemini-2.0-flash-lite",
                "display_name": "Gemini 2.0 Flash Lite",
                "version": "2.0.1",
                "runtime": "MEDIA_PIPE",
                "artifact_url": "https://storage.googleapis.com/nanoai-models/gemini-2.0-flash-lite.bin",
                "size_bytes": 1500000000,
                "checksum": {
                    "algorithm": "SHA256",
                    "value": "abc123def456789012345678901234567890123456789012345678901234abcd"
                },
                "capabilities": ["TEXT_GEN", "CODE_GEN"],
                "min_ram_mb": 2048
            }
            """
        .trimIndent()

    // Act: Validate against schema
    val errors = validateManifest(validManifest)

    // Assert: No validation errors
    assertThat(errors).isEmpty()
  }

  @Test
  fun `manifest with missing required field should fail validation`() {
    // Arrange: Manifest missing 'runtime' field
    val invalidManifest =
      """
            {
                "model_id": "test-model",
                "version": "1.0",
                "artifact_url": "https://example.com/model.bin",
                "size_bytes": 1000000,
                "capabilities": ["TEXT_GEN"]
            }
            """
        .trimIndent()

    // Act: Validate against schema
    val errors = validateManifest(invalidManifest)

    // Assert: Validation should fail for missing 'runtime'
    assertThat(errors).isNotEmpty()
    val errorMessages = errors.map { it.message }
    assertThat(errorMessages.any { it.contains("runtime") }).isTrue()
  }

  @Test
  fun `manifest with invalid runtime enum should fail validation`() {
    // Arrange: Manifest with invalid runtime value
    val invalidManifest =
      """
            {
                "model_id": "test-model",
                "version": "1.0",
                "runtime": "INVALID_RUNTIME",
                "artifact_url": "https://example.com/model.bin",
                "size_bytes": 1000000,
                "capabilities": ["TEXT_GEN"]
            }
            """
        .trimIndent()

    // Act: Validate against schema
    val errors = validateManifest(invalidManifest)

    // Assert: Validation should fail for invalid enum
    assertThat(errors).isNotEmpty()
  }

  @Test
  fun `manifest runtime should accept valid enum values`() {
    // Arrange: Valid runtime enums per schema
    val validRuntimes = listOf("MEDIA_PIPE", "TFLITE", "MLC_LLM", "ONNX_RUNTIME")

    // Act & Assert: Each runtime should pass validation
    validRuntimes.forEach { runtime ->
      val manifest = createMinimalManifest(runtime = runtime)
      val errors = validateManifest(manifest)
      assertThat(errors).isEmpty()
    }
  }

  @Test
  fun `manifest with invalid checksum algorithm should fail validation`() {
    // Arrange: Manifest with invalid checksum algorithm
    val invalidManifest =
      """
            {
                "model_id": "test-model",
                "version": "1.0",
                "runtime": "MEDIA_PIPE",
                "artifact_url": "https://example.com/model.bin",
                "size_bytes": 1000000,
                "checksum": {
                    "algorithm": "MD5",
                    "value": "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"
                },
                "capabilities": ["TEXT_GEN"]
            }
            """
        .trimIndent()

    // Act: Validate against schema
    val errors = validateManifest(invalidManifest)

    // Assert: Validation should fail for invalid algorithm
    assertThat(errors).isNotEmpty()
  }

  @Test
  fun `manifest checksum value should match pattern`() {
    // Arrange: Manifest with invalid checksum value (wrong length/format)
    val invalidManifest =
      """
            {
                "model_id": "test-model",
                "version": "1.0",
                "runtime": "MEDIA_PIPE",
                "artifact_url": "https://example.com/model.bin",
                "size_bytes": 1000000,
                "checksum": {
                    "algorithm": "SHA256",
                    "value": "invalid-checksum-format"
                },
                "capabilities": ["TEXT_GEN"]
            }
            """
        .trimIndent()

    // Act: Validate against schema
    val errors = validateManifest(invalidManifest)

    // Assert: Validation should fail for pattern mismatch
    assertThat(errors).isNotEmpty()
  }

  @Test
  fun `manifest with invalid capability enum should fail validation`() {
    // Arrange: Manifest with invalid capability
    val invalidManifest =
      """
            {
                "model_id": "test-model",
                "version": "1.0",
                "runtime": "MEDIA_PIPE",
                "artifact_url": "https://example.com/model.bin",
                "size_bytes": 1000000,
                "capabilities": ["TEXT_GEN", "INVALID_CAPABILITY"]
            }
            """
        .trimIndent()

    // Act: Validate against schema
    val errors = validateManifest(invalidManifest)

    // Assert: Validation should fail for invalid capability
    assertThat(errors).isNotEmpty()
  }

  @Test
  fun `manifest capabilities should accept valid enum values`() {
    // Arrange: Valid capability enums per schema
    val validCapabilities = listOf("TEXT_GEN", "CODE_GEN", "IMAGE_GEN", "AUDIO_IN", "AUDIO_OUT")

    // Act & Assert: Each capability should pass validation
    validCapabilities.forEach { capability ->
      val manifest = createMinimalManifest(capabilities = listOf(capability))
      val errors = validateManifest(manifest)
      assertThat(errors).isEmpty()
    }
  }

  @Test
  fun `manifest size_bytes should be positive integer`() {
    // Arrange: Manifest with invalid size
    val invalidManifest =
      """
            {
                "model_id": "test-model",
                "version": "1.0",
                "runtime": "MEDIA_PIPE",
                "artifact_url": "https://example.com/model.bin",
                "size_bytes": 0,
                "capabilities": ["TEXT_GEN"]
            }
            """
        .trimIndent()

    // Act: Validate against schema
    val errors = validateManifest(invalidManifest)

    // Assert: Validation should fail for size < 1
    assertThat(errors).isNotEmpty()
  }

  @Test
  fun `manifest min_ram_mb should be at least 1024`() {
    // Arrange: Manifest with too low min_ram_mb
    val invalidManifest =
      """
            {
                "model_id": "test-model",
                "version": "1.0",
                "runtime": "MEDIA_PIPE",
                "artifact_url": "https://example.com/model.bin",
                "size_bytes": 1000000,
                "capabilities": ["TEXT_GEN"],
                "min_ram_mb": 512
            }
            """
        .trimIndent()

    // Act: Validate against schema
    val errors = validateManifest(invalidManifest)

    // Assert: Validation should fail for min_ram_mb < 1024
    assertThat(errors).isNotEmpty()
  }

  @Test
  fun `manifest model_id should match pattern`() {
    // Arrange: Manifest with invalid model_id (uppercase, spaces)
    val invalidManifest =
      """
            {
                "model_id": "Invalid Model ID!",
                "version": "1.0",
                "runtime": "MEDIA_PIPE",
                "artifact_url": "https://example.com/model.bin",
                "size_bytes": 1000000,
                "capabilities": ["TEXT_GEN"]
            }
            """
        .trimIndent()

    // Act: Validate against schema
    val errors = validateManifest(invalidManifest)

    // Assert: Validation should fail for pattern mismatch
    assertThat(errors).isNotEmpty()
  }

  // Helper functions

  private fun validateManifest(manifestJson: String): Set<ValidationMessage> {
    val factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
    val schema = factory.getSchema(schemaFile.toURI())

    val jsonNode = com.fasterxml.jackson.databind.ObjectMapper().readTree(manifestJson)

    return schema.validate(jsonNode)
  }

  private fun createMinimalManifest(
    runtime: String = "MEDIA_PIPE",
    capabilities: List<String> = listOf("TEXT_GEN"),
  ): String {
    val capabilitiesJson = capabilities.joinToString(",") { "\"$it\"" }
    return """
            {
                "model_id": "test-model",
                "version": "1.0.0",
                "runtime": "$runtime",
                "artifact_url": "https://example.com/model.bin",
                "size_bytes": 1000000,
                "capabilities": [$capabilitiesJson]
            }
        """
      .trimIndent()
  }
}
