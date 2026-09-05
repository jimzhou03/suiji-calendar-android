package com.jimzhou03.suijicalendar.ui.theme

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

private val LightColors = lightColorScheme(
    primary = Color(0xFF8B4B62),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E2),
    secondary = Color(0xFF6F5A61),
    tertiary = Color(0xFF795831),
    background = Color(0xFFFFF8F8),
    surface = Color(0xFFFFF8F8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB0C7),
    primaryContainer = Color(0xFF6F344B),
    secondary = Color(0xFFDCBEC7),
    tertiary = Color(0xFFE9BF8A),
)

@Composable
fun SuijiCalendarTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isSystemInDarkTheme() -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        isSystemInDarkTheme() -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
