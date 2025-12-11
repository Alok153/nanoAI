package com.vjaykrsna.nanoai.core.coverage.data

import android.content.Context
import android.content.res.AssetManager
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.ByteArrayInputStream
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [CoverageDashboardRepositoryImpl].
 *
 * Tests loading and parsing of coverage dashboard snapshots from assets.
 */
class CoverageDashboardRepositoryImplTest {

  private lateinit var context: Context
  private lateinit var assetManager: AssetManager
  private lateinit var repository: CoverageDashboardRepositoryImpl

  @BeforeEach
  fun setUp() {
    context = mockk()
    assetManager = mockk()
    every { context.assets } returns assetManager
    repository = CoverageDashboardRepositoryImpl(context)
  }

  @Nested
  inner class SuccessfulLoading {

    @Test
    fun `loads valid dashboard payload`() = runTest {
      val validJson =
        """
        {
          "buildId": "test-build-123",
          "generatedAt": "2024-01-15T10:30:00Z",
          "layers": [
            {"layer": "VIEW_MODEL", "coverage": 78.5, "threshold": 75.0},
            {"layer": "UI", "coverage": 68.2, "threshold": 65.0},
            {"layer": "DATA", "coverage": 72.1, "threshold": 70.0}
          ]
        }
        """
          .trimIndent()

      every { assetManager.open("coverage/dashboard.json") } returns
        ByteArrayInputStream(validJson.toByteArray())

      val result = repository.loadSnapshot()

      assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
      val success = result as NanoAIResult.Success<CoverageDashboardPayload>
      assertThat(success.value.buildId).isEqualTo("test-build-123")
      assertThat(success.value.generatedAt).isEqualTo("2024-01-15T10:30:00Z")
      assertThat(success.value.layers).hasSize(3)
    }

    @Test
    fun `parses layer data correctly`() = runTest {
      val validJson =
        """
        {
          "buildId": "build-456",
          "generatedAt": "2024-02-20T15:00:00Z",
          "layers": [
            {"layer": "VIEW_MODEL", "coverage": 80.0, "threshold": 75.0}
          ]
        }
        """
          .trimIndent()

      every { assetManager.open("coverage/dashboard.json") } returns
        ByteArrayInputStream(validJson.toByteArray())

      val result = repository.loadSnapshot()

      assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
      val success = result as NanoAIResult.Success<CoverageDashboardPayload>
      val layer = success.value.layers.first()
      assertThat(layer.layer).isEqualTo("VIEW_MODEL")
      assertThat(layer.coverage).isEqualTo(80.0)
      assertThat(layer.threshold).isEqualTo(75.0)
    }

    @Test
    fun `handles payload with trend data`() = runTest {
      val jsonWithTrend =
        """
        {
          "buildId": "trend-build",
          "generatedAt": "2024-03-10T08:00:00Z",
          "layers": [],
          "trend": {
            "VIEW_MODEL": 2.5,
            "UI": -1.0,
            "DATA": 0.0
          }
        }
        """
          .trimIndent()

      every { assetManager.open("coverage/dashboard.json") } returns
        ByteArrayInputStream(jsonWithTrend.toByteArray())

      val result = repository.loadSnapshot()

      assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
      val success = result as NanoAIResult.Success<CoverageDashboardPayload>
      assertThat(success.value.trend).containsEntry("VIEW_MODEL", 2.5)
      assertThat(success.value.trend).containsEntry("UI", -1.0)
      assertThat(success.value.trend).containsEntry("DATA", 0.0)
    }

    @Test
    fun `handles payload with risk items`() = runTest {
      val jsonWithRisks =
        """
        {
          "buildId": "risk-build",
          "generatedAt": "2024-04-05T12:00:00Z",
          "layers": [],
          "risks": [
            {
              "riskId": "RISK-001",
              "title": "Low coverage in chat module",
              "severity": "HIGH",
              "status": "OPEN"
            },
            {
              "riskId": "RISK-002",
              "title": "Missing edge case tests",
              "severity": "MEDIUM",
              "status": "MITIGATED"
            }
          ]
        }
        """
          .trimIndent()

      every { assetManager.open("coverage/dashboard.json") } returns
        ByteArrayInputStream(jsonWithRisks.toByteArray())

      val result = repository.loadSnapshot()

      assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
      val success = result as NanoAIResult.Success<CoverageDashboardPayload>
      assertThat(success.value.risks).hasSize(2)
      assertThat(success.value.risks[0].riskId).isEqualTo("RISK-001")
      assertThat(success.value.risks[0].severity).isEqualTo("HIGH")
      assertThat(success.value.risks[1].status).isEqualTo("MITIGATED")
    }

    @Test
    fun `handles empty layers array`() = runTest {
      val emptyLayersJson =
        """
        {
          "buildId": "empty-layers",
          "generatedAt": "2024-05-01T00:00:00Z",
          "layers": []
        }
        """
          .trimIndent()

      every { assetManager.open("coverage/dashboard.json") } returns
        ByteArrayInputStream(emptyLayersJson.toByteArray())

      val result = repository.loadSnapshot()

      assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
      val success = result as NanoAIResult.Success<CoverageDashboardPayload>
      assertThat(success.value.layers).isEmpty()
    }

    @Test
    fun `ignores unknown fields in JSON`() = runTest {
      val jsonWithExtraFields =
        """
        {
          "buildId": "extra-fields",
          "generatedAt": "2024-06-15T09:30:00Z",
          "layers": [],
          "unknownField": "should be ignored",
          "anotherUnknown": 123
        }
        """
          .trimIndent()

      every { assetManager.open("coverage/dashboard.json") } returns
        ByteArrayInputStream(jsonWithExtraFields.toByteArray())

      val result = repository.loadSnapshot()

      assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
      val success = result as NanoAIResult.Success<CoverageDashboardPayload>
      assertThat(success.value.buildId).isEqualTo("extra-fields")
    }
  }

  @Nested
  inner class ErrorHandling {

    @Test
    fun `returns error when asset file not found`() = runTest {
      every { assetManager.open("coverage/dashboard.json") } throws IOException("Asset not found")

      val result = repository.loadSnapshot()

      assertThat(result).isInstanceOf(NanoAIResult.RecoverableError::class.java)
      val error = result as NanoAIResult.RecoverableError
      assertThat(error.message).contains("Unable to read coverage dashboard snapshot")
    }

    @Test
    fun `returns error for malformed JSON`() = runTest {
      val malformedJson = "{ not valid json"

      every { assetManager.open("coverage/dashboard.json") } returns
        ByteArrayInputStream(malformedJson.toByteArray())

      val result = repository.loadSnapshot()

      assertThat(result).isInstanceOf(NanoAIResult.RecoverableError::class.java)
      val error = result as NanoAIResult.RecoverableError
      assertThat(error.message).contains("Coverage dashboard payload invalid")
    }

    @Test
    fun `returns error for missing required fields`() = runTest {
      // Missing buildId and generatedAt
      val missingFieldsJson =
        """
        {
          "layers": []
        }
        """
          .trimIndent()

      every { assetManager.open("coverage/dashboard.json") } returns
        ByteArrayInputStream(missingFieldsJson.toByteArray())

      val result = repository.loadSnapshot()

      assertThat(result).isInstanceOf(NanoAIResult.RecoverableError::class.java)
    }

    @Test
    fun `returns error for wrong type in JSON`() = runTest {
      // coverage should be a number, not a string
      val wrongTypeJson =
        """
        {
          "buildId": "wrong-type",
          "generatedAt": "2024-07-01T00:00:00Z",
          "layers": [
            {"layer": "VIEW_MODEL", "coverage": "not-a-number", "threshold": 75.0}
          ]
        }
        """
          .trimIndent()

      every { assetManager.open("coverage/dashboard.json") } returns
        ByteArrayInputStream(wrongTypeJson.toByteArray())

      val result = repository.loadSnapshot()

      assertThat(result).isInstanceOf(NanoAIResult.RecoverableError::class.java)
    }

    @Test
    fun `returns error for empty JSON`() = runTest {
      every { assetManager.open("coverage/dashboard.json") } returns
        ByteArrayInputStream("{}".toByteArray())

      val result = repository.loadSnapshot()

      // Should fail because buildId and generatedAt are required
      assertThat(result).isInstanceOf(NanoAIResult.RecoverableError::class.java)
    }

    @Test
    fun `includes path context in error`() = runTest {
      every { assetManager.open("coverage/dashboard.json") } throws IOException("File not found")

      val result = repository.loadSnapshot()

      assertThat(result).isInstanceOf(NanoAIResult.RecoverableError::class.java)
      val error = result as NanoAIResult.RecoverableError
      assertThat(error.context).containsEntry("path", "coverage/dashboard.json")
    }
  }

  @Nested
  inner class AssetAccess {

    @Test
    fun `opens correct asset path`() = runTest {
      val validJson =
        """
        {
          "buildId": "access-test",
          "generatedAt": "2024-08-01T00:00:00Z",
          "layers": []
        }
        """
          .trimIndent()

      every { assetManager.open("coverage/dashboard.json") } returns
        ByteArrayInputStream(validJson.toByteArray())

      repository.loadSnapshot()

      verify { assetManager.open("coverage/dashboard.json") }
    }
  }
}
