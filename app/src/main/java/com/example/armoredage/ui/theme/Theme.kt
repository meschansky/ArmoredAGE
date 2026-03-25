package com.example.armoredage.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF0057D9),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9E2FF),
    onPrimaryContainer = Color(0xFF001945),
    secondary = Color(0xFF4B5D92),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDEE2FF),
    onSecondaryContainer = Color(0xFF08174B),
    tertiary = Color(0xFF00696B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF9EF1F1),
    onTertiaryContainer = Color(0xFF002021),
    surface = Color(0xFFF7F9FF),
    surfaceVariant = Color(0xFFE3E7F2),
    background = Color(0xFFF3F6FC),
    error = Color(0xFFBA1A1A)
)

private val Dark = darkColorScheme(
    primary = Color(0xFFB2C5FF),
    onPrimary = Color(0xFF002C72),
    primaryContainer = Color(0xFF0040A5),
    onPrimaryContainer = Color(0xFFD9E2FF),
    secondary = Color(0xFFBCC6F9),
    onSecondary = Color(0xFF1A2E60),
    secondaryContainer = Color(0xFF334578),
    onSecondaryContainer = Color(0xFFDEE2FF),
    tertiary = Color(0xFF82D5D5),
    onTertiary = Color(0xFF003738),
    tertiaryContainer = Color(0xFF004F51),
    onTertiaryContainer = Color(0xFF9EF1F1),
    surface = Color(0xFF11141C),
    surfaceVariant = Color(0xFF434752),
    background = Color(0xFF0D1017),
    error = Color(0xFFFFB4AB)
)

@Composable
fun ArmoredAgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        content = content
    )
}
