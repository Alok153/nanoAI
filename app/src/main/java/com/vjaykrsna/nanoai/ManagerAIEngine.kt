package com.vjaykrsna.nanoai

import android.util.Log

class ManagerAIEngine {

    private val TAG = "offlineAiHub_Manager"

    // Temporary Mock Router (No Nexa SDK dependencies to guarantee a GREEN build)
    fun routeUserRequest(userInput: String, modelPath: String): String {
        Log.d(TAG, "Manager analyzing input without NPU for now...")
        
        val input = userInput.lowercase()

        val generatedTag = when {
            input.contains("code") || input.contains("program") -> "<CODE>"
            input.contains("image") || input.contains("photo") || input.contains("picture") -> "<IMAGE>"
            input.contains("english") && (input.contains("audio") || input.contains("speak")) -> "<AUDIO_EN>"
            input.contains("audio") || input.contains("speak") || input.contains("hindi") -> "<AUDIO_HI>"
            else -> "<CHAT>"
        }

        Log.d(TAG, "Manager AI Decision: $generatedTag")
        return generatedTag
    }
}
