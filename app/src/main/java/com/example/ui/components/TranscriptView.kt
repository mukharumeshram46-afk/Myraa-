package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MessageSender
import com.example.data.model.TranscriptMessage
import com.example.ui.theme.MyraaBorder
import com.example.ui.theme.MyraaMagenta
import com.example.ui.theme.MyraaPink
import com.example.ui.theme.MyraaSurfaceDark
import com.example.ui.theme.MyraaSurfaceVariant
import com.example.ui.theme.MyraaTextPrimary
import com.example.ui.theme.MyraaTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TranscriptView(
  messages: List<TranscriptMessage>,
  modifier: Modifier = Modifier
) {
  val listState = rememberLazyListState()

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  LazyColumn(
    state = listState,
    modifier = modifier
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .testTag("transcript_list"),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    items(messages, key = { it.id }) { message ->
      TranscriptBubble(message = message)
    }
  }
}

@Composable
fun TranscriptBubble(message: TranscriptMessage) {
  val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))

  val (bubbleBg, borderColor, icon, senderLabel, labelColor) = when (message.sender) {
    MessageSender.MYRAA, MessageSender.NAMI -> Quintuple(
      MyraaSurfaceDark,
      MyraaPink.copy(alpha = 0.5f),
      Icons.Default.Favorite,
      "MYRAA",
      MyraaPink
    )
    MessageSender.USER -> Quintuple(
      MyraaSurfaceVariant,
      MyraaMagenta.copy(alpha = 0.5f),
      Icons.Default.Person,
      "PIYUSH",
      Color(0xFFFFBE0B)
    )
    MessageSender.TOOL -> Quintuple(
      Color(0xFF1E1624),
      Color(0xFF06D6A0).copy(alpha = 0.5f),
      Icons.Default.Build,
      "NATIVE COPILOT TOOL",
      Color(0xFF06D6A0)
    )
    MessageSender.SYSTEM -> Quintuple(
      Color(0xFF14121E),
      MyraaBorder.copy(alpha = 0.5f),
      Icons.Default.Info,
      "SYSTEM",
      MyraaTextSecondary
    )
  }

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (message.sender == MessageSender.USER) Arrangement.End else Arrangement.Start
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth(0.90f)
        .clip(RoundedCornerShape(12.dp))
        .background(bubbleBg)
        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
        .padding(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = icon,
            contentDescription = senderLabel,
            tint = labelColor,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = senderLabel,
            color = labelColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
          )
        }
        Text(
          text = timeStr,
          color = Color(0xFF757088),
          fontSize = 10.sp,
          fontFamily = FontFamily.Monospace
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = message.text,
        color = MyraaTextPrimary,
        fontSize = 13.sp,
        lineHeight = 18.sp
      )
    }
  }
}

private data class Quintuple<A, B, C, D, E>(
  val first: A,
  val second: B,
  val third: C,
  val fourth: D,
  val fifth: E
)
