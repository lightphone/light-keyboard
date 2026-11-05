package com.thelightphone.lp3Keyboard.ui

import android.content.ContentResolver
import android.provider.Settings
import com.thelightphone.lp3Keyboard.ui.viewmodel.defaultEmojis

/*
 * The user's LP3 keyboard settings, kept in system settings so any app can read them: the keyboard
 * app, the light SDK server (which writes them, and hands them to tools that ask), and tools that
 * can't talk to the server.
 *
 * Only the system uid (LightOS server), shell or root can write them. Not sure if this is the
 * best way, but it's compatible with current tools. Can migrate if we run into problems.
 */

const val KEYBOARD_SETTING_EMOJIS = "lp3_keyboard_emojis"
const val KEYBOARD_SETTING_SHOW_VOICE = "lp3_keyboard_show_voice"
const val KEYBOARD_SETTING_ENABLE_KEY_ANIMATION = "lp3_keyboard_enable_key_animation"
const val KEYBOARD_SETTING_ENABLE_SWIPE = "lp3_keyboard_enable_swipe"

/** the emoji row, as a string of emoji (see [parseEmojiString]). null for the defaults */
var ContentResolver.keyboardEmojis: String?
    get() = Settings.System.getString(this, KEYBOARD_SETTING_EMOJIS)
    set(value) {
        Settings.System.putString(this, KEYBOARD_SETTING_EMOJIS, value)
    }

var ContentResolver.keyboardShowVoice: Boolean
    get() = getBoolean(KEYBOARD_SETTING_SHOW_VOICE)
    set(value) = putBoolean(KEYBOARD_SETTING_SHOW_VOICE, value)

var ContentResolver.keyboardEnableKeyAnimation: Boolean
    get() = getBoolean(KEYBOARD_SETTING_ENABLE_KEY_ANIMATION)
    set(value) = putBoolean(KEYBOARD_SETTING_ENABLE_KEY_ANIMATION, value)

var ContentResolver.keyboardEnableSwipe: Boolean
    get() = getBoolean(KEYBOARD_SETTING_ENABLE_SWIPE)
    set(value) = putBoolean(KEYBOARD_SETTING_ENABLE_SWIPE, value)

fun ContentResolver.keyboardOptions() = KeyboardOptions(
    emojis = parseEmojiString(keyboardEmojis) ?: defaultEmojis,
    displayReturn = true,
    displayVoice = keyboardShowVoice,
    enableKeyAnimation = keyboardEnableKeyAnimation,
    swipeEnabled = keyboardEnableSwipe,
)

// every switch defaults to on
private fun ContentResolver.getBoolean(name: String) = Settings.System.getInt(this, name, 1) == 1

private fun ContentResolver.putBoolean(name: String, value: Boolean) {
    Settings.System.putInt(this, name, if (value) 1 else 0)
}
