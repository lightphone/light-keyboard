package com.thelightphone.lp3Keyboard.ui.viewmodel

import android.text.InputType
import android.text.TextUtils
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.ui.text.TextRange
import com.thelightphone.lp3Keyboard.ui.SpecialKey

/**
 * Applies LP3 keyboard events to [state], editing at its cursor the way the IME edits through an
 * InputConnection. Shared by the keyboard app's full-screen text entry and imported by the SDK's
 * embedded editor.
 *
 * @param singleLine whether the return key calls [onReturn] instead of adding a line break
 * @param onHaptic called whenever a key goes down
 * @param onCapsModeChange given whether the next letter starts a sentence, after every edit and
 * [updateCapsMode]. null leaves caps alone
 */
class TextFieldKeyboardCallback(
    val state: TextFieldState,
    var singleLine: Boolean = false,
    private val onReturn: () -> Unit = {},
    private val onHaptic: () -> Unit = {},
    private val onCapsModeChange: ((Boolean) -> Unit)? = null,
) : Lp3RepeatableKeyboardCallback {

    // replaces all the text with [text] and puts the cursor at the end. a single line gets spaces for line breaks
    fun setText(text: CharSequence) {
        state.setTextAndPlaceCursorAtEnd(
            if (singleLine) text.lines().joinToString(" ") else text.toString()
        )
        updateCapsMode()
    }

    fun moveCursor(offset: Int) {
        state.edit { selection = TextRange(offset.coerceIn(0, length)) }
        updateCapsMode()
    }

    fun updateCapsMode() {
        val onCapsModeChange = onCapsModeChange ?: return
        val caps = TextUtils.getCapsMode(
            state.text,
            state.selection.min,
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        )
        onCapsModeChange(caps != 0)
    }

    override fun onKeyPressed(code: Int) = onHaptic()

    override fun onSpecialKeyPressed(key: SpecialKey) {
        onHaptic()
        if (key == SpecialKey.Space) insert(" ")
    }

    override fun onKeyReleased(code: Int) = insert(buildString { appendCodePoint(code) })

    override fun onSpecialKeyReleased(key: SpecialKey) {
        when (key) {
            SpecialKey.Backspace -> deleteChar()
            SpecialKey.Return -> if (singleLine) onReturn() else insert("\n")
            else -> Unit
        }
    }

    override fun onKeyLongPressed(code: Int) = Unit

    override fun onSpecialKeyLongPressed(key: SpecialKey) {
        if (key == SpecialKey.Backspace) deleteWord()
    }

    override fun onSubmitWord(word: CharSequence) = insert(word.toString())

    override fun onKeyRepeated(code: Int) = onKeyReleased(code)

    override fun onSpecialKeyRepeated(specialKey: SpecialKey) {
        when (specialKey) {
            SpecialKey.Space -> insert(" ")
            SpecialKey.Backspace -> deleteWord()
            else -> Unit
        }
    }

    // replaces the selection (usually just the cursor) with [text]
    private fun insert(text: String) {
        state.edit {
            val start = selection.min
            replace(start, selection.max, text)
            selection = TextRange(start + text.length)
        }
        updateCapsMode()
    }

    // deletes from [start] up to the cursor
    private fun deleteBefore(start: Int) {
        state.edit {
            val end = selection.min
            if (start >= end) return@edit
            delete(start, end)
            selection = TextRange(start)
        }
        updateCapsMode()
    }

    private fun deleteChar() {
        val end = state.selection.min
        if (end == 0) return
        // a whole code point, so emoji don't leave half a surrogate pair behind
        deleteBefore(Character.offsetByCodePoints(state.text, end, -1))
    }

    // back to the start of the word before the cursor, along with any spaces after it
    private fun deleteWord() {
        val before = state.text.subSequence(0, state.selection.min).trimEnd()
        val lastSpace = before.indexOfLast { it.isWhitespace() }
        deleteBefore(if (lastSpace >= 0) lastSpace + 1 else 0)
    }
}
