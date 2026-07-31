package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF1E1E24), // Elegant charcoal primary
    onPrimary = Color.White,
    secondary = Color(0xFF5D5E67),
    onSecondary = Color.White,
    background = Color(0xFFF9F7F8),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFF9F7F8),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF9F7F8),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFE1DFE3)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Force false to preserve elegant light theme
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false, // Disable dynamic colors by default to preserve custom theme
  content: @Composable () -> Unit,
) {
  val colorScheme = LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
