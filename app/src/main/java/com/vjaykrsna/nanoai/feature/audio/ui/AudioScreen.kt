package com.vjaykrsna.nanoai.feature.audio.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vjaykrsna.nanoai.feature.audio.presentation.AudioSessionState
import com.vjaykrsna.nanoai.feature.audio.presentation.AudioViewModel
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoSpacing
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.flow.collectLatest

/**
 * Audio/voice calling screen with waveform visualization and call controls.
 *
 * Follows Material 3 design and nanoAI calling interface patterns.
 */
@Composable
fun AudioScreen(modifier: Modifier = Modifier, viewModel: AudioViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(Unit) {
    viewModel.errorEvents.collectLatest { error ->
      val message =
        when (error) {
          is com.vjaykrsna.nanoai.feature.audio.presentation.AudioError.SessionError ->
            error.message
          is com.vjaykrsna.nanoai.feature.audio.presentation.AudioError.PermissionError ->
            error.message
        }
      snackbarHostState.showSnackbar(message)
    }
  }

  Box(
    modifier =
      modifier.fillMaxSize().semantics { contentDescription = "Audio voice calling screen" }
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(NanoSpacing.lg),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Header
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NanoSpacing.sm),
      ) {
        Text(
          text = "Voice Assistant",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text =
            when (uiState.sessionState) {
              AudioSessionState.IDLE -> "Ready to start"
              AudioSessionState.ACTIVE -> "Listening…"
              AudioSessionState.ENDED -> "Session ended"
            },
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // Central waveform visualization
      WaveformVisualization(
        waveformData = uiState.waveformData,
        isActive = uiState.sessionState == AudioSessionState.ACTIVE,
        modifier = Modifier.size(280.dp).testTag("audio_waveform"),
      )

      // Control buttons
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NanoSpacing.lg),
      ) {
        // Mute and Speaker controls
        if (uiState.sessionState == AudioSessionState.ACTIVE) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(NanoSpacing.xl),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            // Mute button
            FloatingActionButton(
              onClick = viewModel::toggleMute,
              containerColor =
                if (uiState.isMuted) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.secondaryContainer,
              modifier = Modifier.size(64.dp).testTag("audio_mute_button"),
            ) {
              Icon(
                imageVector = if (uiState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = if (uiState.isMuted) "Unmute" else "Mute",
                modifier = Modifier.size(32.dp),
              )
            }

            // Speaker button
            FloatingActionButton(
              onClick = viewModel::toggleSpeaker,
              containerColor =
                if (uiState.isSpeakerOn) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.secondaryContainer,
              modifier = Modifier.size(64.dp).testTag("audio_speaker_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = if (uiState.isSpeakerOn) "Speaker on" else "Speaker off",
                modifier = Modifier.size(32.dp),
                tint =
                  if (uiState.isSpeakerOn) MaterialTheme.colorScheme.onPrimaryContainer
                  else MaterialTheme.colorScheme.onSecondaryContainer,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(NanoSpacing.md))

        // Start/End call button
        when (uiState.sessionState) {
          AudioSessionState.IDLE -> {
            Button(
              onClick = viewModel::startAudioSession,
              modifier = Modifier.fillMaxWidth().testTag("audio_start_button"),
            ) {
              Icon(Icons.Default.Call, contentDescription = null)
              Spacer(modifier = Modifier.size(NanoSpacing.sm))
              Text("Start Voice Session")
            }
          }
          AudioSessionState.ACTIVE -> {
            Button(
              onClick = viewModel::endSession,
              colors =
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
              modifier = Modifier.fillMaxWidth().testTag("audio_end_button"),
            ) {
              Icon(Icons.Default.CallEnd, contentDescription = null)
              Spacer(modifier = Modifier.size(NanoSpacing.sm))
              Text("End Session")
            }
          }
          AudioSessionState.ENDED -> {
            Button(
              onClick = viewModel::startAudioSession,
              modifier = Modifier.fillMaxWidth().testTag("audio_restart_button"),
            ) {
              Icon(Icons.Default.Call, contentDescription = null)
              Spacer(modifier = Modifier.size(NanoSpacing.sm))
              Text("Start New Session")
            }
          }
        }
      }
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier.align(Alignment.BottomCenter).padding(NanoSpacing.md),
    )
  }
}

/**
 * Animated waveform visualization in a circular pattern.
 *
 * Displays real-time audio waveform data around a central circle.
 */
@Composable
private fun WaveformVisualization(
  waveformData: List<Float>,
  isActive: Boolean,
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "waveform_rotation")
  val rotationAngle by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 10000, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "rotation",
    )

  val primaryColor = MaterialTheme.colorScheme.primary
  val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

  Surface(modifier = modifier, shape = CircleShape, color = surfaceVariant) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      // Outer waveform ring
      if (isActive && waveformData.isNotEmpty()) {
        Canvas(modifier = Modifier.fillMaxSize().padding(NanoSpacing.md)) {
          val centerX = size.width / 2
          val centerY = size.height / 2
          val baseRadius = size.minDimension / 3
          val angleStep = 360f / waveformData.size

          waveformData.forEachIndexed { index, amplitude ->
            val angle = Math.toRadians((index * angleStep + rotationAngle).toDouble())
            val radiusOffset = amplitude * (size.minDimension / 8)
            val startRadius = baseRadius
            val endRadius = baseRadius + radiusOffset

            val startX = centerX + startRadius * cos(angle).toFloat()
            val startY = centerY + startRadius * sin(angle).toFloat()
            val endX = centerX + endRadius * cos(angle).toFloat()
            val endY = centerY + endRadius * sin(angle).toFloat()

            drawLine(
              color = primaryColor,
              start = Offset(startX, startY),
              end = Offset(endX, endY),
              strokeWidth = 3f,
            )
          }
        }
      }

      // Central circle
      Surface(
        modifier = Modifier.size(120.dp),
        shape = CircleShape,
        color = if (isActive) primaryColor else MaterialTheme.colorScheme.surface,
      ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint =
              if (isActive) MaterialTheme.colorScheme.onPrimary
              else MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }
  }
}
