package com.vjaykrsna.nanoai.feature.audio.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoSpacing
import kotlin.math.cos
import kotlin.math.sin

private const val WAVEFORM_ROTATION_DEGREES = 360f
private const val WAVEFORM_ROTATION_DURATION_MS = 10_000
private const val WAVEFORM_BASE_RADIUS_DIVISOR = 3f
private const val WAVEFORM_RADIUS_OFFSET_DIVISOR = 8f
private const val WAVEFORM_STROKE_WIDTH = 3f

/**
 * Animated waveform visualization in a circular pattern.
 *
 * Displays real-time audio waveform data around a central circle.
 */
@Composable
internal fun WaveformVisualization(
  waveformData: List<Float>,
  isActive: Boolean,
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "waveform_rotation")
  val rotationAngle by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = WAVEFORM_ROTATION_DEGREES,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = WAVEFORM_ROTATION_DURATION_MS, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "rotation",
    )

  val primaryColor = MaterialTheme.colorScheme.primary
  val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

  Surface(modifier = modifier, shape = CircleShape, color = surfaceVariant) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      if (isActive && waveformData.isNotEmpty()) {
        WaveformRing(
          waveformData = waveformData,
          rotationAngle = rotationAngle,
          color = primaryColor,
        )
      }

      WaveformCenterCircle(isActive = isActive, color = primaryColor)
    }
  }
}

@Composable
private fun WaveformRing(waveformData: List<Float>, rotationAngle: Float, color: Color) {
  Canvas(modifier = Modifier.fillMaxSize().padding(NanoSpacing.md)) {
    drawWaveformRing(waveformData = waveformData, rotationAngle = rotationAngle, color = color)
  }
}

private fun DrawScope.drawWaveformRing(
  waveformData: List<Float>,
  rotationAngle: Float,
  color: Color,
) {
  val centerX = size.width / 2
  val centerY = size.height / 2
  val baseRadius = size.minDimension / WAVEFORM_BASE_RADIUS_DIVISOR
  val angleStep = WAVEFORM_ROTATION_DEGREES / waveformData.size

  waveformData.forEachIndexed { index, amplitude ->
    val angle = Math.toRadians((index * angleStep + rotationAngle).toDouble())
    val radiusOffset = amplitude * (size.minDimension / WAVEFORM_RADIUS_OFFSET_DIVISOR)
    val startRadius = baseRadius
    val endRadius = baseRadius + radiusOffset

    val startX = centerX + startRadius * cos(angle).toFloat()
    val startY = centerY + startRadius * sin(angle).toFloat()
    val endX = centerX + endRadius * cos(angle).toFloat()
    val endY = centerY + endRadius * sin(angle).toFloat()

    drawLine(
      color = color,
      start = Offset(startX, startY),
      end = Offset(endX, endY),
      strokeWidth = WAVEFORM_STROKE_WIDTH,
    )
  }
}

@Composable
private fun WaveformCenterCircle(isActive: Boolean, color: Color) {
  Surface(
    modifier = Modifier.size(120.dp),
    shape = CircleShape,
    color = if (isActive) color else MaterialTheme.colorScheme.surface,
  ) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Icon(
        imageVector = Icons.Default.Mic,
        contentDescription = null,
        modifier = Modifier.size(48.dp),
        tint =
          if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}
