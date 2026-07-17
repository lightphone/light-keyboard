package com.thelightphone.lp3Keyboard.ui

import android.view.KeyEvent.KEYCODE_DEL
import android.view.KeyEvent.KEYCODE_ENTER
import android.view.KeyEvent.KEYCODE_NUMPAD_ENTER
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalView

// Routes key events from an external (Bluetooth/USB) hardware keyboard into [callback]
@Composable
fun Modifier.hardwareKeyboardInput(callback: Lp3KeyboardCallback): Modifier {
    val view = LocalView.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        if (!view.isFocused) {
            view.requestFocus()
        }
        focusRequester.requestFocus()
    }
    return this
        .focusRequester(focusRequester)
        .focusable()
        .onKeyEvent { keyEvent ->
            val native = keyEvent.nativeKeyEvent
            val specialKey = when (native.keyCode) {
                KEYCODE_DEL -> SpecialKey.Backspace
                KEYCODE_ENTER, KEYCODE_NUMPAD_ENTER -> SpecialKey.Return
                else -> null
            }
            if (specialKey != null) {
                when (keyEvent.type) {
                    KeyEventType.KeyDown -> {
                        if (native.repeatCount == 0) {
                            callback.onSpecialKeyPressed(specialKey)
                        }
                        true
                    }
                    KeyEventType.KeyUp -> {
                        callback.onSpecialKeyReleased(specialKey)
                        true
                    }
                    else -> false
                }
            } else {
                val codePoint = native.unicodeChar.takeIf { it != 0 } ?: return@onKeyEvent false
                when (keyEvent.type) {
                    KeyEventType.KeyDown -> {
                        if (native.repeatCount == 0) {
                            callback.onKeyPressed(codePoint)
                        }
                        true
                    }
                    KeyEventType.KeyUp -> {
                        callback.onKeyReleased(codePoint)
                        true
                    }
                    else -> false
                }
            }
        }
}
