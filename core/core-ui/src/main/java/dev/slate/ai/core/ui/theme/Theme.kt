package dev.slate.ai.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SlatePrimary,
    onPrimary = SlateOnPrimary,
    primaryContainer = SlatePrimaryContainer,
    onPrimaryContainer = SlateOnPrimaryContainer,
    secondary = SlateSecondary,
    onSecondary = SlateOnSecondary,
    secondaryContainer = SlateSecondaryContainer,
    surface = SlateSurface,
    onSurface = SlateOnSurface,
    onSurfaceVariant = SlateOnSurfaceVariant,
    surfaceContainer = SlateSurfaceContainer,
    surfaceContainerHigh = SlateSurfaceContainerHigh,
    surfaceContainerHighest = SlateSurfaceContainerHighest,
    error = SlateError,
    onError = SlateOnError,
    errorContainer = SlateErrorContainer,
    outline = SlateOutline,
    outlineVariant = SlateOutlineVariant,
)

private val LightColorScheme = lightColorScheme(
    primary = SlateLightPrimary,
    primaryContainer = SlateLightPrimaryContainer,
    surface = SlateLightSurface,
    onSurface = SlateLightOnSurface,
    onSurfaceVariant = SlateLightOnSurfaceVariant,
    surfaceContainer = SlateLightSurfaceContainer,
    outline = SlateLightOutline,
)

@Composable
fun SlateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SlateTypography,
        content = content,
    )
}
