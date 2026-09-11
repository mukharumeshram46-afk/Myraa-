package com.example.data.model

import androidx.compose.ui.graphics.Color

/**
 * Mood State Machine for MYRAA.
 * Moods: IDLE, HAPPY, CUTE, SHY, PLAYFUL, TEASING, MOCK_ANGRY, CARING, PROUD, EXCITED, SLEEPY, THINKING
 *
 * Each mood defines:
 * - auraColor: Glow color used on the UI orb and background aura
 * - secondaryAuraColor: Dual-tone glow
 * - emoji: Visual mood indicator
 * - statusTag: Cute status label displayed on UI
 * - decayDurationMs: How long this mood stays before decaying back toward HAPPY/IDLE
 */
enum class MyraaMood(
  val label: String,
  val auraColor: Color,
  val secondaryAuraColor: Color,
  val emoji: String,
  val statusTag: String,
  val decayDurationMs: Long = 18_000L
) {
  IDLE(
    label = "IDLE",
    auraColor = Color(0xFF00F0FF), // Soft Cyan
    secondaryAuraColor = Color(0xFF0077B6),
    emoji = "✨",
    statusTag = "Here for Piyush",
    decayDurationMs = 0L
  ),
  HAPPY(
    label = "HAPPY",
    auraColor = Color(0xFFFF70A6), // Warm Bright Rose
    secondaryAuraColor = Color(0xFFFF9770),
    emoji = "🌸",
    statusTag = "Khush & Cheerful",
    decayDurationMs = 25_000L
  ),
  CUTE(
    label = "CUTE",
    auraColor = Color(0xFFFFB4D6), // Soft Pastel Pink
    secondaryAuraColor = Color(0xFFFF85A1),
    emoji = "🥰",
    statusTag = "Aww Piyush",
    decayDurationMs = 20_000L
  ),
  SHY(
    label = "SHY",
    auraColor = Color(0xFFFF99C8), // Blushing Rose
    secondaryAuraColor = Color(0xFFE4C1F9),
    emoji = "🙈",
    statusTag = "Blushing",
    decayDurationMs = 15_000L
  ),
  PLAYFUL(
    label = "PLAYFUL",
    auraColor = Color(0xFFB5179E), // Vibrant Magenta Violet
    secondaryAuraColor = Color(0xFF7209B7),
    emoji = "😜",
    statusTag = "Masti Mood",
    decayDurationMs = 22_000L
  ),
  TEASING(
    label = "TEASING",
    auraColor = Color(0xFFFF007F), // Electric Pink
    secondaryAuraColor = Color(0xFFFF5400),
    emoji = "😏",
    statusTag = "Nakhre-wali",
    decayDurationMs = 18_000L
  ),
  MOCK_ANGRY(
    label = "MOCK_ANGRY",
    auraColor = Color(0xFFFF4D6D), // Playful Crimson Red
    secondaryAuraColor = Color(0xFFC9184A),
    emoji = "😤",
    statusTag = "Katti! (Just kidding)",
    decayDurationMs = 16_000L
  ),
  CARING(
    label = "CARING",
    auraColor = Color(0xFF06D6A0), // Gentle Emerald Jade
    secondaryAuraColor = Color(0xFF118AB2),
    emoji = "🥺",
    statusTag = "Take care of yourself",
    decayDurationMs = 30_000L
  ),
  PROUD(
    label = "PROUD",
    auraColor = Color(0xFFFFD166), // Glowing Amber Gold
    secondaryAuraColor = Color(0xFFF77F00),
    emoji = "💖",
    statusTag = "Proud of you, Piyush!",
    decayDurationMs = 20_000L
  ),
  EXCITED(
    label = "EXCITED",
    auraColor = Color(0xFFFFBE0B), // Neon Sunshine Spark
    secondaryAuraColor = Color(0xFFFF006E),
    emoji = "🎉",
    statusTag = "Yay! So excited!",
    decayDurationMs = 20_000L
  ),
  SLEEPY(
    label = "SLEEPY",
    auraColor = Color(0xFF7209B7), // Deep Twilight Indigo
    secondaryAuraColor = Color(0xFF3F37C9),
    emoji = "🥱",
    statusTag = "Neend aa rahi hai...",
    decayDurationMs = 25_000L
  ),
  THINKING(
    label = "THINKING",
    auraColor = Color(0xFF4CC9F0), // Luminous Cyan-Blue
    secondaryAuraColor = Color(0xFF4895EF),
    emoji = "🤔",
    statusTag = "Analyzing screen & tools...",
    decayDurationMs = 15_000L
  );

  companion object {
    fun fromName(name: String?): MyraaMood {
      if (name.isNullOrBlank()) return IDLE
      return values().find { it.name.equals(name.trim(), ignoreCase = true) } ?: IDLE
    }
  }
}
