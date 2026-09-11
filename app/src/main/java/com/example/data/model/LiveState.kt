package com.example.data.model

/**
 * State machine for Gemini Live session as specified:
 * DISCONNECTED -> CONNECTING -> CONNECTED -> LISTENING -> SPEAKING -> ERROR -> RECONNECTING
 */
enum class LiveConnectionState(val label: String) {
  DISCONNECTED("DISCONNECTED"),
  CONNECTING("CONNECTING"),
  CONNECTED("CONNECTED"),
  LISTENING("LISTENING"),
  SPEAKING("SPEAKING"),
  ERROR("ERROR"),
  RECONNECTING("RECONNECTING")
}

/**
 * Diagnostics information structure showing real status for all subsystems.
 */
data class DiagnosticsInfo(
  val model: String = "gemini-3.1-flash-live-preview",
  val authStatus: String = "NOT CONFIGURED", // "OK", "FAILED", "NOT CONFIGURED"
  val websocketStatus: String = "DISCONNECTED", // "CONNECTED", "CONNECTING", "ERROR", "DISCONNECTED"
  val audioInputStatus: String = "READY", // "STREAMING", "READY", "OFF", "PERMISSION_DENIED"
  val audioOutputStatus: String = "READY", // "PLAYING", "READY", "OFF"
  val liveSessionState: LiveConnectionState = LiveConnectionState.DISCONNECTED,
  val lastError: String? = null,
  val details: List<String> = emptyList()
)

/**
 * Conversation transcript item.
 */
data class TranscriptMessage(
  val id: String = java.util.UUID.randomUUID().toString(),
  val sender: MessageSender,
  val text: String,
  val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
  MYRAA,
  NAMI,
  USER,
  SYSTEM,
  TOOL
}

/**
 * Assistant operating modes toggled via voice tools or UI chips.
 */
enum class MyraaMode(val displayName: String, val badgeColorHex: Long) {
  STANDARD("MYRAA COPILOT", 0xFFFF70A6),
  COMPANION("GIRLFRIEND MODE", 0xFFFF007F),
  FOCUS("FOCUS MODE", 0xFF9D4EDD),
  STUDY("STUDY MODE", 0xFF00E676),
  DEVELOPER("DEV MODE", 0xFFFF9100)
}

typealias NamiMode = MyraaMode
