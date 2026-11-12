package com.thelightphone.lp3Keyboard.ui

import android.text.TextUtils
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.TextRange
import com.thelightphone.lp3Keyboard.ui.viewmodel.TextFieldKeyboardCallback
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.Assert.assertEquals
import org.junit.Test

class TextFieldKeyboardCallbackTest {

    private fun callback(
        text: String,
        cursor: Int = text.length,
        singleLine: Boolean = false,
        onReturn: () -> Unit = {},
    ) = TextFieldKeyboardCallback(TextFieldState(text, TextRange(cursor)), singleLine, onReturn)

    private val TextFieldKeyboardCallback.text get() = state.text.toString()
    private val TextFieldKeyboardCallback.cursor get() = state.selection.min

    @Test
    fun `keys insert at the cursor`() {
        val callback = callback("ac", cursor = 1)
        callback.onKeyReleased('b'.code)
        assertEquals("abc", callback.text)
        assertEquals(2, callback.cursor)
    }

    @Test
    fun `keys replace the selection`() {
        val callback = TextFieldKeyboardCallback(TextFieldState("a big dog", TextRange(2, 5)))
        callback.onSubmitWord("small")
        assertEquals("a small dog", callback.text)
        assertEquals(7, callback.cursor)
    }

    @Test
    fun `space goes in when pressed, not released`() {
        val callback = callback("a")
        callback.onSpecialKeyPressed(SpecialKey.Space)
        assertEquals("a ", callback.text)
        callback.onSpecialKeyReleased(SpecialKey.Space)
        assertEquals("a ", callback.text)
    }

    @Test
    fun `backspace deletes the character before the cursor`() {
        val callback = callback("abc", cursor = 2)
        callback.onSpecialKeyReleased(SpecialKey.Backspace)
        assertEquals("ac", callback.text)
        assertEquals(1, callback.cursor)
    }

    @Test
    fun `backspace deletes a whole emoji`() {
        val callback = callback("hi😀")
        callback.onSpecialKeyReleased(SpecialKey.Backspace)
        assertEquals("hi", callback.text)
    }

    @Test
    fun `backspace at the start does nothing`() {
        val callback = callback("abc", cursor = 0)
        callback.onSpecialKeyReleased(SpecialKey.Backspace)
        assertEquals("abc", callback.text)
    }

    @Test
    fun `long press and held backspace delete the word before the cursor`() {
        val callback = callback("one two  three", cursor = 9)
        callback.onSpecialKeyLongPressed(SpecialKey.Backspace)
        assertEquals("one three", callback.text)
        callback.onSpecialKeyRepeated(SpecialKey.Backspace)
        assertEquals("three", callback.text)
        assertEquals(0, callback.cursor)
    }

    @Test
    fun `return adds a line break to multi line text`() {
        var returned = false
        val callback = callback("a", onReturn = { returned = true })
        callback.onSpecialKeyReleased(SpecialKey.Return)
        assertEquals("a\n", callback.text)
        assertEquals(false, returned)
    }

    @Test
    fun `return in single line text calls onReturn instead`() {
        var returned = false
        val callback = callback("a", singleLine = true, onReturn = { returned = true })
        callback.onSpecialKeyReleased(SpecialKey.Return)
        assertEquals("a", callback.text)
        assertEquals(true, returned)
    }

    @Test
    fun `setText puts the cursor at the end, and a single line gets spaces for line breaks`() {
        val callback = callback("", singleLine = true)
        callback.setText("one\ntwo\r\nthree")
        assertEquals("one two three", callback.text)
        assertEquals(callback.text.length, callback.cursor)
    }

    @Test
    fun `moveCursor stays inside the text`() {
        val callback = callback("abc")
        callback.moveCursor(1)
        assertEquals(1, callback.cursor)
        callback.moveCursor(10)
        assertEquals(3, callback.cursor)
    }

    @Test
    fun `caps mode follows the cursor`() {
        mockkStatic(TextUtils::class)
        try {
            // caps wherever the cursor is right after ". "
            every { TextUtils.getCapsMode(any(), any(), any()) } answers {
                val text = firstArg<CharSequence>()
                val offset = secondArg<Int>()
                if (offset == 0 || text.subSequence(0, offset).endsWith(". ")) 1 else 0
            }
            val caps = mutableListOf<Boolean>()
            val callback = TextFieldKeyboardCallback(
                TextFieldState("Hi. there", TextRange(9)),
                onCapsModeChange = { caps += it },
            )
            callback.moveCursor(4)
            callback.onKeyReleased('x'.code)
            assertEquals(listOf(true, false), caps)
        } finally {
            unmockkStatic(TextUtils::class)
        }
    }

    private class FakeClipboard(var clip: CharSequence? = null) :
        TextFieldKeyboardCallback.KeyboardClipboard {
        override fun getText() = clip
        override fun setText(text: CharSequence) {
            clip = text
        }
    }

    @Test
    fun `copy puts all the text on the clipboard`() {
        val clipboard = FakeClipboard()
        val callback = TextFieldKeyboardCallback(TextFieldState("hello", TextRange(2)), clipboard = clipboard)
        callback.onEditAction(EditAction.Copy)
        assertEquals("hello", clipboard.clip)
    }

    @Test
    fun `paste inserts the clipboard at the cursor`() {
        val callback = TextFieldKeyboardCallback(
            TextFieldState("ac", TextRange(1)),
            clipboard = FakeClipboard("b"),
        )
        callback.onEditAction(EditAction.Paste)
        assertEquals("abc", callback.text)
        assertEquals(2, callback.cursor)
    }

    @Test
    fun `paste joins lines on a single line`() {
        val callback = TextFieldKeyboardCallback(
            TextFieldState(""),
            singleLine = true,
            clipboard = FakeClipboard("a\nb"),
        )
        callback.onEditAction(EditAction.Paste)
        assertEquals("a b", callback.text)
    }

    @Test
    fun `clear empties the text`() {
        val callback = callback("hello")
        callback.onEditAction(EditAction.Clear)
        assertEquals("", callback.text)
    }
}
