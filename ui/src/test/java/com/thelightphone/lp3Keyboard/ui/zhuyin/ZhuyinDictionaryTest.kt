package com.thelightphone.lp3Keyboard.ui.zhuyin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZhuyinDictionaryTest {

    // Minimal fixture mirroring the asset format: `key\tword1 word2 …`, sorted by key.
    private val dict = ZhuyinDictionary.fromLines(
        sequenceOf(
            "ㄋㄧ\t你 妳 泥 尼",
            "ㄋㄧㄇㄣ\t你們",
            "ㄋㄧㄏㄠ\t你好 妳好",
            "ㄕ\t是 事 市",
        ),
    )

    @Test
    fun `exact single-syllable lookup is frequency-ordered`() {
        // Exact-ㄋㄧ words come first (then ㄋㄧ… prefixes, covered below).
        assertEquals(listOf("你", "妳", "泥", "尼"), dict.lookup("ㄋㄧ").take(4))
    }

    @Test
    fun `exact match comes before longer prefix extensions`() {
        val r = dict.lookup("ㄋㄧ")
        // 你 (exact ㄋㄧ) must precede 你好/你們 (ㄋㄧ… prefixes).
        assertTrue(r.indexOf("你") < r.indexOf("你好"))
        assertTrue("你好" in r && "你們" in r)
    }

    @Test
    fun `multi-syllable exact lookup`() {
        assertEquals(listOf("你好", "妳好"), dict.lookup("ㄋㄧㄏㄠ"))
    }

    @Test
    fun `lookupExact ignores longer-key extensions`() {
        // Prefix lookup pulls in 你好/你們; exact must not.
        assertEquals(listOf("你", "妳", "泥", "尼"), dict.lookupExact("ㄋㄧ"))
        assertTrue("你好" !in dict.lookupExact("ㄋㄧ"))
    }

    @Test
    fun `lookupExact on a full phrase key still resolves`() {
        assertEquals(listOf("你好", "妳好"), dict.lookupExact("ㄋㄧㄏㄠ"))
    }

    @Test
    fun `lookupExact miss returns nothing`() {
        assertTrue(dict.lookupExact("ㄋㄧㄏ").isEmpty()) // partial, not a key
        assertTrue(dict.lookupExact("").isEmpty())
    }

    @Test
    fun `limit caps the result count`() {
        assertEquals(2, dict.lookup("ㄋㄧ", limit = 2).size)
    }

    @Test
    fun `miss and empty query return nothing`() {
        assertTrue(dict.lookup("ㄅㄆㄇ").isEmpty())
        assertTrue(dict.lookup("").isEmpty())
    }

    @Test
    fun `readingKey strips tone marks and spaces to match asset keys`() {
        assertEquals("ㄕ", ZhuyinDictionary.readingKey("ㄕˋ"))
        assertEquals("ㄋㄧㄏㄠ", ZhuyinDictionary.readingKey("ㄋㄧˇ ㄏㄠˇ"))
        assertEquals("ㄇㄣ", ZhuyinDictionary.readingKey("ㄇㄣ˙"))
    }

    @Test
    fun `readingKey feeds lookup end-to-end for a toned buffer`() {
        // What the composer buffer would hold after typing ㄕˋ.
        assertEquals(listOf("是", "事", "市"), dict.lookup(ZhuyinDictionary.readingKey("ㄕˋ")))
    }

    @Test
    fun `fromLines can sort unsorted input`() {
        val d = ZhuyinDictionary.fromLines(
            sequenceOf("ㄕ\t是", "ㄋㄧ\t你"),
            presorted = false,
        )
        assertEquals(listOf("你"), d.lookup("ㄋㄧ"))
        assertEquals(listOf("是"), d.lookup("ㄕ"))
    }

    // --- tone-aware ranking (3-column fixture: key \t words \t sig-sets) --------

    // 媽=1st, 馬=3rd, 嗎=1st/3rd/neutral, 罵=4th; freq order 媽 馬 嗎 罵.
    private val toned = ZhuyinDictionary.fromLines(
        sequenceOf(
            "ㄇㄚ\t媽 馬 嗎 罵\t1 3 0/1/3 4",
            "ㄋㄧㄏㄠ\t你好 妳好\t33 33",
        ),
    )

    @Test
    fun `no tone mark keeps plain frequency order`() {
        assertEquals(listOf("媽", "馬", "嗎", "罵"), toned.lookup("ㄇㄚ"))
    }

    @Test
    fun `third tone floats tone-matching words to the front`() {
        // ㄇㄚˇ ⇒ 馬 (3) and 嗎 (has 3) first, then the non-matching 媽 罵 in order.
        assertEquals(listOf("馬", "嗎", "媽", "罵"), toned.lookup("ㄇㄚˇ"))
    }

    @Test
    fun `fourth tone floats a different word to the front`() {
        assertEquals(listOf("罵", "媽", "馬", "嗎"), toned.lookup("ㄇㄚˋ"))
    }

    @Test
    fun `tone reorders but never drops candidates`() {
        assertEquals(
            toned.lookup("ㄇㄚ").sorted(),
            toned.lookup("ㄇㄚˇ").sorted(),
        )
    }

    @Test
    fun `lookupExact is tone-aware too`() {
        assertEquals(listOf("馬", "嗎", "媽", "罵"), toned.lookupExact("ㄇㄚˇ"))
        assertEquals(listOf("媽", "馬", "嗎", "罵"), toned.lookupExact("ㄇㄚ"))
    }

    @Test
    fun `multi-syllable tone pattern matches a phrase`() {
        assertEquals(listOf("你好", "妳好"), toned.lookupExact("ㄋㄧˇㄏㄠˇ"))
    }

    @Test
    fun `prefix query scores a longer word by its leading syllable tone`() {
        // ㄇㄚ has no ㄇㄚ… phrase extension here, so use ㄋㄧㄏㄠ: a 3rd-tone first
        // syllable ㄋㄧˇ prefix-matches the phrase whose first syllable is 3rd.
        assertTrue("你好" in toned.lookup("ㄋㄧˇ"))
    }
}
