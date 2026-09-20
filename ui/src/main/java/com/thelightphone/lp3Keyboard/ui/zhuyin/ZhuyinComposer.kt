package com.thelightphone.lp3Keyboard.ui.zhuyin

import com.thelightphone.lp3Keyboard.ui.composer.ComposerCandidate
import com.thelightphone.lp3Keyboard.ui.composer.ComposerState

/**
 * Holds the in-progress bopomofo buffer and derives candidates from a
 * [CandidateSource]. Pure logic, no Compose / no Android — so it unit-tests
 * cleanly and the view model can own one per keyboard session.
 *
 * The buffer is one flat run of symbols, but [snapshot] segments it (via
 * [ZhuyinSyllable]) so a multi-syllable buffer offers both the whole-buffer
 * phrase and shorter prefixes down to the first syllable. Committing a candidate
 * consumes only its reading and leaves the remainder composing (see [commit]),
 * so phrases can be built one syllable at a time.
 */
class ZhuyinComposer(private val source: CandidateSource) {
    private val buffer = StringBuilder()

    val composing: String get() = buffer.toString()
    val isEmpty: Boolean get() = buffer.isEmpty()

    /**
     * Append one bopomofo symbol or tone mark. Callers gate on
     * [isZhuyinSymbol]; anything else should be committed directly, not routed
     * here. Segmentation happens lazily in [snapshot], so append stays trivial.
     */
    fun append(symbol: Char) {
        buffer.append(symbol)
    }

    /** Pop the last symbol. Returns false if the buffer was already empty (so
     *  the caller can fall through to a normal field backspace). */
    fun backspace(): Boolean {
        if (buffer.isEmpty()) return false
        buffer.deleteCharAt(buffer.length - 1)
        return true
    }

    fun clear() {
        buffer.setLength(0)
    }

    /**
     * Drop the first [consumed] characters of the buffer (the reading a chosen
     * candidate covered) and return the resulting snapshot, so any trailing
     * syllables stay composing for the next pick. [consumed] is clamped to the
     * buffer length.
     */
    fun commit(consumed: Int): ComposerState {
        buffer.delete(0, consumed.coerceIn(0, buffer.length))
        return snapshot()
    }

    /**
     * Current snapshot: buffer text + candidates (empty buffer ⇒ no lookup).
     *
     * A single syllable (or a still-incomplete one) uses prefix lookup, so
     * partially-typed readings and short-phrase predictions still surface, and
     * every candidate consumes the whole buffer. Once the buffer holds more than
     * one syllable it's segmented: the whole-buffer phrase comes first (exact,
     * so an over-long phrase can't sneak in), then each shorter prefix down to
     * the first syllable, each tagged with exactly how much it consumes.
     */
    fun snapshot(): ComposerState {
        val reading = buffer.toString()
        if (reading.isEmpty()) return ComposerState.EMPTY

        val segments = ZhuyinSyllable.segment(reading)
        if (segments.size <= 1) {
            val cands = source.candidates(reading).map { ComposerCandidate(it, reading.length) }
            return ComposerState(reading, cands)
        }

        // Multi-syllable: whole buffer first, then shrinking prefixes so the user
        // can commit a leading phrase/char and keep the rest. De-dupe by text,
        // first (longest) occurrence winning.
        val byText = LinkedHashMap<String, ComposerCandidate>()
        for (k in segments.size downTo 1) {
            val prefix = segments.subList(0, k).joinToString("")
            val consumed = prefix.length
            for (word in source.candidatesExact(prefix)) {
                byText.getOrPut(word) { ComposerCandidate(word, consumed) }
            }
        }
        return ComposerState(reading, byText.values.toList())
    }

    companion object {
        // Bopomofo consonants + medials + rimes: U+3105 (ㄅ) .. U+3129 (ㄩ),
        // with the block reserving through U+312F for later additions.
        private const val BOPOMOFO_START = 0x3105
        private const val BOPOMOFO_END = 0x312F

        // Tone marks the MOE layout emits. First tone (ˉ) is conventionally
        // omitted, so it isn't here. ˙ˇˊˋ = light/3rd/2nd/4th.
        private val TONE_MARKS = charArrayOf('˙', 'ˇ', 'ˊ', 'ˋ')

        /** True if [code] is a bopomofo symbol or tone mark the composer accepts. */
        fun isZhuyinSymbol(code: Int): Boolean =
            code in BOPOMOFO_START..BOPOMOFO_END ||
                (code <= Char.MAX_VALUE.code && code.toChar() in TONE_MARKS)
    }
}
