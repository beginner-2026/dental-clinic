package com.dental

import androidx.compose.runtime.Composable

expect fun getPlatformName(): String

@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
