package com.thelightphone.lp3Keyboard.ui.zhuyin

import com.thelightphone.lp3Keyboard.ui.composer.ComposerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZhuyinComposerTest {

    private fun composer() = ZhuyinComposer(StubCandidateSource())

    private fun ComposerState.texts() = candidates.map { it.text }

    @Test
    fun `empty buffer yields empty state`() {
        assertEquals(ComposerState.EMPTY, composer().snapshot())
    }

    @Test
    fun `building a full reading surfaces candidates`() {
        val c = composer()
        "ㄋㄧˇ".forEach { c.append(it) }
        val state = c.snapshot()
        assertEquals("ㄋㄧˇ", state.composing)
        assertTrue("你" in state.texts())
    }

    @Test
    fun `single-syllable candidate consumes the whole buffer`() {
        val c = composer()
        "ㄋㄧˇ".forEach { c.append(it) }
        val you = c.snapshot().candidates.first { it.text == "你" }
        assertEquals(3, you.consumed) // ㄋ ㄧ ˇ
    }

    @Test
    fun `partial reading matches by prefix before the tone mark`() {
        val c = composer()
        c.append('ㄋ'); c.append('ㄧ') // no tone yet
        // "ㄋㄧ" isn't an exact key, but "ㄋㄧˇ" is a prefix match.
        assertTrue("你" in c.snapshot().texts())
    }

    @Test
    fun `multi-syllable buffer offers the whole-buffer phrase first then the first syllable`() {
        val c = composer()
        "ㄋㄧˇㄏㄠˇ".forEach { c.append(it) } // 你好
        val cands = c.snapshot().candidates
        // Phrase (whole buffer, consumes 6) leads; 你 (first syllable, consumes 3) follows.
        val phrase = cands.first()
        assertEquals("你好", phrase.text)
        assertEquals(6, phrase.consumed)
        val you = cands.first { it.text == "你" }
        assertEquals(3, you.consumed)
    }

    @Test
    fun `committing the first syllable keeps the remainder composing`() {
        val c = composer()
        "ㄋㄧˇㄏㄠˇ".forEach { c.append(it) }
        val you = c.snapshot().candidates.first { it.text == "你" }
        val after = c.commit(you.consumed)
        assertEquals("ㄏㄠˇ", after.composing)
        assertTrue("好" in after.texts())
    }

    @Test
    fun `committing the whole phrase empties the buffer`() {
        val c = composer()
        "ㄋㄧˇㄏㄠˇ".forEach { c.append(it) }
        val phrase = c.snapshot().candidates.first { it.text == "你好" }
        val after = c.commit(phrase.consumed)
        assertTrue(c.isEmpty)
        assertEquals(ComposerState.EMPTY, after)
    }

    @Test
    fun `multi-syllable phrase does not over-consume from a longer prefix`() {
        // Buffer is just 你 + 好's readings; no phrase longer than the buffer
        // should appear tagged as consuming only part of it.
        val c = composer()
        "ㄋㄧㄏㄠ".forEach { c.append(it) } // toneless 你好
        for (cand in c.snapshot().candidates) {
            assertTrue(cand.consumed <= "ㄋㄧㄏㄠ".length)
        }
    }

    @Test
    fun `backspace pops one symbol and reports emptiness`() {
        val c = composer()
        c.append('ㄕ'); c.append('ˋ')
        assertTrue(c.backspace())
        assertEquals("ㄕ", c.composing)
        assertTrue(c.backspace())
        assertFalse(c.backspace()) // now empty
        assertTrue(c.isEmpty)
    }

    @Test
    fun `clear empties the buffer`() {
        val c = composer()
        "ㄉㄚˋ".forEach { c.append(it) }
        c.clear()
        assertTrue(c.isEmpty)
        assertEquals(ComposerState.EMPTY, c.snapshot())
    }

    @Test
    fun `isZhuyinSymbol accepts bopomofo and tone marks but not latin`() {
        assertTrue(ZhuyinComposer.isZhuyinSymbol('ㄅ'.code))
        assertTrue(ZhuyinComposer.isZhuyinSymbol('ㄩ'.code))
        assertTrue(ZhuyinComposer.isZhuyinSymbol('ˇ'.code))
        assertTrue(ZhuyinComposer.isZhuyinSymbol('˙'.code))
        assertFalse(ZhuyinComposer.isZhuyinSymbol('a'.code))
        assertFalse(ZhuyinComposer.isZhuyinSymbol('5'.code))
    }
}
