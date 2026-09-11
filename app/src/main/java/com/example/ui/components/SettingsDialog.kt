package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.live.GeminiLiveSessionManager

@Composable
fun SettingsDialog(
  currentKey: String,
  onSaveKey: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var keyInput by remember { mutableStateOf(currentKey) }
  var isPasswordVisible by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier
      .clip(RoundedCornerShape(16.dp))
      .border(1.dp, Color(0xFF00F0FF).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
      .testTag("settings_dialog"),
    containerColor = Color(0xFF0D1424),
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Key,
          contentDescription = "API Auth",
          tint = Color(0xFF00F0FF),
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "GEMINI AUTHENTICATION",
          color = Color(0xFF00F0FF),
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 1.sp
        )
      }
    },
    text = {
      Column {
        Text(
          text = "Model: ${GeminiLiveSessionManager.LIVE_MODEL}",
          color = Color(0xFFE0E6ED),
          fontSize = 12.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Enter your Google Gemini API key or inject GEMINI_API_KEY via AI Studio Secrets panel. Key is stored safely in runtime memory.",
          color = Color(0xFF8892B0),
          fontSize = 12.sp,
          lineHeight = 16.sp
        )
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
          value = keyInput,
          onValueChange = { keyInput = it },
          label = { Text("Gemini API Key", color = Color(0xFF8892B0)) },
          visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          trailingIcon = {
            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
              Icon(
                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = "Toggle visibility",
                tint = Color(0xFF00F0FF)
              )
            }
          },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("api_key_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF00F0FF),
            unfocusedBorderColor = Color(0xFF1E2A44),
            focusedTextColor = Color(0xFFE0E6ED),
            unfocusedTextColor = Color(0xFFE0E6ED),
            cursorColor = Color(0xFF00F0FF)
          )
        )
      }
    },
    confirmButton = {
      ElevatedButton(
        onClick = {
          onSaveKey(keyInput.trim())
          onDismiss()
        },
        colors = ButtonDefaults.elevatedButtonColors(
          containerColor = Color(0xFF00F0FF),
          contentColor = Color(0xFF070A10)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.testTag("save_key_button")
      ) {
        Text("Save & Apply", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = Color(0xFF8892B0))
      }
    }
  )
}
