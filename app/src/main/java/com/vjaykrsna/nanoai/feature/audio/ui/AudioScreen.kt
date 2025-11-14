package com.vjaykrsna.nanoai.feature.audio.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vjaykrsna.nanoai.feature.audio.presentation.AudioSessionState
import com.vjaykrsna.nanoai.feature.audio.presentation.AudioUiState
import com.vjaykrsna.nanoai.feature.audio.presentation.AudioViewModel
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoSpacing
import kotlinx.coroutines.flow.collectLatest

/**
 * Audio/voice calling screen with waveform visualization and call controls.
 *
 * Follows Material 3 design and nanoAI calling interface patterns.
 */
@Composable
fun AudioScreen(modifier: Modifier = Modifier, viewModel: AudioViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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

  AudioScreenContent(
    onStart = viewModel::startAudioSession,
    onEnd = viewModel::endSession,
    onToggleMute = viewModel::toggleMute,
    onToggleSpeaker = viewModel::toggleSpeaker,
    snackbarHostState = snackbarHostState,
    uiState = uiState,
    modifier = modifier,
  )
}

@Composable
private fun AudioScreenContent(
  onStart: () -> Unit,
  onEnd: () -> Unit,
  onToggleMute: () -> Unit,
  onToggleSpeaker: () -> Unit,
  snackbarHostState: SnackbarHostState,
  uiState: AudioUiState,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier.fillMaxSize().semantics { contentDescription = "Audio voice calling screen" }
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(NanoSpacing.lg),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      AudioHeader(sessionState = uiState.sessionState)

      WaveformVisualization(
        waveformData = uiState.waveformData,
        isActive = uiState.sessionState == AudioSessionState.ACTIVE,
        modifier = Modifier.size(280.dp).testTag("audio_waveform"),
      )

      AudioControls(
        sessionState = uiState.sessionState,
        isMuted = uiState.isMuted,
        isSpeakerOn = uiState.isSpeakerOn,
        onStart = onStart,
        onEnd = onEnd,
        onToggleMute = onToggleMute,
        onToggleSpeaker = onToggleSpeaker,
      )
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier.align(Alignment.BottomCenter).padding(NanoSpacing.md),
    )
  }
}

@Composable
private fun AudioHeader(sessionState: AudioSessionState) {
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
        when (sessionState) {
          AudioSessionState.IDLE -> "Ready to start"
          AudioSessionState.ACTIVE -> "Listening…"
          AudioSessionState.ENDED -> "Session ended"
        },
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun AudioControls(
  sessionState: AudioSessionState,
  isMuted: Boolean,
  isSpeakerOn: Boolean,
  onStart: () -> Unit,
  onEnd: () -> Unit,
  onToggleMute: () -> Unit,
  onToggleSpeaker: () -> Unit,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(NanoSpacing.lg),
  ) {
    if (sessionState == AudioSessionState.ACTIVE) {
      ActiveSessionControls(
        isMuted = isMuted,
        isSpeakerOn = isSpeakerOn,
        onToggleMute = onToggleMute,
        onToggleSpeaker = onToggleSpeaker,
      )
    }

    Spacer(modifier = Modifier.height(NanoSpacing.md))
    AudioActionButton(sessionState = sessionState, onStart = onStart, onEnd = onEnd)
  }
}

@Composable
private fun ActiveSessionControls(
  isMuted: Boolean,
  isSpeakerOn: Boolean,
  onToggleMute: () -> Unit,
  onToggleSpeaker: () -> Unit,
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(NanoSpacing.xl),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    MuteControlButton(isMuted = isMuted, onToggleMute = onToggleMute)

    SpeakerControlButton(isSpeakerOn = isSpeakerOn, onToggleSpeaker = onToggleSpeaker)
  }
}

@Composable
private fun MuteControlButton(isMuted: Boolean, onToggleMute: () -> Unit) {
  val containerColor =
    if (isMuted) {
      MaterialTheme.colorScheme.error
    } else {
      MaterialTheme.colorScheme.secondaryContainer
    }
  val icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic
  val description = if (isMuted) "Unmute" else "Mute"

  SessionControlFab(
    onClick = onToggleMute,
    containerColor = containerColor,
    icon = icon,
    contentDescription = description,
    modifier = Modifier.size(64.dp).testTag("audio_mute_button"),
  )
}

@Composable
private fun SpeakerControlButton(isSpeakerOn: Boolean, onToggleSpeaker: () -> Unit) {
  val containerColor =
    if (isSpeakerOn) {
      MaterialTheme.colorScheme.primaryContainer
    } else {
      MaterialTheme.colorScheme.secondaryContainer
    }
  val description = if (isSpeakerOn) "Speaker on" else "Speaker off"
  val tint =
    if (isSpeakerOn) {
      MaterialTheme.colorScheme.onPrimaryContainer
    } else {
      MaterialTheme.colorScheme.onSecondaryContainer
    }

  SessionControlFab(
    onClick = onToggleSpeaker,
    containerColor = containerColor,
    icon = Icons.AutoMirrored.Filled.VolumeUp,
    contentDescription = description,
    modifier = Modifier.size(64.dp).testTag("audio_speaker_button"),
    iconTint = tint,
  )
}

@Composable
private fun SessionControlFab(
  onClick: () -> Unit,
  containerColor: Color,
  icon: ImageVector,
  contentDescription: String,
  modifier: Modifier = Modifier,
  iconTint: Color? = null,
) {
  FloatingActionButton(onClick = onClick, containerColor = containerColor, modifier = modifier) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      modifier = Modifier.size(32.dp),
      tint = iconTint ?: LocalContentColor.current,
    )
  }
}

@Composable
private fun AudioActionButton(
  sessionState: AudioSessionState,
  onStart: () -> Unit,
  onEnd: () -> Unit,
) {
  when (sessionState) {
    AudioSessionState.IDLE -> {
      PrimaryAudioButton(
        onClick = onStart,
        testTag = "audio_start_button",
        icon = Icons.Default.Call,
        label = "Start Voice Session",
      )
    }
    AudioSessionState.ACTIVE -> {
      PrimaryAudioButton(
        onClick = onEnd,
        testTag = "audio_end_button",
        icon = Icons.Default.CallEnd,
        label = "End Session",
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
      )
    }
    AudioSessionState.ENDED -> {
      PrimaryAudioButton(
        onClick = onStart,
        testTag = "audio_restart_button",
        icon = Icons.Default.Call,
        label = "Start New Session",
      )
    }
  }
}

@Composable
private fun PrimaryAudioButton(
  onClick: () -> Unit,
  testTag: String,
  icon: ImageVector,
  label: String,
  colors: ButtonColors = ButtonDefaults.buttonColors(),
) {
  Button(onClick = onClick, modifier = Modifier.fillMaxWidth().testTag(testTag), colors = colors) {
    Icon(icon, contentDescription = null)
    Spacer(modifier = Modifier.size(NanoSpacing.sm))
    Text(label)
  }
}
