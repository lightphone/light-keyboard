package com.thelightphone.lp3Keyboard.textentry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextEntryRequestTest {

    @Test
    fun `defaults to a single line where return finishes like the check`() {
        val request = TextEntryRequest()
        assertEquals(TextEntryMode.Simple, request.mode)
        assertEquals(TextEntryRequest.ACTION_ID_DONE, request.returnActionId)
    }

    @Test
    fun `return does nothing by default when there's no done action`() {
        val submit = TextEntryAction("submit", TextEntryPosition.BottomCenter, TextEntryAction.Content.Text("SUBMIT"))
        assertNull(TextEntryRequest(actions = listOf(submit)).returnActionId)
    }

    @Test
    fun `an explicit return action wins`() {
        val request = TextEntryRequest(returnActionId = null)
        assertNull(request.returnActionId)
    }

    @Test
    fun `not a password by default`() {
        assertFalse(TextEntryRequest().isPassword)
    }

    @Test
    fun `the password option obscures a single line`() {
        assertTrue(TextEntryRequest(options = TextEntryRequest.PASSWORD_OPTIONS).isPassword)
    }

    @Test
    fun `multi line ignores the password option`() {
        val request = TextEntryRequest(mode = TextEntryMode.Multi, options = TextEntryRequest.PASSWORD_OPTIONS)
        assertFalse(request.isPassword)
    }

    @Test
    fun `auto capitalizes by default`() {
        assertTrue(TextEntryRequest().autoCapitalize)
        assertTrue(TextEntryRequest(mode = TextEntryMode.Multi).autoCapitalize)
    }

    @Test
    fun `auto capitalize can be turned off`() {
        val options = mapOf(TextEntryProtocol.OPTION_AUTO_CAPITALIZE to "false")
        assertFalse(TextEntryRequest(options = options).autoCapitalize)
    }

    @Test
    fun `passwords don't auto capitalize unless asked to`() {
        assertFalse(TextEntryRequest(options = TextEntryRequest.PASSWORD_OPTIONS).autoCapitalize)
        val options = TextEntryRequest.PASSWORD_OPTIONS + (TextEntryProtocol.OPTION_AUTO_CAPITALIZE to "true")
        assertTrue(TextEntryRequest(options = options).autoCapitalize)
    }
}
