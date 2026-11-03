package com.thelightphone.lp3Keyboard.textentry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
