package com.dental

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import java.awt.Dialog
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent

actual fun getPlatformName(): String = "Desktop"

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // На десктопе нет системного жеста «назад» — используем клавишу Esc
    val currentOnBack by rememberUpdatedState(onBack)
    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose {}
        val focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
        val dispatcher = java.awt.KeyEventDispatcher { e ->
            if (e.id == KeyEvent.KEY_PRESSED && e.keyCode == KeyEvent.VK_ESCAPE) {
                if (focusManager.focusedWindow is Dialog) {
                    // В модальных диалогах Esc обрабатывается самим диалогом
                    false
                } else {
                    currentOnBack()
                    true
                }
            } else {
                false
            }
        }
        focusManager.addKeyEventDispatcher(dispatcher)
        onDispose { focusManager.removeKeyEventDispatcher(dispatcher) }
    }
}
