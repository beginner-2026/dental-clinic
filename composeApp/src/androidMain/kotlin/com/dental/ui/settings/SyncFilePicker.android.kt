package com.dental.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberSyncFilePickerLauncher(
    onSelected: (String) -> Unit,
    onError: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val currentOnSelected by rememberUpdatedState(onSelected)
    val currentOnError by rememberUpdatedState(onError)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            currentOnError("Не удалось закрепить постоянный доступ к sync.json")
            return@rememberLauncherForActivityResult
        }
        currentOnSelected(uri.toString())
    }
    return remember(launcher) {
        {
            launcher.launch(
                arrayOf(
                    "application/json",
                    "text/json",
                    "text/plain",
                    "application/octet-stream"
                )
            )
        }
    }
}
