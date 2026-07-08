package com.dental.ui.settings

import androidx.compose.runtime.Composable
import javax.swing.JFileChooser

@Composable
actual fun rememberSyncFilePickerLauncher(
    onSelected: (String) -> Unit
): () -> Unit {
    return {
        val chooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            dialogTitle = "Выберите папку синхронизации"
            isAcceptAllFileFilterUsed = false
        }
        val result = chooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            onSelected(chooser.selectedFile.absolutePath)
        }
    }
}
