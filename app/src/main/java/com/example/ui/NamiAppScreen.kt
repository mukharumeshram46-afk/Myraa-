package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.LiveConnectionState
import com.example.data.model.MyraaMode
import com.example.data.model.MyraaMood
import com.example.nativecontrol.MyraaAccessibilityService
import com.example.ui.components.DiagnosticsPanel
import com.example.ui.components.HolographicOrb
import com.example.ui.components.SettingsDialog
import com.example.ui.components.TranscriptView
import com.example.ui.theme.MyraaBgDark
import com.example.ui.theme.MyraaBorder
import com.example.ui.theme.MyraaBorderGlow
import com.example.ui.theme.MyraaMagenta
import com.example.ui.theme.MyraaPink
import com.example.ui.theme.MyraaSurfaceDark
import com.example.ui.theme.MyraaSurfaceVariant
import com.example.ui.theme.MyraaTextPrimary
import com.example.ui.theme.MyraaTextSecondary
import com.example.ui.viewmodel.MyraaViewModel

@Composable
fun MyraaAppScreen(
  viewModel: MyraaViewModel,
  onRequestPermission: () -> Unit
) {
  val context = LocalContext.current
  val connectionState by viewModel.connectionState.collectAsState()
  val diagnostics by viewModel.diagnostics.collectAsState()
  val transcript by viewModel.transcript.collectAsState()
  val amplitude by viewModel.audioAmplitude.collectAsState()
  val currentMode by viewModel.currentMode.collectAsState()
  val currentMood by viewModel.currentMood.collectAsState()
  val moodReason by viewModel.moodStatusReason.collectAsState()
  val wakeStatus by viewModel.wakeStatus.collectAsState()
  val isWakeEngineEnabled by viewModel.isWakeEngineEnabled.collectAsState()
  val isScreenVisionEnabled by viewModel.isScreenVisionEnabled.collectAsState()

  var showSettingsDialog by remember { mutableStateOf(false) }
  var showTranscriptDialog by remember { mutableStateOf(false) }
  var showNativeControlDialog by remember { mutableStateOf(false) }

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .background(MyraaBgDark)
      .testTag("myraa_main_screen"),
    containerColor = MyraaBgDark
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // 1. Top HUD Header
      TopHudHeader(
        currentMode = currentMode,
        currentMood = currentMood,
        onModeToggle = {
          val nextMode = when (currentMode) {
            MyraaMode.STANDARD -> MyraaMode.COMPANION
            MyraaMode.COMPANION -> MyraaMode.DEVELOPER
            MyraaMode.DEVELOPER -> MyraaMode.FOCUS
            MyraaMode.FOCUS -> MyraaMode.STUDY
            MyraaMode.STUDY -> MyraaMode.STANDARD
          }
          viewModel.setMode(nextMode)
        },
        onOpenNativeControls = { showNativeControlDialog = true },
        onOpenSettings = { showSettingsDialog = true },
        onOpenTranscript = { showTranscriptDialog = true }
      )

      Spacer(modifier = Modifier.height(6.dp))

      // Mood & Persona Status Chip
      MoodBadgeChip(
        mood = currentMood,
        statusReason = moodReason,
        onSelectMood = { newMood ->
          viewModel.setMood(newMood, "Piyush changed my mood")
        }
      )

      Spacer(modifier = Modifier.height(8.dp))

      // 2. Holographic Orb Centerpiece with Dynamic Aura
      HolographicOrb(
        state = connectionState,
        amplitude = amplitude,
        mood = currentMood,
        modifier = Modifier.padding(vertical = 10.dp),
        onClick = {
          if (connectionState == LiveConnectionState.DISCONNECTED || connectionState == LiveConnectionState.ERROR) {
            onRequestPermission()
            viewModel.launchLiveSession()
          } else {
            viewModel.disconnectLiveSession()
          }
        }
      )

      // State Title & Subtitle for Piyush
      Text(
        text = when (connectionState) {
          LiveConnectionState.CONNECTED, LiveConnectionState.LISTENING -> "MYRAA READY"
          LiveConnectionState.SPEAKING -> "MYRAA SPEAKING"
          LiveConnectionState.CONNECTING -> "CALLING PIYUSH..."
          LiveConnectionState.RECONNECTING -> "RECONNECTING..."
          LiveConnectionState.ERROR -> "CONNECTION ERROR"
          LiveConnectionState.DISCONNECTED -> "STANDBY FOR PIYUSH"
        },
        fontSize = 19.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 2.sp,
        color = when (connectionState) {
          LiveConnectionState.CONNECTED, LiveConnectionState.LISTENING -> currentMood.auraColor
          LiveConnectionState.SPEAKING -> MyraaMagenta
          LiveConnectionState.CONNECTING, LiveConnectionState.RECONNECTING -> Color(0xFFFFB703)
          LiveConnectionState.ERROR -> Color(0xFFFF4D6D)
          LiveConnectionState.DISCONNECTED -> MyraaTextSecondary
        }
      )

      Text(
        text = when (connectionState) {
          LiveConnectionState.DISCONNECTED -> "Tap below or say \"Hey Myraa\" to start chatting!"
          LiveConnectionState.CONNECTING -> "Connecting with Gemini Live for Piyush..."
          LiveConnectionState.RECONNECTING -> "Reconnecting to live bridge..."
          LiveConnectionState.CONNECTED -> "Myraa connected! Initializing microphone..."
          LiveConnectionState.LISTENING -> "Suno na Piyush, I'm listening... Batao kya help chahiye?"
          LiveConnectionState.SPEAKING -> "Speaking with Piyush..."
          LiveConnectionState.ERROR -> diagnostics.lastError ?: "Connection Error"
        },
        fontSize = 12.sp,
        color = MyraaTextSecondary,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
      )

      Spacer(modifier = Modifier.height(10.dp))

      // 3. Quick Action Buttons
      ControlsSection(
        connectionState = connectionState,
        mood = currentMood,
        onLaunch = {
          onRequestPermission()
          viewModel.launchLiveSession()
        },
        onDisconnect = { viewModel.disconnectLiveSession() },
        onRetry = {
          onRequestPermission()
          viewModel.retryConnection()
        }
      )

      Spacer(modifier = Modifier.height(10.dp))

      // 4. Wake Engine Toggle Bar
      WakeEngineControlBar(
        isEnabled = isWakeEngineEnabled,
        statusText = wakeStatus,
        onToggle = { enabled ->
          if (enabled) onRequestPermission()
          viewModel.toggleWakeEngine(enabled)
        },
        onManualWake = {
          onRequestPermission()
          viewModel.triggerManualWake()
        }
      )

      Spacer(modifier = Modifier.height(10.dp))

      // 5. Diagnostics Panel
      DiagnosticsPanel(
        diagnostics = diagnostics,
        onRetryConnection = {
          onRequestPermission()
          viewModel.retryConnection()
        },
        modifier = Modifier.fillMaxWidth()
      )
    }
  }

  // Settings Dialog for API Key
  if (showSettingsDialog) {
    SettingsDialog(
      currentKey = viewModel.liveSessionManager.getApiKey(),
      onSaveKey = { newKey -> viewModel.setApiKey(newKey) },
      onDismiss = { showSettingsDialog = false }
    )
  }

  // Native Android Control Dialog
  if (showNativeControlDialog) {
    NativeControlModal(
      context = context,
      isScreenVisionEnabled = isScreenVisionEnabled,
      onToggleVision = { viewModel.toggleScreenVision(it) },
      onDismiss = { showNativeControlDialog = false }
    )
  }

  // Transcript Dialog
  if (showTranscriptDialog) {
    Dialog(onDismissRequest = { showTranscriptDialog = false }) {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .height(520.dp)
          .clip(RoundedCornerShape(16.dp))
          .border(1.dp, currentMood.auraColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        color = MyraaSurfaceDark
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "${currentMood.emoji} MYRAA LIVE CHAT",
                color = currentMood.auraColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
            }
            IconButton(onClick = { showTranscriptDialog = false }) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = MyraaTextSecondary
              )
            }
          }
          TranscriptView(
            messages = transcript,
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
          )
        }
      }
    }
  }
}

@Composable
private fun TopHudHeader(
  currentMode: MyraaMode,
  currentMood: MyraaMood,
  onModeToggle: () -> Unit,
  onOpenNativeControls: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenTranscript: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // App Brand
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(10.dp)
          .clip(CircleShape)
          .background(currentMood.auraColor)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "MYRAA",
        color = MyraaPink,
        fontSize = 18.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 2.sp
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "AI COPILOT",
        color = currentMood.auraColor,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
      )
    }

    // Top action icons
    Row(verticalAlignment = Alignment.CenterVertically) {
      // Mode pill button
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .background(Color(currentMode.badgeColorHex).copy(alpha = 0.2f))
          .border(1.dp, Color(currentMode.badgeColorHex).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
          .clickable { onModeToggle() }
          .padding(horizontal = 10.dp, vertical = 5.dp)
          .testTag("mode_toggle_button"),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = currentMode.displayName,
          color = Color(currentMode.badgeColorHex),
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }

      Spacer(modifier = Modifier.width(4.dp))

      // Native Controls Shortcut
      IconButton(
        onClick = onOpenNativeControls,
        modifier = Modifier.testTag("native_controls_button")
      ) {
        Icon(
          imageVector = Icons.Default.Security,
          contentDescription = "Android Native Control",
          tint = currentMood.auraColor
        )
      }

      IconButton(
        onClick = onOpenTranscript,
        modifier = Modifier.testTag("open_transcript_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Chat,
          contentDescription = "Transcript",
          tint = MyraaPink
        )
      }

      IconButton(
        onClick = onOpenSettings,
        modifier = Modifier.testTag("open_settings_button")
      ) {
        Icon(
          imageVector = Icons.Default.Settings,
          contentDescription = "Settings",
          tint = MyraaTextSecondary
        )
      }
    }
  }
}

@Composable
private fun MoodBadgeChip(
  mood: MyraaMood,
  statusReason: String,
  onSelectMood: (MyraaMood) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }

  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(20.dp))
        .background(mood.auraColor.copy(alpha = 0.15f))
        .border(1.dp, mood.auraColor.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
        .clickable { expanded = !expanded }
        .padding(horizontal = 14.dp, vertical = 6.dp)
        .testTag("mood_badge_chip")
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = mood.emoji, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "${mood.label} • $statusReason",
          color = mood.auraColor,
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          fontFamily = FontFamily.Monospace
        )
      }
    }

    // Mood Selector Bar
    AnimatedVisibility(visible = expanded) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 8.dp, start = 16.dp, end = 16.dp)
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        MyraaMood.values().forEach { m ->
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(if (m == mood) m.auraColor else MyraaSurfaceDark)
              .border(1.dp, m.auraColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
              .clickable {
                onSelectMood(m)
                expanded = false
              }
              .padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(text = m.emoji, fontSize = 12.sp)
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = m.label,
                color = if (m == mood) MyraaBgDark else MyraaTextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ControlsSection(
  connectionState: LiveConnectionState,
  mood: MyraaMood,
  onLaunch: () -> Unit,
  onDisconnect: () -> Unit,
  onRetry: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp),
    horizontalArrangement = Arrangement.Center
  ) {
    when (connectionState) {
      LiveConnectionState.DISCONNECTED -> {
        ElevatedButton(
          onClick = onLaunch,
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("launch_live_button"),
          colors = ButtonDefaults.elevatedButtonColors(
            containerColor = mood.auraColor,
            contentColor = MyraaBgDark
          ),
          shape = RoundedCornerShape(14.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Launch",
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "TALK TO MYRAA",
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
          )
        }
      }
      LiveConnectionState.CONNECTING, LiveConnectionState.RECONNECTING -> {
        ElevatedButton(
          onClick = onDisconnect,
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("cancel_connecting_button"),
          colors = ButtonDefaults.elevatedButtonColors(
            containerColor = Color(0xFFFFB703),
            contentColor = MyraaBgDark
          ),
          shape = RoundedCornerShape(14.dp)
        ) {
          Text(
            text = if (connectionState == LiveConnectionState.RECONNECTING) "RECONNECTING... (CANCEL)" else "CONNECTING... (CANCEL)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
      LiveConnectionState.ERROR -> {
        Row(modifier = Modifier.fillMaxWidth()) {
          ElevatedButton(
            onClick = onRetry,
            modifier = Modifier
              .weight(1f)
              .height(52.dp)
              .testTag("retry_button_main"),
            colors = ButtonDefaults.elevatedButtonColors(
              containerColor = Color(0xFFFF4D6D),
              contentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Retry",
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Retry", fontWeight = FontWeight.Bold)
          }
          Spacer(modifier = Modifier.width(8.dp))
          ElevatedButton(
            onClick = onDisconnect,
            modifier = Modifier
              .height(52.dp)
              .testTag("disconnect_error_button"),
            colors = ButtonDefaults.elevatedButtonColors(
              containerColor = MyraaSurfaceVariant,
              contentColor = MyraaTextSecondary
            ),
            shape = RoundedCornerShape(14.dp)
          ) {
            Text("Cancel")
          }
        }
      }
      LiveConnectionState.CONNECTED, LiveConnectionState.LISTENING, LiveConnectionState.SPEAKING -> {
        ElevatedButton(
          onClick = onDisconnect,
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("disconnect_live_button"),
          colors = ButtonDefaults.elevatedButtonColors(
            containerColor = Color(0xFFC9184A),
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(14.dp)
        ) {
          Icon(
            imageVector = Icons.Default.PowerSettingsNew,
            contentDescription = "Disconnect",
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "END SESSION WITH MYRAA",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
        }
      }
    }
  }
}

@Composable
private fun WakeEngineControlBar(
  isEnabled: Boolean,
  statusText: String,
  onToggle: (Boolean) -> Unit,
  onManualWake: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp)
      .clip(RoundedCornerShape(14.dp))
      .background(MyraaSurfaceDark)
      .border(1.dp, MyraaBorder, RoundedCornerShape(14.dp))
      .padding(14.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "VOICE WAKE ENGINE",
            color = MyraaPink,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "(\"Hey Myraa\")",
            color = MyraaTextSecondary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
          )
        }
        Text(
          text = statusText,
          color = if (isEnabled) MyraaTextSecondary else Color(0xFFFF4D6D),
          fontSize = 11.sp,
          fontFamily = FontFamily.Monospace
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        ElevatedButton(
          onClick = onManualWake,
          modifier = Modifier
            .height(34.dp)
            .testTag("wake_test_button"),
          colors = ButtonDefaults.elevatedButtonColors(
            containerColor = MyraaSurfaceVariant,
            contentColor = MyraaPink
          ),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text("Simulate", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.width(10.dp))

        Switch(
          checked = isEnabled,
          onCheckedChange = onToggle,
          modifier = Modifier.testTag("wake_engine_switch"),
          colors = SwitchDefaults.colors(
            checkedThumbColor = MyraaPink,
            checkedTrackColor = MyraaPink.copy(alpha = 0.3f),
            uncheckedThumbColor = Color(0xFF4A4458),
            uncheckedTrackColor = MyraaSurfaceVariant
          )
        )
      }
    }
  }
}

@Composable
private fun NativeControlModal(
  context: android.content.Context,
  isScreenVisionEnabled: Boolean,
  onToggleVision: (Boolean) -> Unit,
  onDismiss: () -> Unit
) {
  val isAccessibilityActive = MyraaAccessibilityService.isServiceEnabled(context)

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .border(1.dp, MyraaPink.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
      color = MyraaSurfaceDark
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "ANDROID NATIVE COPILOT",
            color = MyraaPink,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = MyraaTextSecondary)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Accessibility Service Status
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MyraaSurfaceVariant)
            .padding(12.dp)
        ) {
          Column {
            Text(
              text = "Accessibility Screen & Tap Control",
              color = MyraaTextPrimary,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp
            )
            Text(
              text = if (isAccessibilityActive) "Status: ACTIVE (Full Phone Control)" else "Status: NOT ENABLED",
              color = if (isAccessibilityActive) Color(0xFF06D6A0) else Color(0xFFFF4D6D),
              fontSize = 11.sp,
              fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(6.dp))
            ElevatedButton(
              onClick = { MyraaAccessibilityService.openAccessibilitySettings(context) },
              colors = ButtonDefaults.elevatedButtonColors(
                containerColor = MyraaPink,
                contentColor = MyraaBgDark
              ),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text("Open Accessibility Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Screen Vision Multimodal Toggle
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MyraaSurfaceVariant)
            .padding(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Screen Vision (2-5 FPS)",
                color = MyraaTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
              Text(
                text = "Streams screen frames so Myraa can see what Piyush is doing.",
                color = MyraaTextSecondary,
                fontSize = 10.sp
              )
            }
            Switch(
              checked = isScreenVisionEnabled,
              onCheckedChange = onToggleVision,
              colors = SwitchDefaults.colors(
                checkedThumbColor = MyraaPink,
                checkedTrackColor = MyraaPink.copy(alpha = 0.3f)
              )
            )
          }
        }
      }
    }
  }
}

// Backward alias
@Composable
fun NamiAppScreen(
  viewModel: MyraaViewModel,
  onRequestPermission: () -> Unit
) = MyraaAppScreen(viewModel, onRequestPermission)
