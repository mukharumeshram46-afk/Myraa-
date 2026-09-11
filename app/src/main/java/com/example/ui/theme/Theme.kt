package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MyraaDarkColorScheme = darkColorScheme(
  primary = MyraaPink,
  onPrimary = Color(0xFF0B0A12),
  primaryContainer = Color(0xFF381528),
  onPrimaryContainer = MyraaPinkLight,
  secondary = MyraaMagenta,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFF3B0B2E),
  onSecondaryContainer = Color(0xFFFFB4D6),
  tertiary = MyraaEmerald,
  background = MyraaBgDark,
  onBackground = MyraaTextPrimary,
  surface = MyraaSurfaceDark,
  onSurface = MyraaTextPrimary,
  surfaceVariant = MyraaSurfaceVariant,
  onSurfaceVariant = MyraaTextSecondary,
  outline = MyraaBorder,
  error = MyraaRed,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = MyraaDarkColorScheme,
    typography = Typography,
    content = content
  )
}
