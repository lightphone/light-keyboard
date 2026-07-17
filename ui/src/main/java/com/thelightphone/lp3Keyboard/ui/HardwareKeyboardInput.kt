package com.thelightphone.lp3Keyboard.ui

import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
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

/**
 * Key codes reported by the hardware input on an LP3
 */
enum class LightDeviceKeys(
    val keyCode: Int
) {
    VolumeUp(24),
    VolumeDown(25),
    ShutterPressed(27),
    ShutterHalfPressed(80),
    RotaryTurnUp(317),
    RotaryTurnDown(318),
    RotaryButtonPress(319)
    ;
    companion object {
        val mapping = entries.associateBy { it.keyCode }
    }
}

/**
 * Unfortunately, the Android build running on LP3s uses a keyboard layout that remaps
 * common keys (like 't' and 'r') to behave like LP3-specific hardware buttons. This is likely
 * leftover from early development -> external keyboards weren't really a considered use case
 *
 * So here we re-re-map events from EXTERNAL HID devices. Shouldn't have much of an impact, though
 * if your keyboard produces WHEEL_CW/CCW events, they might come through as T's and R's.
 */
fun lightOsRemap(nativeKeyEvent: KeyEvent): Int {
    val device = InputDevice.getDevice(nativeKeyEvent.deviceId)
    if (device == null || !device.isExternal) return nativeKeyEvent.keyCode
    return when (LightDeviceKeys.mapping[nativeKeyEvent.keyCode]) {
        LightDeviceKeys.RotaryTurnUp -> KeyEvent.KEYCODE_R
        LightDeviceKeys.RotaryTurnDown -> KeyEvent.KEYCODE_T
        LightDeviceKeys.RotaryButtonPress -> KeyEvent.KEYCODE_F8
        LightDeviceKeys.ShutterPressed -> KeyEvent.KEYCODE_RIGHT_BRACKET
        LightDeviceKeys.ShutterHalfPressed -> KeyEvent.KEYCODE_NUMPAD_2
        // don't remap these
        LightDeviceKeys.VolumeUp, LightDeviceKeys.VolumeDown, null -> nativeKeyEvent.keyCode
    }
}

// Routes key events from an external (Bluetooth/USB) hardware keyboard into [callback].
@Composable
fun Modifier.hardwareKeyboardInput(
    callback: Lp3KeyboardCallback,
    remapKeyCode: ((KeyEvent) -> Int)? = ::lightOsRemap
): Modifier {
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
            val keyCode = remapKeyCode?.invoke(native) ?: native.keyCode
            if (keyCode == KeyEvent.KEYCODE_UNKNOWN) return@onKeyEvent true

            val specialKey = when (keyCode) {
                KeyEvent.KEYCODE_DEL -> SpecialKey.Backspace
                KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> SpecialKey.Return
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
                // If we didn't remap, native.unicodeChar already reflects meta state (shift,
                // etc). If we did, it's still resolved for the *original* (wrong) keyCode, so
                // look up the remapped keyCode's character ourselves instead.
                val codePoint = if (keyCode == native.keyCode) {
                    native.unicodeChar.takeIf { it != 0 }
                } else {
                    KeyCharacterMap.load(native.deviceId).get(keyCode, native.metaState)
                        .takeIf { it != 0 }
                } ?: return@onKeyEvent false
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
