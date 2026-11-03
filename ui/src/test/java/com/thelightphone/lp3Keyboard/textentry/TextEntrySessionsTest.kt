package com.thelightphone.lp3Keyboard.textentry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class TextEntrySessionsTest {
    @Test
    fun `opening a session replaces the previous one`() {
        val sessions = TextEntrySessions<String>()
        val first = sessions.open { "first" }
        assertNull(first.replaced)

        val second = sessions.open { "second" }
        assertEquals("first", second.replaced)
        assertNotEquals(first.id, second.id)
        assertNull(sessions.find(first.id))
        assertEquals("second", sessions.find(second.id))
    }

    @Test
    fun `stale ids can't close the open session`() {
        val sessions = TextEntrySessions<String>()
        val first = sessions.open { "first" }
        val second = sessions.open { "second" }

        assertNull(sessions.close(first.id))
        assertEquals("second", sessions.close(second.id))
        assertNull(sessions.find(second.id))
    }

    @Test
    fun `payloads are built with their session id`() {
        val sessions = TextEntrySessions<String>()
        val opened = sessions.open { id -> "result-$id" }
        assertEquals("result-${opened.id}", sessions.find(opened.id))
    }

    @Test
    fun `paths round trip`() {
        TextEntryFile.entries.forEach { file ->
            assertEquals("id" to file, TextEntryFile.parse(TextEntryFile.pathSegments("id", file)))
        }
        assertNull(TextEntryFile.parse(listOf("id", "other")))
        assertNull(TextEntryFile.parse(listOf("id")))
    }

    @Test
    fun `source is read-only and result is write-only`() {
        TextEntryFile.Source.checkMode("r")
        listOf("w", "rw", "wt").forEach { mode ->
            assertThrows(SecurityException::class.java) { TextEntryFile.Source.checkMode(mode) }
        }

        listOf("w", "wt", "wa").forEach { mode -> TextEntryFile.Result.checkMode(mode) }
        listOf("r", "rw", "rwt").forEach { mode ->
            assertThrows(SecurityException::class.java) { TextEntryFile.Result.checkMode(mode) }
        }
    }

    @Test
    fun `protocol values have not changed`() {
        // installed keyboards and apps built against older versions rely on these
        assertEquals("com.thelightphone.lp3keyboard.action.ENTER_TEXT", TextEntryProtocol.ACTION_ENTER_TEXT)
        assertEquals("com.thelightphone.lp3keyboard.extra.PROTOCOL_VERSION", TextEntryProtocol.EXTRA_PROTOCOL_VERSION)
        assertEquals("com.thelightphone.lp3keyboard.extra.SOURCE_URI", TextEntryProtocol.EXTRA_SOURCE_URI)
        assertEquals("com.thelightphone.lp3keyboard.extra.RESULT_URI", TextEntryProtocol.EXTRA_RESULT_URI)
        assertEquals("com.thelightphone.lp3keyboard.extra.REPLY", TextEntryProtocol.EXTRA_REPLY)
        assertEquals("com.thelightphone.lp3keyboard.extra.TITLE", TextEntryProtocol.EXTRA_TITLE)
        assertEquals("com.thelightphone.lp3keyboard.extra.MODE", TextEntryProtocol.EXTRA_MODE)
        assertEquals("com.thelightphone.lp3keyboard.extra.ACTIONS", TextEntryProtocol.EXTRA_ACTIONS)
        assertEquals("com.thelightphone.lp3keyboard.extra.RETURN_ACTION_ID", TextEntryProtocol.EXTRA_RETURN_ACTION_ID)
        assertEquals("com.thelightphone.lp3keyboard.extra.ACTION_ID", TextEntryProtocol.EXTRA_ACTION_ID)
        assertEquals("com.thelightphone.lp3keyboard.cancel", TextEntryProtocol.ACTION_ID_CANCEL)
        assertEquals(
            listOf("id", "position", "icon", "icon_uri", "text", "label"),
            listOf(
                TextEntryProtocol.ACTION_KEY_ID,
                TextEntryProtocol.ACTION_KEY_POSITION,
                TextEntryProtocol.ACTION_KEY_ICON,
                TextEntryProtocol.ACTION_KEY_ICON_URI,
                TextEntryProtocol.ACTION_KEY_TEXT,
                TextEntryProtocol.ACTION_KEY_LABEL,
            ),
        )
        assertEquals(listOf("simple", "multi"), TextEntryMode.entries.map { it.wireName })
        assertEquals(
            listOf("top_left", "top_right", "bottom_left", "bottom_center", "bottom_right"),
            TextEntryPosition.entries.map { it.wireName },
        )
        assertEquals(listOf("back", "check"), TextEntryIcon.entries.map { it.wireName })
    }

    @Test
    fun `wire names round trip, and unknown ones are rejected`() {
        TextEntryMode.entries.forEach { assertEquals(it, TextEntryMode.fromWireName(it.wireName)) }
        TextEntryPosition.entries.forEach { assertEquals(it, TextEntryPosition.fromWireName(it.wireName)) }
        TextEntryIcon.entries.forEach { assertEquals(it, TextEntryIcon.fromWireName(it.wireName)) }
        assertNull(TextEntryMode.fromWireName("other"))
        assertNull(TextEntryPosition.fromWireName("middle"))
        assertNull(TextEntryIcon.fromWireName(null))
    }
}
