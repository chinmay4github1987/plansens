package com.prasjaychi.plantsense.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = Indigo400,
    onPrimary = Slate900,
    primaryContainer = Indigo800,
    onPrimaryContainer = Indigo100,
    secondary = Cyan400,
    onSecondary = Slate900,
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = Cyan100,
    tertiary = Violet400,
    onTertiary = Slate900,
    tertiaryContainer = Color(0xFF4C1D95),
    onTertiaryContainer = Violet100,
    background = Color(0xFF0B0F19),
    onBackground = Slate100,
    surface = Color(0xFF111827),
    onSurface = Slate100,
    surfaceVariant = Color(0xFF1F2937),
    onSurfaceVariant = Slate300,
    outline = Slate600
)

private val LightColorScheme = lightColorScheme(
    primary = Indigo600,
    onPrimary = Color.White,
    primaryContainer = Indigo50,
    onPrimaryContainer = Indigo800,
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Cyan100,
    onSecondaryContainer = Color(0xFF0E7490),
    tertiary = Violet500,
    onTertiary = Color.White,
    tertiaryContainer = Violet100,
    onTertiaryContainer = Color(0xFF5B21B6),
    background = Color(0xFFF8FAFC),
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700,
    outline = Slate300
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our handcrafted harmonious palette by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
