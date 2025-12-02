package com.vjaykrsna.nanoai.core.runtime

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.chat.PromptImage
import com.vjaykrsna.nanoai.testing.MainDispatcherRule
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Robolectric tests for [MediaPipeInferenceService].
 *
 * Uses JUnit 4 with [MainDispatcherRule] because Robolectric requires `@RunWith`.
 */
@RunWith(RobolectricTestRunner::class)
class MediaPipeInferenceServiceTest {

  @Suppress("DEPRECATION") // MainDispatcherRule required for JUnit 4 / Robolectric
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  private lateinit var context: Context
  private lateinit var service: MediaPipeInferenceService

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    service = MediaPipeInferenceService(context)
  }

  private fun createTestModelFile(modelId: String): File {
    val modelFile = File(context.filesDir, "models/$modelId.bin")
    modelFile.parentFile?.mkdirs()
    modelFile.createNewFile()
    return modelFile
  }

  @Test
  fun `generate should return success for text only request`() = runTest {
    // Given
    val modelId = "test_model"
    createTestModelFile(modelId)
    val request = LocalGenerationRequest(modelId, "test prompt")

    // When
    val result = service.generate(request)

    // Then
    assertResult(result)
  }

  @Test
  fun `generate should return success for image and text request`() = runTest {
    // Given
    val modelId = "test_model"
    createTestModelFile(modelId)
    val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    val request = LocalGenerationRequest(modelId, "test prompt", image = bitmap.toPromptImage())

    // When
    val result = service.generate(request)

    // Then
    assertResult(result)
  }

  @Test
  fun `generate should return success for audio and text request`() = runTest {
    // Given
    val modelId = "test_model"
    createTestModelFile(modelId)
    val audioData = ByteArray(1)
    val request = LocalGenerationRequest(modelId, "test prompt", audio = audioData)

    // When
    val result = service.generate(request)

    // Then
    assertResult(result)
  }

  private fun assertResult(result: NanoAIResult<LocalGenerationResult>) {
    when (result) {
      is NanoAIResult.Success -> assertThat(result.value.text).isNotEmpty()
      is NanoAIResult.RecoverableError -> assertThat(result.message).contains("MediaPipe")
      is NanoAIResult.FatalError ->
        assertWithMessage("Expected success or recoverable error but was fatal: ${result.message}")
          .fail()
    }
  }

  private fun Bitmap.toPromptImage(): PromptImage {
    val buffer = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.PNG, 100, buffer)
    return PromptImage(
      bytes = buffer.toByteArray(),
      width = width,
      height = height,
      mimeType = "image/png",
    )
  }
}
