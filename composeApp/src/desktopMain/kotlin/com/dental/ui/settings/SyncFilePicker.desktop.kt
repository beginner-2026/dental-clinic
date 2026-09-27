package com.dental.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import javax.swing.JFileChooser

@Composable
actual fun rememberSyncFilePickerLauncher(
    onSelected: (String) -> Unit,
    onError: (String) -> Unit
): () -> Unit {
    val currentOnSelected by rememberUpdatedState(onSelected)
    val currentOnError by rememberUpdatedState(onError)
    return {
        val chooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            dialogTitle = "Выберите папку My Drive в Google Диске"
            isAcceptAllFileFilterUsed = false
        }
        when (chooser.showOpenDialog(null)) {
            JFileChooser.APPROVE_OPTION -> {
                val selected = chooser.selectedFile
                if (selected != null && selected.isDirectory) {
                    currentOnSelected(selected.absolutePath)
                } else {
                    currentOnError("Выберите папку Google Диска")
                }
            }
            else -> Unit
        }
    }
}
