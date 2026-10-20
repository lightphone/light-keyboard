package com.thelightphone.lp3Keyboard.ui.zhuyin

import org.junit.Assert.assertEquals
import org.junit.Test

class ZhuyinSyllableTest {

    @Test
    fun `empty buffer segments to nothing`() {
        assertEquals(emptyList<String>(), ZhuyinSyllable.segment(""))
    }

    @Test
    fun `single complete syllable stays one segment`() {
        assertEquals(listOf("ㄕㄧˋ"), ZhuyinSyllable.segment("ㄕㄧˋ"))
    }

    @Test
    fun `two syllables split at the next initial`() {
        // 你好, both tone-marked.
        assertEquals(listOf("ㄋㄧˇ", "ㄏㄠˇ"), ZhuyinSyllable.segment("ㄋㄧˇㄏㄠˇ"))
    }

    @Test
    fun `toneless run still splits at initials`() {
        assertEquals(listOf("ㄋㄧ", "ㄏㄠ"), ZhuyinSyllable.segment("ㄋㄧㄏㄠ"))
    }

    @Test
    fun `medial-only and final-only syllables are recognised`() {
        // ㄨㄛˇ 我 then ㄦ 兒: no-initial medial start, then final-only.
        assertEquals(listOf("ㄨㄛˇ", "ㄦ"), ZhuyinSyllable.segment("ㄨㄛˇㄦ"))
    }

    @Test
    fun `three syllables including a bare medial syllable`() {
        // ㄓㄨㄥ 中 ㄨㄣˊ 文 ㄧ 一
        assertEquals(listOf("ㄓㄨㄥ", "ㄨㄣˊ", "ㄧ"), ZhuyinSyllable.segment("ㄓㄨㄥㄨㄣˊㄧ"))
    }

    @Test
    fun `trailing partial syllable is its own segment`() {
        // 你 fully, then a lone initial the user just started.
        assertEquals(listOf("ㄋㄧ", "ㄏ"), ZhuyinSyllable.segment("ㄋㄧㄏ"))
    }

    @Test
    fun `concatenation of segments reproduces the input`() {
        val input = "ㄒㄧㄝˋㄒㄧㄝ˙ㄋㄧˇ"
        assertEquals(input, ZhuyinSyllable.segment(input).joinToString(""))
    }
}
