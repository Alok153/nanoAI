package com.vjaykrsna.nanoai.core.runtime

import android.content.Context
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

/**
 * MediaPipe-backed implementation of [LocalModelRuntime].
 *
 * The current implementation provides a lightweight shim that verifies the model payload exists
 * and returns a synthesized response. Integration with MediaPipe LiteRT graph execution will be
 * added once the converted model artifacts are available (see TODO in code).
 */
@Singleton
class MediaPipeLocalModelRuntime
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : LocalModelRuntime {
        private val modelDirectory: File by lazy { File(context.filesDir, "models") }

        override suspend fun isModelReady(modelId: String): Boolean =
            withContext(Dispatchers.IO) {
                modelFile(modelId).exists()
            }

        override suspend fun hasReadyModel(models: List<ModelPackage>): Boolean {
            if (models.isEmpty()) return false
            return models.any { model ->
                model.providerType.name != "CLOUD_API" && isModelReady(model.modelId)
            }
        }

        @OptIn(ExperimentalTime::class)
        override suspend fun generate(request: LocalGenerationRequest): Result<LocalGenerationResult> =
            withContext(Dispatchers.Default) {
                val file = modelFile(request.modelId)
                if (!file.exists()) {
                    return@withContext Result.failure(
                        FileNotFoundException("Local model ${request.modelId} is not installed"),
                    )
                }

                // TODO: Replace synthesis with actual MediaPipe LiteRT inference pipeline.
                val timedValue =
                    measureTimedValue {
                        synthesizeResponse(request)
                    }

                Result.success(
                    LocalGenerationResult(
                        text = timedValue.value,
                        latencyMs = timedValue.duration.inWholeMilliseconds,
                        metadata =
                            mapOf(
                                "modelId" to request.modelId,
                                "temperature" to request.temperature,
                                "topP" to request.topP,
                            ),
                    ),
                )
            }

        private fun modelFile(modelId: String): File = File(modelDirectory, "$modelId.bin")

        private fun synthesizeResponse(request: LocalGenerationRequest): String {
            val systemContext = request.systemPrompt?.takeIf { it.isNotBlank() }?.let { "$it\n\n" } ?: ""
            val cappedPrompt = request.prompt.trim().ifBlank { "No prompt provided." }
            val hint =
                buildString {
                    request.temperature?.let { append("(temp=${String.format("%.2f", it)}) ") }
                    request.topP?.let { append("(topP=${String.format("%.2f", it)}) ") }
                }.trim()

            val header = "[Local:${request.modelId}]"
            return listOf(header, hint, systemContext + cappedPrompt)
                .filter { it.isNotBlank() }
                .joinToString(separator = " ")
                .trim()
        }
    }
