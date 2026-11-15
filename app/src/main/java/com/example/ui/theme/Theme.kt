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

private val LightColorScheme =
  lightColorScheme(
    primary = NavyPrimary,
    onPrimary = CrispWhite,
    primaryContainer = SkyLight,
    onPrimaryContainer = DeepNavyText,
    secondary = TealSecondary,
    onSecondary = CrispWhite,
    secondaryContainer = TealContainerLight,
    onSecondaryContainer = TealDarkText,
    tertiary = AmberTertiary,
    onTertiary = CrispWhite,
    tertiaryContainer = AmberContainerLight,
    onTertiaryContainer = AmberDarkText,
    background = PaperBackground,
    onBackground = SlateInk,
    surface = CrispWhite,
    onSurface = SlateInk,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = MutedSlate,
    outline = SubtleOutline,
    error = ErrorCrimson,
    onError = CrispWhite,
    errorContainer = ErrorContainerLight,
    onErrorContainer = Color(0xFF7F1D1D),
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DeepNavyText,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = SkyLight,
    secondary = DarkSecondary,
    onSecondary = TealDarkText,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = TealContainerLight,
    tertiary = DarkTertiary,
    onTertiary = AmberDarkText,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = AmberContainerLight,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextMuted,
    outline = DarkOutline,
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF7F1D1D),
    errorContainer = Color(0xFF991B1B),
    onErrorContainer = Color(0xFFFEE2E2),
  )

@Composable
fun SmartClassroomTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = SmartClassroomTypography,
    content = content,
  )
}
