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
    primary = LogoBlue,
    onPrimary = Color.White,
    secondary = LogoOrange,
    onSecondary = Color.White,
    tertiary = LogoPink,
    onTertiary = Color.White,
    background = BlueLightBackground, // White (#FFFFFF)
    surface = SurfaceWhite,           // Very Light Gray (#F5F5F5)
    onBackground = TextDarkPrimary,   // Koyu Gri (#212121)
    onSurface = TextDarkPrimary,       // Koyu Gri (#212121)
    outlineVariant = GrayBorder
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Forced to false per user request
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  // Always use the bright LightColorScheme to ensure Açık Tema standards
  val colorScheme = LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
