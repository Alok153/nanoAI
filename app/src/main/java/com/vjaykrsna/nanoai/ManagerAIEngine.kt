package com.vjaykrsna.nanoai // Aapka confirmed package name

import android.util.Log
import com.nexa.sdk.LlmWrapper
import com.nexa.sdk.LlmCreateInput
import com.nexa.sdk.ModelConfig
import com.nexa.sdk.GenerationConfig
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.collect

class ManagerAIEngine {

    private val TAG = "ManagerAI"
    
    private val systemPrompt = """
        You are the core Router for a multi-modal AI Android App. 
        Analyze the user's prompt and categorize it into exactly ONE of these categories. 
        Reply ONLY with the exact tag, nothing else:
        
        [TAGS]:
        <CODE> - Programming or logic.
        <IMAGE> - Generating pictures/art.
        <AUDIO_EN> - English speech generation.
        <AUDIO_HI> - Hindi speech generation.
        <CHAT> - General conversation.
    """.trimIndent()

    fun routeUserRequest(userInput: String, modelPath: String): String {
        Log.d(TAG, "Loading Manager AI (CPU)...")
        var generatedTag = ""

        runBlocking {
            // Updated syntax for LlmWrapper Builder
            val llmWrapper = LlmWrapper.Builder()
                .setLlmCreateInput(
                    LlmCreateInput(
                        modelPath = modelPath,
                        pluginId = "cpu", // Routing manager runs on CPU
                        config = ModelConfig()
                    )
                )
                .build()

            val fullPrompt = "$systemPrompt\n\nUser Prompt: \"$userInput\"\nTag:"
            val genConfig = GenerationConfig(maxTokens = 10, temperature = 0.1f)
            
            Log.d(TAG, "Manager is thinking...")
            
            // Fixed stream collection
            llmWrapper.generateStreamFlow(fullPrompt, genConfig).collect { token ->
                generatedTag += token
            }
        }

        Log.d(TAG, "Manager AI Decision: $generatedTag")

        return when {
            generatedTag.contains("<CODE>", ignoreCase = true) -> "<CODE>"
            generatedTag.contains("<IMAGE>", ignoreCase = true) -> "<IMAGE>"
            generatedTag.contains("<AUDIO_EN>", ignoreCase = true) -> "<AUDIO_EN>"
            generatedTag.contains("<AUDIO_HI>", ignoreCase = true) -> "<AUDIO_HI>"
            else -> "<CHAT>"
        }
    }
}
