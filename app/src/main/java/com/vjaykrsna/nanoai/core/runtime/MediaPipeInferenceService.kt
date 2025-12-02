package com.vjaykrsna.nanoai.core.runtime

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.genai.llminference.AudioModelOptions
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.chat.PromptImage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

private const val DEFAULT_MAX_TOKENS = 1024
private const val DEFAULT_TEMPERATURE = 0.7f

@Singleton
class MediaPipeInferenceService
@Inject
constructor(@ApplicationContext private val context: Context) : InferenceService {
  private val modelDirectory: File by lazy { File(context.filesDir, "models") }

  private var llmInference: LlmInference? = null
  private var lastModelId: String? = null

  override suspend fun isModelReady(modelId: String): Boolean =
    withContext(Dispatchers.IO) { modelFile(modelId).exists() }

  @OptIn(ExperimentalTime::class)
  override suspend fun generate(
    request: LocalGenerationRequest
  ): NanoAIResult<LocalGenerationResult> =
    withContext(Dispatchers.Default) {
      try {
        val file = modelFile(request.modelId)
        if (!file.exists()) {
          return@withContext NanoAIResult.recoverable(
            message = "Local model ${request.modelId} is not installed",
            telemetryId = LOCAL_MODEL_MISSING,
            cause = FileNotFoundException("Model ${request.modelId} missing"),
            context = mapOf("modelId" to request.modelId),
          )
        }

        initializeInference(request)
        val prompt = buildPrompt(request)
        val resultText = generateWithSession(request, prompt)

        NanoAIResult.success(
          LocalGenerationResult(
            text = resultText.first,
            latencyMs = resultText.second,
            metadata =
              mapOf(
                "modelId" to request.modelId,
                "temperature" to request.temperature,
                "topP" to request.topP,
              ),
          )
        )
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (runtime: RuntimeException) {
        if (runtime.message?.contains("MediaPipe") != true) throw runtime
        runtime.toMediaPipeFailure(request.modelId)
      } catch (error: Throwable) {
        error.toMediaPipeFailure(request.modelId)
      }
    }

  private fun buildPrompt(request: LocalGenerationRequest): String {
    return if (request.systemPrompt.isNullOrBlank()) {
      request.prompt
    } else {
      "${request.systemPrompt}\n\n${request.prompt}"
    }
  }

  private suspend fun generateWithSession(
    request: LocalGenerationRequest,
    prompt: String,
  ): Pair<String, Long> {
    val graphOptions =
      GraphOptions.builder()
        .setEnableVisionModality(request.image != null)
        .setEnableAudioModality(request.audio != null)
        .build()

    val sessionOptions =
      LlmInferenceSession.LlmInferenceSessionOptions.builder()
        .setTemperature(request.temperature ?: DEFAULT_TEMPERATURE)
        .setGraphOptions(graphOptions)
        .build()

    val session = LlmInferenceSession.createFromOptions(llmInference!!, sessionOptions)
    return try {
      var resultText = ""
      val latency = measureTime {
        resultText = suspendCancellableCoroutine { continuation ->
          val resultBuilder = StringBuilder()
          session.addQueryChunk(prompt)
          request.image?.toBitmap()?.let { bitmap ->
            session.addImage(BitmapImageBuilder(bitmap).build())
          }
          request.audio?.let { session.addAudio(it) }

          session.generateResponseAsync { partialResult, done ->
            resultBuilder.append(partialResult)
            if (done) {
              if (continuation.isActive) {
                continuation.resume(resultBuilder.toString())
              }
            }
          }
        }
      }

      Pair(resultText, latency.inWholeMilliseconds)
    } finally {
      session.close()
    }
  }

  private fun modelFile(modelId: String): File = File(modelDirectory, "$modelId.bin")

  private fun initializeInference(request: LocalGenerationRequest) {
    if (llmInference == null || lastModelId != request.modelId) {
      llmInference?.close()

      try {
        val optionsBuilder =
          LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelFile(request.modelId).absolutePath)
            .setMaxTokens(request.maxOutputTokens ?: DEFAULT_MAX_TOKENS)

        if (request.audio != null) {
          optionsBuilder.setAudioModelOptions(AudioModelOptions.builder().build())
        }

        if (request.image != null) {
          optionsBuilder.setMaxNumImages(1)
        }

        llmInference = LlmInference.createFromOptions(context, optionsBuilder.build())
        lastModelId = request.modelId
      } catch (e: UnsatisfiedLinkError) {
        throw IllegalStateException("MediaPipe native libraries not available", e)
      } catch (e: NoClassDefFoundError) {
        throw IllegalStateException("MediaPipe classes not available", e)
      }
    }
  }

  private companion object {
    private const val LOCAL_MODEL_MISSING = "MEDIAPIPE_MODEL_MISSING"
    private const val MEDIAPIPE_INFERENCE_ERROR = "MEDIAPIPE_INFERENCE_ERROR"
    private const val GENERIC_MEDIAPIPE_ERROR = "Local MediaPipe inference failed"
  }

  private fun PromptImage.toBitmap(): Bitmap? {
    return runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
  }

  private fun Throwable.toMediaPipeFailure(modelId: String): NanoAIResult<LocalGenerationResult> =
    NanoAIResult.recoverable(
      message = message ?: GENERIC_MEDIAPIPE_ERROR,
      telemetryId = MEDIAPIPE_INFERENCE_ERROR,
      cause = this,
      context = mapOf("modelId" to modelId),
    )
}
