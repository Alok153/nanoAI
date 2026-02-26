package com.vjaykrsna.nanoai

import android.util.Log
import com.nexa.sdk.LlmWrapper
import com.nexa.sdk.LlmInput
import com.nexa.sdk.ModelConfig
import com.nexa.sdk.GenerationConfig
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.collect

class ManagerAIEngine {

    private val TAG = "offlineAiHub_Manager"
    
    private val systemPrompt = """
        You are the core Router for offlineAiHub. 
        Analyze the user's prompt and categorize it. 
        Reply ONLY with the exact tag:
        
        [TAGS]:
        <CODE> - Programming/Logic.
        <IMAGE> - Generating pictures.
        <AUDIO_EN> - English TTS.
        <AUDIO_HI> - Hindi TTS (Piper/Sherpa).
        <CHAT> - General talk.
    """.trimIndent()

    fun routeUserRequest(userInput: String, modelPath: String): String {
        Log.d(TAG, "Initializing Manager on Hexagon NPU...")
        var generatedTag = ""

        runBlocking {
            try {
                // Fixed: Builder syntax for Nexa SDK Android
                val llmWrapper = LlmWrapper.Builder()
                    .setLlmInput(
                        LlmInput(
                            modelPath = modelPath,
                            pluginId = "cpu_gpu", // Hardware acceleration for Snapdragon 8 Elite
                            config = ModelConfig().apply {
                                deviceId = "dev0" // GGML Hexagon backend as per docs
                            }
                        )
                    )
                    .build()

                val fullPrompt = "$systemPrompt\n\nUser: \"$userInput\"\nTag:"
                // Fixed: MaxTokens parameter name
                val genConfig = GenerationConfig(maxTokens = 10, temperature = 0.1f)
                
                Log.d(TAG, "Manager is thinking...")
                
                // Fixed: Explicit type for 'token' to resolve inference error
                llmWrapper.generateStreamFlow(fullPrompt, genConfig).collect { token: String ->
                    generatedTag += token
                }
            } catch (e: Exception) {
                Log.e(TAG, "Critical Manager Error: ${e.message}")
                generatedTag = "<CHAT>" // Safe fallback
            }
        }

        return when {
            generatedTag.contains("<CODE>") -> "<CODE>"
            generatedTag.contains("<IMAGE>") -> "<IMAGE>"
            generatedTag.contains("<AUDIO_EN>") -> "<AUDIO_EN>"
            generatedTag.contains("<AUDIO_HI>") -> "<AUDIO_HI>"
            else -> "<CHAT>"
        }
    }
}
