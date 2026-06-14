package com.mobilegamecontroller.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val XboxGreen = Color(0xFF107C10)
private val XboxGreenDark = Color(0xFF0E6B0E)
private val DarkSurface = Color(0xFF1A1A2E)
private val DarkBackground = Color(0xFF0F0F1A)

private val DarkColorScheme = darkColorScheme(
    primary = XboxGreen,
    onPrimary = Color.White,
    secondary = Color(0xFF5CDB95),
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = XboxGreen,
    onPrimary = Color.White,
    secondary = XboxGreenDark,
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A)
)

@Composable
fun MobileGameControllerTheme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit
) {
    val useDarkTheme = darkTheme ?: isSystemInDarkTheme()
    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
