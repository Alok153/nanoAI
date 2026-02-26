package com.vjaykrsna.nanoai // Aapke package ka naam (build.gradle.kts ke according)

import android.util.Log
import com.nexa.sdk.LlmWrapper
import com.nexa.sdk.models.LlmCreateInput
import com.nexa.sdk.models.ModelConfig
import com.nexa.sdk.models.GenerationConfig
import kotlinx.coroutines.runBlocking

class ManagerAIEngine {

    private val TAG = "ManagerAI"
    
    // Naya Prompt (with Hindi/English Audio routing)
    private val systemPrompt = """
        You are the core Router for a multi-modal AI Android App. 
        Analyze the user's prompt and categorize it into exactly ONE of these categories. 
        Reply ONLY with the exact tag, nothing else:
        
        [TAGS]:
        <CODE> - If the user wants software, programming, or app logic.
        <IMAGE> - If the user wants to generate a picture or Stable Diffusion.
        <AUDIO_EN> - If the user specifically asks to generate English speech/audio.
        <AUDIO_HI> - If the user specifically asks to generate Hindi speech/audio.
        <CHAT> - If the user just wants to talk or needs text info.
    """.trimIndent()

    fun routeUserRequest(userInput: String, modelPath: String): String {
        Log.d(TAG, "Loading Manager AI (CPU)...")
        var generatedTag = ""

        // Nexa SDK Flow use karta hai, isliye humein runBlocking chahiye
        runBlocking {
            val llmWrapperResult = LlmWrapper.builder()
                .llmCreateInput(
                    LlmCreateInput(
                        model_name = "manager",
                        model_path = modelPath,
                        plugin_id = "cpu", 
                        config = ModelConfig()
                    )
                )
                .build()

            llmWrapperResult.onSuccess { managerLlm ->
                val fullPrompt = "$systemPrompt\n\nUser Prompt: \"$userInput\"\nTag:"
                val genConfig = GenerationConfig(max_tokens = 10, temperature = 0.1f)
                
                Log.d(TAG, "Manager is thinking...")
                
                // FIXED: Using .collect instead of 'for' loop
                managerLlm.generateStreamFlow(fullPrompt, genConfig).collect { token ->
                    generatedTag += token
                }
                
            }.onFailure { error ->
                Log.e(TAG, "Manager AI failed to load: ${error.message}")
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
