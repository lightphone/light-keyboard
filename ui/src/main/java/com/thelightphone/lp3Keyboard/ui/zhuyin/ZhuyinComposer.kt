package com.thelightphone.lp3Keyboard.ui.zhuyin

import kotlinx.coroutines.flow.StateFlow

/**
 * A single tappable candidate: the [text] to commit and how many characters of
 * the raw composing buffer picking it consumes ([consumed]). For a single
 * syllable that's the whole buffer; for a segmented multi-syllable buffer a
 * first-syllable candidate consumes only that syllable and leaves the rest
 * composing, which is what lets the user build phrases the dictionary doesn't
 * know by committing one reading at a time.
 */
data class ZhuyinCandidate(val text: String, val consumed: Int)

/**
 * Immutable snapshot the UI renders: the raw bopomofo the user is building
 * ([composing], shown as underlined pre-edit text in the field) and the ranked
 * [candidates] for it (shown in the candidate bar).
 */
data class ZhuyinComposerState(
    val composing: String,
    val candidates: List<ZhuyinCandidate>,
) {
    /** Whether a composition is in progress — drives whether the bar shows. */
    val isActive: Boolean get() = composing.isNotEmpty()

    companion object {
        val EMPTY = ZhuyinComposerState("", emptyList())

        /**
         * Preview/test helper: build a state whose candidates each consume the
         * whole [composing] buffer (the common single-syllable case).
         */
        fun of(composing: String, texts: List<String>): ZhuyinComposerState =
            ZhuyinComposerState(composing, texts.map { ZhuyinCandidate(it, composing.length) })
    }
}

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
    fun commit(consumed: Int): ZhuyinComposerState {
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
    fun snapshot(): ZhuyinComposerState {
        val reading = buffer.toString()
        if (reading.isEmpty()) return ZhuyinComposerState.EMPTY

        val segments = ZhuyinSyllable.segment(reading)
        if (segments.size <= 1) {
            val cands = source.candidates(reading).map { ZhuyinCandidate(it, reading.length) }
            return ZhuyinComposerState(reading, cands)
        }

        // Multi-syllable: whole buffer first, then shrinking prefixes so the user
        // can commit a leading phrase/char and keep the rest. De-dupe by text,
        // first (longest) occurrence winning.
        val byText = LinkedHashMap<String, ZhuyinCandidate>()
        for (k in segments.size downTo 1) {
            val prefix = segments.subList(0, k).joinToString("")
            val consumed = prefix.length
            for (word in source.candidatesExact(prefix)) {
                byText.getOrPut(word) { ZhuyinCandidate(word, consumed) }
            }
        }
        return ZhuyinComposerState(reading, byText.values.toList())
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

/**
 * Implemented by a view model that supports Zhuyin composition, so the
 * candidate-bar UI can observe state and report taps without knowing the
 * concrete view model type.
 */
interface ZhuyinComposerHost {
    val composerStateFlow: StateFlow<ZhuyinComposerState>

    /** User tapped a candidate in the bar. */
    fun onCandidateSelected(candidate: ZhuyinCandidate)
}

/**
 * Implemented by the InputMethodService. The view model drives the actual
 * InputConnection pre-edit / commit through this, so composition state stays in
 * the view model and only the IC calls live in the service.
 */
interface ZhuyinImeActions {
    /** Set (or, on empty text, finish) the underlined composing region. */
    fun onComposingChanged(composing: CharSequence)

    /** Commit a chosen candidate and end the current composition. */
    fun onCommitCandidate(text: CharSequence)
}
