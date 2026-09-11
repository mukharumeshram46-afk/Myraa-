package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DiagnosticsInfo
import com.example.data.model.LiveConnectionState
import com.example.ui.theme.MyraaBorder
import com.example.ui.theme.MyraaPink
import com.example.ui.theme.MyraaSurfaceDark
import com.example.ui.theme.MyraaTextPrimary
import com.example.ui.theme.MyraaTextSecondary

@Composable
fun DiagnosticsPanel(
  diagnostics: DiagnosticsInfo,
  onRetryConnection: () -> Unit,
  modifier: Modifier = Modifier
) {
  var isExpanded by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
      .background(MyraaSurfaceDark.copy(alpha = 0.96f))
      .border(1.dp, MyraaBorder, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
      .padding(14.dp)
  ) {
    // Header Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clickable { isExpanded = !isExpanded }
        .testTag("diagnostics_header"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.BugReport,
          contentDescription = "Diagnostics",
          tint = MyraaPink,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "MYRAA SYSTEM DIAGNOSTICS",
          color = MyraaPink,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 1.sp
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        DiagnosticPill(
          label = diagnostics.liveSessionState.label,
          statusColor = when (diagnostics.liveSessionState) {
            LiveConnectionState.CONNECTED, LiveConnectionState.LISTENING, LiveConnectionState.SPEAKING -> Color(0xFF06D6A0)
            LiveConnectionState.CONNECTING, LiveConnectionState.RECONNECTING -> Color(0xFFFFB703)
            LiveConnectionState.ERROR -> Color(0xFFFF4D6D)
            LiveConnectionState.DISCONNECTED -> MyraaTextSecondary
          }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
          imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
          contentDescription = "Toggle Details",
          tint = MyraaTextSecondary,
          modifier = Modifier.size(20.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Status Grid
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(modifier = Modifier.weight(1f)) {
        DiagnosticRow(label = "MODEL", value = diagnostics.model, valueColor = MyraaPink)
        DiagnosticRow(
          label = "AUTH",
          value = diagnostics.authStatus,
          valueColor = if (diagnostics.authStatus == "OK") Color(0xFF06D6A0) else Color(0xFFFF4D6D)
        )
        DiagnosticRow(
          label = "WEBSOCKET",
          value = diagnostics.websocketStatus,
          valueColor = if (diagnostics.websocketStatus == "CONNECTED") Color(0xFF06D6A0) else Color(0xFFFFB703)
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        DiagnosticRow(label = "AUDIO IN", value = diagnostics.audioInputStatus, valueColor = MyraaTextSecondary)
        DiagnosticRow(label = "AUDIO OUT", value = diagnostics.audioOutputStatus, valueColor = MyraaTextSecondary)
        DiagnosticRow(
          label = "LIVE STATUS",
          value = diagnostics.liveSessionState.label,
          valueColor = if (diagnostics.liveSessionState == LiveConnectionState.ERROR) Color(0xFFFF4D6D) else Color(0xFF06D6A0)
        )
      }
    }

    // Error banner
    if (diagnostics.lastError != null || diagnostics.liveSessionState == LiveConnectionState.ERROR) {
      Spacer(modifier = Modifier.height(10.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFFFF4D6D).copy(alpha = 0.15f))
          .border(1.dp, Color(0xFFFF4D6D).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
          .padding(10.dp)
      ) {
        Column {
          Text(
            text = diagnostics.lastError ?: "Session Connection Error",
            color = Color(0xFFFF4D6D),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace
          )
          Spacer(modifier = Modifier.height(6.dp))
          ElevatedButton(
            onClick = onRetryConnection,
            modifier = Modifier
              .align(Alignment.End)
              .testTag("retry_connection_button"),
            colors = ButtonDefaults.elevatedButtonColors(
              containerColor = Color(0xFFFF4D6D),
              contentColor = Color.White
            ),
            shape = RoundedCornerShape(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Retry",
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Retry Connection", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // Expandable logs
    AnimatedVisibility(visible = isExpanded) {
      Column(modifier = Modifier.padding(top = 10.dp)) {
        Text(
          text = "MYRAA LOG STREAM:",
          fontSize = 11.sp,
          color = MyraaTextSecondary,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF070A10))
            .border(1.dp, MyraaBorder, RoundedCornerShape(8.dp))
            .padding(8.dp)
        ) {
          LazyColumn(reverseLayout = true) {
            items(diagnostics.details.reversed()) { log ->
              Text(
                text = log,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = when {
                  log.contains("[ERROR]") -> Color(0xFFFF4D6D)
                  log.contains("CONNECTED") -> Color(0xFF06D6A0)
                  log.contains("[TOOL]") -> Color(0xFFFFD166)
                  else -> MyraaTextSecondary
                },
                modifier = Modifier.padding(vertical = 1.dp)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DiagnosticRow(label: String, value: String, valueColor: Color) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      fontSize = 10.sp,
      color = MyraaTextSecondary,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.SemiBold
    )
    Text(
      text = value,
      fontSize = 10.sp,
      color = valueColor,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Bold
    )
  }
}

@Composable
private fun DiagnosticPill(label: String, statusColor: Color) {
  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .background(statusColor.copy(alpha = 0.2f))
      .border(1.dp, statusColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
      .padding(horizontal = 8.dp, vertical = 2.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(6.dp)
        .clip(CircleShape)
        .background(statusColor)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = label,
      color = statusColor,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace
    )
  }
}
