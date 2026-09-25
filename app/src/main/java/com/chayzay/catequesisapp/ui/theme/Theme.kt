package com.chayzay.catequesisapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

// Paleta exacta declarada por la aplicación legacy en res/values/colors.xml.
// Se usa de forma fija: la app original no aplicaba Material You ni modo oscuro.
private val LegacyColorScheme = lightColorScheme(
    primary = Color(0xFF7A9989),
    onPrimary = Color.White,
    secondary = Color(0xFF76C4D7),
    onSecondary = Color.White,
    tertiary = Color(0xFF4D749B),
    onTertiary = Color.White,
    background = Color.White,
    onBackground = Color(0xFF505050),
    surface = Color.White,
    onSurface = Color(0xFF505050),
    surfaceVariant = Color(0xFFF2F2F2),
    onSurfaceVariant = Color(0xFF505050),
    outline = Color(0xFF868686),
    error = Color(0xFFB61818)
)

@Composable
fun CatequesisTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    fontOption: Int = 2,
    content: @Composable () -> Unit
) {
    // Se conservan los parámetros para no romper llamadas existentes, pero la paleta
    // permanece fija como en la app original.
    @Suppress("UNUSED_VARIABLE") val legacyFixedPalette = darkTheme || dynamicColor
    val systemDensity = LocalDensity.current
    val multiplier = when (fontOption) { 1 -> 0.85f; 3 -> 1.2f; 4 -> 1.4f; else -> 1f }

    CompositionLocalProvider(LocalDensity provides Density(systemDensity.density,
        systemDensity.fontScale * multiplier)) {
        MaterialTheme(colorScheme = LegacyColorScheme, typography = Typography, content = content)
    }
}
