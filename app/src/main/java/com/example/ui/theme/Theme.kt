package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val HadirinLightColorScheme = lightColorScheme(
    primary = HadirinNavy,
    onPrimary = Color.White,
    primaryContainer = HadirinBlueSoft,
    onPrimaryContainer = HadirinNavy,
    secondary = HadirinBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFF6FF),
    onSecondaryContainer = HadirinNavyLight,
    tertiary = HadirinSuccess,
    onTertiary = Color.White,
    tertiaryContainer = HadirinSuccessSoft,
    onTertiaryContainer = Color(0xFF14532D),
    error = HadirinDanger,
    onError = Color.White,
    errorContainer = HadirinDangerSoft,
    onErrorContainer = Color(0xFF7F1D1D),
    background = HadirinBackground,
    onBackground = HadirinInkPrimary,
    surface = HadirinSurface,
    onSurface = HadirinInkPrimary,
    surfaceVariant = HadirinSurfaceVariant,
    onSurfaceVariant = HadirinInkSecondary,
    outline = HadirinOutline,
    outlineVariant = Color(0xFFCBD5E1)
)

private val HadirinDarkColorScheme = darkColorScheme(
    primary = HadirinDarkPrimary,
    onPrimary = HadirinNavy,
    primaryContainer = HadirinNavyLight,
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = HadirinDarkSecondary,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF1E3A8A),
    onSecondaryContainer = Color(0xFFDBEAFE),
    tertiary = Color(0xFF4ADE80),
    onTertiary = Color(0xFF052E16),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    background = HadirinDarkBackground,
    onBackground = Color(0xFFF8FAFC),
    surface = HadirinDarkSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = HadirinDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = HadirinDarkOutline
)

val HadirinShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun HadirinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) HadirinDarkColorScheme else HadirinLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = HadirinShapes,
        content = content
    )
}
