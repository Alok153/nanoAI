package com.vjaykrsna.nanoai // Package name fixed

import android.util.Log
import com.nexa.sdk.LlmWrapper
import com.nexa.sdk.LlmCreateInput
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
                // Using the advanced 'cpu_gpu' plugin as per Nexa docs
                val llmWrapper = LlmWrapper.Builder()
                    .setLlmCreateInput(
                        LlmCreateInput(
                            modelPath = modelPath,
                            pluginId = "cpu_gpu", // Hardware acceleration
                            config = ModelConfig().apply {
                                device_id = "dev0" // Powers GGML Hexagon backend
                            }
                        )
                    )
                    .build()

                val fullPrompt = "$systemPrompt\n\nUser: \"$userInput\"\nTag:"
                val genConfig = GenerationConfig(maxTokens = 10, temperature = 0.1f)
                
                // Flow collect logic fixed for latest SDK
                llmWrapper.generateStreamFlow(fullPrompt, genConfig).collect { token ->
                    generatedTag += token
                }
            } catch (e: Exception) {
                Log.e(TAG, "Critical Manager Error: ${e.message}")
                generatedTag = "<CHAT>" // Fallback
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
