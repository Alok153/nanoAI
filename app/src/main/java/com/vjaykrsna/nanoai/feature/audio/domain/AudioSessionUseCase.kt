package com.vjaykrsna.nanoai.feature.audio.domain

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import kotlinx.coroutines.flow.Flow

/**
 * Use case for managing audio sessions and processing.
 */
interface AudioSessionUseCase {

    /**
     * Starts an audio recording session.
     */
    suspend fun startSession(): NanoAIResult<Unit>

    /**
     * Ends the current audio session.
     */
    suspend fun endSession(): NanoAIResult<Unit>

    /**
     * Toggles microphone mute state.
     */
    suspend fun toggleMute(): NanoAIResult<Boolean>

    /**
     * Toggles speaker output.
     */
    suspend fun toggleSpeaker(): NanoAIResult<Boolean>

    /**
     * Gets real-time waveform data flow.
     */
    fun getWaveformData(): Flow<List<Float>>

    /**
     * Checks if microphone permission is granted.
     */
    suspend fun checkMicrophonePermission(): NanoAIResult<Boolean>
}
