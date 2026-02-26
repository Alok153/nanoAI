package com.nexa.demo // Aapke package ka naam (build.gradle.kts ke according)

import ai.nexa.core.LLM // Nexa SDK import
import ai.nexa.core.ModelConfig
import ai.nexa.core.GenerationConfig
import android.util.Log

class ManagerAIEngine {

    private val TAG = "ManagerAI"
    
    // The strict prompt that forces the AI to only output routing tags
    private val systemPrompt = """
        You are the core Router for a multi-modal AI Android App. 
        Analyze the user's prompt and categorize it into exactly ONE of these categories. 
        Reply ONLY with the exact tag, nothing else:
        
        [TAGS]:
        <CODE> - If the user wants software, programming, HTML, Python, debugging, or app logic.
        <IMAGE> - If the user wants to generate a picture, drawing, art, photo, or Stable Diffusion.
        <AUDIO> - If the user specifically asks to generate a voice, speech, or audio file.
        <CHAT> - If the user just wants to talk, ask general questions, or needs text info.
    """.trimIndent()

    /**
     * Yeh function Manager AI (398MB GGUF) ko CPU par load karega, 
     * user input process karega, aur routing tag return karke memory clear kar dega.
     */
    fun routeUserRequest(userInput: String, modelPath: String): String {
        Log.d(TAG, "Loading Manager AI (CPU)...")
        
        // 1. Load Manager AI (ruvltra-claude-code)
        // Note: Hum CPU use kar rahe hain taaki NPU Image/Code ke liye free rahe
        val config = ModelConfig()
        val managerLlm = LLM.from_(
            model = modelPath, // Aapke phone mein 398MB gguf file ka path
            plugin_id = "cpu", 
            config = config
        )

        // 2. Format the prompt
        val fullPrompt = "$systemPrompt\n\nUser Prompt: \"$userInput\"\nTag:"
        
        var generatedTag = ""
        Log.d(TAG, "Manager is thinking...")

        // 3. Generate Output (It should be lightning fast)
        val genConfig = GenerationConfig(max_tokens = 10, temperature = 0.1f)
        for (token in managerLlm.generate_stream(fullPrompt, genConfig)) {
            generatedTag += token
        }

        // 4. Release Memory (CRITICAL: Taaki NPU models crash na ho)
        managerLlm.release() 
        Log.d(TAG, "Manager AI unloaded. Decision: $generatedTag")

        // 5. Clean up the response just in case the AI added extra spaces
        return when {
            generatedTag.contains("<CODE>", ignoreCase = true) -> "<CODE>"
            generatedTag.contains("<IMAGE>", ignoreCase = true) -> "<IMAGE>"
            generatedTag.contains("<AUDIO>", ignoreCase = true) -> "<AUDIO>"
            else -> "<CHAT>"
        }
    }
}
