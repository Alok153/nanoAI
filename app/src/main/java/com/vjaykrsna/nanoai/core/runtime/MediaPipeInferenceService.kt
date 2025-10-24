package com.vjaykrsna.nanoai.core.runtime

import android.content.Context
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.genai.llminference.AudioModelOptions
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton
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
  override suspend fun generate(request: LocalGenerationRequest): Result<LocalGenerationResult> {
    return withContext(Dispatchers.Default) {
      val file = modelFile(request.modelId)
      if (!file.exists()) {
        return@withContext Result.failure(
          FileNotFoundException("Local model ${request.modelId} is not installed")
        )
      }

      initializeInference(request)
      val prompt = buildPrompt(request)
      val resultText = generateWithSession(request, prompt)

      Result.success(
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
          request.image?.let { session.addImage(BitmapImageBuilder(it).build()) }
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
    }
  }
}
