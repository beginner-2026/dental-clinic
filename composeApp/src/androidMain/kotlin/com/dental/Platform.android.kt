package com.dental

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

actual fun getPlatformName(): String = "Android"

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
