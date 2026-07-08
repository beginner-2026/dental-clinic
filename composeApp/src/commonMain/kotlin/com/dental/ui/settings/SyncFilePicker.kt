package com.dental.ui.settings

import androidx.compose.runtime.Composable

@Composable
expect fun rememberSyncFilePickerLauncher(
    onSelected: (String) -> Unit
): () -> Unit
