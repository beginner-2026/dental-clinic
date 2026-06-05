package com.dental.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = DentalColors.Primary,
    secondary = DentalColors.Secondary,
    background = DentalColors.Background,
    surface = DentalColors.Surface,
    error = DentalColors.Error,
    onPrimary = DentalColors.OnPrimary,
    onSecondary = DentalColors.OnSecondary,
    onBackground = DentalColors.OnBackground,
    onSurface = DentalColors.OnSurface,
    onError = DentalColors.OnError,
)

@Composable
fun DentalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
