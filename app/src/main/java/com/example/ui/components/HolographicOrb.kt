package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LiveConnectionState
import com.example.data.model.MyraaMood
import kotlin.math.cos
import kotlin.math.sin

/**
 * MYRAA Holographic Avatar Orb.
 * Features:
 * - Dynamic mood glow integration (color switches seamlessly to mood's aura)
 * - Multi-ring rotation & pulsing orbital nodes
 * - Center mood emoji & active state indicator
 * - Real-time audio waveform reaction to amplitude
 */
@Composable
fun HolographicOrb(
  state: LiveConnectionState,
  amplitude: Float,
  mood: MyraaMood,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.94f,
    targetValue = 1.06f,
    animationSpec = infiniteRepeatable(
      animation = tween(2400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_scale"
  )

  val rotationAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(14000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "rotation"
  )

  // Primary and secondary aura colors governed by active mood
  val primaryColor = when (state) {
    LiveConnectionState.ERROR -> Color(0xFFFF4D6D)
    LiveConnectionState.DISCONNECTED -> Color(0xFF4A4458)
    else -> mood.auraColor
  }

  val secondaryColor = when (state) {
    LiveConnectionState.ERROR -> Color(0xFFC9184A)
    LiveConnectionState.DISCONNECTED -> Color(0xFF2B2638)
    else -> mood.secondaryAuraColor
  }

  // Dynamic scale factoring voice amplitude
  val dynamicScale = (pulseScale + (amplitude * 0.30f)).coerceIn(0.88f, 1.35f)

  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      modifier = Modifier
        .size(240.dp)
        .testTag("myraa_avatar_orb")
        .clickable { onClick() },
      contentAlignment = Alignment.Center
    ) {
      // Background canvas for animated orbital rings and particle aura
      Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = (size.minDimension / 2f) * 0.82f

        // Outer glow aura
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              primaryColor.copy(alpha = 0.40f + amplitude * 0.35f),
              secondaryColor.copy(alpha = 0.15f),
              Color.Transparent
            ),
            center = center,
            radius = radius * 1.35f
          ),
          center = center,
          radius = radius * 1.35f
        )

        // Outer holographic ring
        drawCircle(
          color = secondaryColor.copy(alpha = 0.5f),
          center = center,
          radius = radius * dynamicScale,
          style = Stroke(width = 2.dp.toPx())
        )

        // Middle rotating ring with orbital markers
        drawCircle(
          color = primaryColor.copy(alpha = 0.7f),
          center = center,
          radius = radius * 0.88f * dynamicScale,
          style = Stroke(width = 1.5.dp.toPx())
        )

        // Orbital nodes
        val rads = Math.toRadians(rotationAngle.toDouble())
        val nodeX = center.x + (radius * 0.88f * dynamicScale) * cos(rads).toFloat()
        val nodeY = center.y + (radius * 0.88f * dynamicScale) * sin(rads).toFloat()
        drawCircle(
          color = Color.White,
          center = Offset(nodeX, nodeY),
          radius = 4.dp.toPx()
        )

        // Opposite orbital node
        val oppX = center.x - (radius * 0.88f * dynamicScale) * cos(rads).toFloat()
        val oppY = center.y - (radius * 0.88f * dynamicScale) * sin(rads).toFloat()
        drawCircle(
          color = primaryColor,
          center = Offset(oppX, oppY),
          radius = 3.dp.toPx()
        )

        // Inner glowing core
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              Color.White.copy(alpha = 0.85f),
              primaryColor,
              secondaryColor,
              Color(0xFF0B0A12)
            ),
            center = center,
            radius = radius * 0.72f * dynamicScale
          ),
          center = center,
          radius = radius * 0.72f * dynamicScale
        )
      }

      // Center Core Icon & Mood Indicator
      Box(
        modifier = Modifier
          .size(92.dp)
          .clip(CircleShape)
          .background(Color(0xFF0B0A12).copy(alpha = 0.82f))
          .border(2.dp, primaryColor.copy(alpha = 0.85f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        when (state) {
          LiveConnectionState.SPEAKING -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = mood.emoji,
                fontSize = 28.sp
              )
              Text(
                text = "SPEAKING",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                fontFamily = FontFamily.Monospace
              )
            }
          }
          LiveConnectionState.LISTENING -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Listening",
                tint = primaryColor,
                modifier = Modifier.size(34.dp)
              )
              Text(
                text = mood.emoji,
                fontSize = 12.sp
              )
            }
          }
          LiveConnectionState.CONNECTED -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = mood.emoji,
                fontSize = 32.sp
              )
              Text(
                text = "MYRAA",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                fontFamily = FontFamily.Monospace
              )
            }
          }
          LiveConnectionState.ERROR -> {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = "Error",
              tint = primaryColor,
              modifier = Modifier.size(36.dp)
            )
          }
          else -> {
            Icon(
              imageVector = Icons.Default.PowerSettingsNew,
              contentDescription = "Tap to Connect",
              tint = primaryColor,
              modifier = Modifier.size(36.dp)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Real-time Sound Wave Equalizer Bars
    AudioWaveformVisualizer(
      isActive = state == LiveConnectionState.LISTENING || state == LiveConnectionState.SPEAKING,
      amplitude = amplitude,
      activeColor = primaryColor
    )
  }
}

@Composable
fun AudioWaveformVisualizer(
  isActive: Boolean,
  amplitude: Float,
  activeColor: Color,
  modifier: Modifier = Modifier
) {
  val barCount = 21

  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(28.dp)
      .padding(horizontal = 40.dp),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically
  ) {
    for (i in 0 until barCount) {
      val distFromCenter = Math.abs(i - barCount / 2).toFloat() / (barCount / 2)
      val heightFactor = (1.0f - distFromCenter * 0.65f)
      val barHeight = if (isActive) {
        (6.dp + (32.dp * amplitude * heightFactor)).coerceIn(4.dp, 28.dp)
      } else {
        4.dp
      }

      Box(
        modifier = Modifier
          .size(width = 3.dp, height = barHeight)
          .clip(CircleShape)
          .background(
            if (isActive) activeColor.copy(alpha = 0.6f + (amplitude * 0.4f))
            else Color(0xFF2C2442)
          )
      )
    }
  }
}
