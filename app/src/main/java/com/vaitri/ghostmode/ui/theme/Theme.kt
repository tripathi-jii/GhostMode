package com.vaitri.ghostmode.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GhostModeColorScheme = darkColorScheme(

    primary = GhostPurple,
    onPrimary = GhostBlack,

    primaryContainer = GhostPurpleDark,
    onPrimaryContainer = GhostTextPrimary,

    secondary = GhostCyan,
    onSecondary = GhostBlack,

    secondaryContainer = GhostSurfaceLight,
    onSecondaryContainer = GhostTextPrimary,

    tertiary = GhostPink,
    onTertiary = GhostBlack,

    background = GhostBlack,
    onBackground = GhostTextPrimary,

    surface = GhostSurface,
    onSurface = GhostTextPrimary,

    surfaceVariant = GhostSurfaceLight,
    onSurfaceVariant = GhostTextSecondary,

    error = GhostError
)

@Composable
fun GhostModeTheme(
    content: @Composable () -> Unit
) {

    MaterialTheme(
        colorScheme = GhostModeColorScheme,
        typography = Typography,
        content = content
    )
}