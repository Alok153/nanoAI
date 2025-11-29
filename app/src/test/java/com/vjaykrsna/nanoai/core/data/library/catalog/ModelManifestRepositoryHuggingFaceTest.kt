package com.vjaykrsna.nanoai.core.data.library.catalog

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.library.ModelManifestUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ModelManifestRepositoryHuggingFaceTest {
  private val modelManifestUseCase: ModelManifestUseCase = mockk(relaxed = true)
  private val clock: Clock = FixedClock

  private lateinit var repository: ModelManifestRepository

  @BeforeEach
  fun setUp() {
    repository = ModelManifestRepositoryImpl(modelManifestUseCase = modelManifestUseCase)
  }

  @Test
  fun `refreshManifest resolves Hugging Face locator`() = runTest {
    val modelId = "gemma"
    val expectedManifest =
      DownloadManifest(
        modelId = modelId,
        version = "main",
        checksumSha256 = "${"d".repeat(64)}",
        sizeBytes = 1024L,
        downloadUrl =
          "https://huggingface.co/google/gemma-2-2b-it/resolve/main/LiteRT/model.bin?download=1",
        signature = null,
        publicKeyUrl = null,
        expiresAt = null,
        fetchedAt = clock.now(),
      )
    coEvery { modelManifestUseCase.refreshManifest(modelId, "main") } returns
      NanoAIResult.success(expectedManifest)

    val result = repository.refreshManifest(modelId, "main")

    assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
    val manifest = (result as NanoAIResult.Success).value
    assertThat(manifest).isEqualTo(expectedManifest)
  }

  @Test
  fun `reportVerification is no-op for Hugging Face models`() = runTest {
    val modelId = "gemma"
    coEvery {
      modelManifestUseCase.reportVerification(
        modelId = modelId,
        version = "main",
        checksumSha256 = "${"a".repeat(64)}",
        status = VerificationOutcome.SUCCESS,
        failureReason = null,
      )
    } returns NanoAIResult.success(Unit)

    val result =
      repository.reportVerification(
        modelId = modelId,
        version = "main",
        checksumSha256 = "${"a".repeat(64)}",
        status = VerificationOutcome.SUCCESS,
        failureReason = null,
      )

    assertThat(result).isInstanceOf(NanoAIResult.Success::class.java)
  }

  private object FixedClock : Clock {
    private val fixed = Instant.parse("2024-01-01T00:00:00Z")

    override fun now(): Instant = fixed
  }
}
