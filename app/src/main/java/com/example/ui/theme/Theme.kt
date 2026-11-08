package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemePalette(
    val id: String,
    val displayName: String,
    val primaryColor: Color,
    val accentColor: Color,
    val primaryContainerLight: Color,
    val primaryContainerDark: Color,
    val onPrimaryContainerLight: Color,
    val onPrimaryContainerDark: Color,
    val sentBubbleDark: Color,
    val sentBubbleLight: Color
) {
    EMERALD(
        id = "EMERALD",
        displayName = "Emerald",
        primaryColor = Color(0xFF10B981),
        accentColor = Color(0xFF14B8A6),
        primaryContainerLight = Color(0xFFD1FAE5),
        primaryContainerDark = Color(0xFF047857),
        onPrimaryContainerLight = Color(0xFF065F46),
        onPrimaryContainerDark = Color(0xFFECFDF5),
        sentBubbleDark = Color(0xFF064E3B),
        sentBubbleLight = Color(0xFFD1FAE5)
    ),
    MIDNIGHT(
        id = "MIDNIGHT",
        displayName = "Midnight",
        primaryColor = Color(0xFF6366F1),
        accentColor = Color(0xFF818CF8),
        primaryContainerLight = Color(0xFFE0E7FF),
        primaryContainerDark = Color(0xFF3730A3),
        onPrimaryContainerLight = Color(0xFF1E1B4B),
        onPrimaryContainerDark = Color(0xFFEEF2FF),
        sentBubbleDark = Color(0xFF312E81),
        sentBubbleLight = Color(0xFFE0E7FF)
    ),
    OCEAN(
        id = "OCEAN",
        displayName = "Ocean",
        primaryColor = Color(0xFF0EA5E9),
        accentColor = Color(0xFF38BDF8),
        primaryContainerLight = Color(0xFFE0F2FE),
        primaryContainerDark = Color(0xFF075985),
        onPrimaryContainerLight = Color(0xFF0C4A6E),
        onPrimaryContainerDark = Color(0xFFF0F9FF),
        sentBubbleDark = Color(0xFF0C4A6E),
        sentBubbleLight = Color(0xFFE0F2FE)
    ),
    FOREST(
        id = "FOREST",
        displayName = "Forest",
        primaryColor = Color(0xFF16A34A),
        accentColor = Color(0xFF4ADE80),
        primaryContainerLight = Color(0xFFDCFCE7),
        primaryContainerDark = Color(0xFF14532D),
        onPrimaryContainerLight = Color(0xFF052E16),
        onPrimaryContainerDark = Color(0xFFF0FDF4),
        sentBubbleDark = Color(0xFF14532D),
        sentBubbleLight = Color(0xFFDCFCE7)
    ),
    SUNSET(
        id = "SUNSET",
        displayName = "Sunset",
        primaryColor = Color(0xFFF43F5E),
        accentColor = Color(0xFFFB7185),
        primaryContainerLight = Color(0xFFFFE4E6),
        primaryContainerDark = Color(0xFF881337),
        onPrimaryContainerLight = Color(0xFF4C0519),
        onPrimaryContainerDark = Color(0xFFFFF1F2),
        sentBubbleDark = Color(0xFF881337),
        sentBubbleLight = Color(0xFFFFE4E6)
    )
}

fun getDarkColorScheme(palette: AppThemePalette): ColorScheme {
    return darkColorScheme(
        primary = palette.primaryColor,
        onPrimary = Color.White,
        primaryContainer = palette.primaryContainerDark,
        onPrimaryContainer = palette.onPrimaryContainerDark,
        secondary = palette.accentColor,
        onSecondary = Color.White,
        tertiary = Sky500,
        background = Slate900,
        onBackground = Slate100,
        surface = Slate800,
        onSurface = Slate100,
        surfaceVariant = Slate700,
        onSurfaceVariant = Slate400,
        outline = Slate600
    )
}

fun getLightColorScheme(palette: AppThemePalette): ColorScheme {
    return lightColorScheme(
        primary = palette.primaryColor,
        onPrimary = Color.White,
        primaryContainer = palette.primaryContainerLight,
        onPrimaryContainer = palette.onPrimaryContainerLight,
        secondary = palette.accentColor,
        onSecondary = Color.White,
        tertiary = Sky500,
        background = LightBg,
        onBackground = LightTextPrimary,
        surface = LightSurface,
        onSurface = LightTextPrimary,
        surfaceVariant = Color(0xFFF1F5F9),
        onSurfaceVariant = LightTextSecondary,
        outline = LightBorder
    )
}

@Composable
fun LinkoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: AppThemePalette = AppThemePalette.EMERALD,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) getDarkColorScheme(palette) else getLightColorScheme(palette)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

