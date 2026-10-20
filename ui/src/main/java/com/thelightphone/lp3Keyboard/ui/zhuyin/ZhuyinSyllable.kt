package com.thelightphone.lp3Keyboard.ui.zhuyin

/**
 * Splits a raw bopomofo buffer into individual syllables so the composer can
 * treat multi-character input as a sequence of readings (你好, not one long
 * blur) and commit them left-to-right.
 *
 * Bopomofo phonotactics are near-unambiguous: a syllable is
 *   [initial]? [medial]? [final]? [tone]?
 * with at least one sound, and an initial consonant always begins a new
 * syllable. So a simple greedy left-to-right munch segments correctly for
 * well-formed input, which is all the keyboard can produce.
 *
 * Pure Kotlin / no Android — unit-tested directly.
 */
object ZhuyinSyllable {

    // Initial consonants ㄅ..ㄙ (U+3105..U+3119).
    private const val INITIAL_START = 0x3105
    private const val INITIAL_END = 0x3119

    // Rimes / finals ㄚ..ㄦ (U+311A..U+3126).
    private const val FINAL_START = 0x311A
    private const val FINAL_END = 0x3126

    // Medial glides ㄧㄨㄩ (U+3127..U+3129).
    private const val MEDIAL_START = 0x3127
    private const val MEDIAL_END = 0x3129

    // Tone marks the MOE layout emits (ˉ first tone included for completeness).
    private val TONES = charArrayOf('ˉ', 'ˊ', 'ˇ', 'ˋ', '˙')

    private fun isInitial(c: Char) = c.code in INITIAL_START..INITIAL_END
    private fun isMedial(c: Char) = c.code in MEDIAL_START..MEDIAL_END
    private fun isFinal(c: Char) = c.code in FINAL_START..FINAL_END
    private fun isTone(c: Char) = c in TONES

    /**
     * Segment [reading] into syllable substrings, in order, whose concatenation
     * equals [reading]. An empty buffer yields an empty list.
     */
    fun segment(reading: String): List<String> {
        if (reading.isEmpty()) return emptyList()
        val out = ArrayList<String>()
        var i = 0
        val n = reading.length
        while (i < n) {
            val start = i
            if (i < n && isInitial(reading[i])) i++
            if (i < n && isMedial(reading[i])) i++
            if (i < n && isFinal(reading[i])) i++
            if (i < n && isTone(reading[i])) i++
            // Guard against a character in none of the classes (shouldn't happen
            // for gated input) so we never spin.
            if (i == start) i++
            out.add(reading.substring(start, i))
        }
        return out
    }
}
