package com.dental.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState

@Composable
expect fun rememberSyncFilePickerLauncher(
    onSelected: (String) -> Unit,
    onError: (String) -> Unit = {}
): () -> Unit
