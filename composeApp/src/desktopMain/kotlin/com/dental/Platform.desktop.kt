package com.dental

import androidx.compose.runtime.Composable

actual fun getPlatformName(): String = "Desktop"

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // На десктопе нет системного жеста «назад»
}
