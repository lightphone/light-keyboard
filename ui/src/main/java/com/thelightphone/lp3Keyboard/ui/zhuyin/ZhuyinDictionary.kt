package com.thelightphone.lp3Keyboard.ui.zhuyin

/**
 * In-memory bopomofo→漢字 lookup, built from `key\tword…\tsig…` lines (see
 * [fromLines]). Pure Kotlin / no Android, so it's unit-testable and independent
 * of how the bytes are loaded — a [CandidateSource] can back itself with one.
 *
 * Keys are readings with **tone marks and spaces removed** (matching how the
 * keyboard buffer arrives: space-less, tones optional). Per-key word lists are
 * already frequency-ranked by the preprocessing step. Each word also carries a
 * set of tone signatures (one digit per syllable, e.g. 嗎 = "0","1","3"), which
 * lets lookup rank tone-consistent words first **when** the caller's reading
 * carries explicit tone marks — while the key itself stays tone-insensitive.
 */
class ZhuyinDictionary private constructor(
    private val keys: Array<String>,             // sorted ascending
    private val values: Array<Array<String>>,    // parallel: words per key, best-first
    private val sigs: Array<Array<Array<String>>>, // parallel: tone-signature set per word
) {
    val keyCount: Int get() = keys.size

    /**
     * Candidates for [reading] (a raw keyboard buffer, tone marks optional):
     * exact-match words first, then words from keys that have the reading as a
     * prefix (so partially-typed multi-syllable phrases still surface),
     * de-duplicated and capped at [limit].
     *
     * If [reading] carries explicit tone marks, words whose reading is consistent
     * with every typed tone are ranked ahead of the rest (stable within each
     * group); a tone-less reading behaves exactly as a plain frequency lookup.
     */
    fun lookup(reading: String, limit: Int = DEFAULT_LIMIT): List<String> {
        val key = readingKey(reading)
        if (key.isEmpty() || keys.isEmpty()) return emptyList()
        val pattern = tonePattern(reading)
        val toneAware = pattern.any { it != null }

        // Ordered de-dupe by word: insertion order is the frequency/exact-first
        // ranking; a word seen again under a longer prefix key only raises its
        // tone score. A stable sort later keeps this order within equal scores.
        val order = LinkedHashMap<String, Score>()
        var i = lowerBound(key)
        while (i < keys.size && keys[i].startsWith(key)) {
            val words = values[i]
            val wordSigs = sigs[i]
            for (w in words.indices) {
                val word = words[w]
                val existing = order[word]
                if (existing == null) {
                    order[word] = Score(if (toneAware) scoreOf(pattern, wordSigs[w]) else 0)
                } else if (toneAware) {
                    val s = scoreOf(pattern, wordSigs[w])
                    if (s > existing.value) existing.value = s
                }
            }
            // Tone-less: stop as soon as we have enough. Tone-aware needs a wider
            // scan (a low-frequency exact-tone word may outrank a high-frequency
            // wrong-tone one) but stays bounded by SCAN_CAP.
            if (!toneAware) {
                if (order.size >= limit) break
            } else if (order.size >= SCAN_CAP) {
                break
            }
            i++
        }
        if (!toneAware) return order.keys.take(limit)
        return order.entries
            .sortedByDescending { it.value.value } // stable: ties keep insertion order
            .take(limit)
            .map { it.key }
    }

    /**
     * Candidates whose key is **exactly** [reading]'s (tone/space-stripped) key,
     * best-first, capped at [limit]. Unlike [lookup] this never pulls in words
     * from longer keys, so a caller that has committed a reading boundary (e.g.
     * one segmented syllable) never over-consumes into a longer phrase. Explicit
     * tones in [reading] still float tone-consistent words to the front.
     */
    fun lookupExact(reading: String, limit: Int = DEFAULT_LIMIT): List<String> {
        val key = readingKey(reading)
        if (key.isEmpty() || keys.isEmpty()) return emptyList()
        val i = lowerBound(key)
        if (i >= keys.size || keys[i] != key) return emptyList()
        val words = values[i]
        val pattern = tonePattern(reading)
        if (pattern.none { it != null }) {
            return if (words.size <= limit) words.toList() else words.take(limit)
        }
        val wordSigs = sigs[i]
        return words.indices
            .sortedByDescending { scoreOf(pattern, wordSigs[it]) } // stable on ties
            .take(limit)
            .map { words[it] }
    }

    /** First index whose key is >= [q]; keys.size if none. */
    private fun lowerBound(q: String): Int {
        var lo = 0
        var hi = keys.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (keys[mid] < q) lo = mid + 1 else hi = mid
        }
        return lo
    }

    private class Score(var value: Int)

    companion object {
        const val DEFAULT_LIMIT = 60

        // How many distinct words to gather before tone-ranking a prefix lookup.
        // Bounds work for common initials while still reaching low-frequency
        // exact-tone matches; tone matches almost always appear well before this.
        private const val SCAN_CAP = 256

        // Must match the tone/space stripping in build_zhuyin_dict.py so a runtime
        // query lands on the same keys the asset was built with.
        // U+02C9 ˉ, U+02CA ˊ, U+02C7 ˇ, U+02CB ˋ, U+02D9 ˙.
        private val STRIP = charArrayOf(' ', '\t', 'ˉ', 'ˊ', 'ˇ', 'ˋ', '˙')

        // Tone mark -> signature digit (must match build_zhuyin_dict.py).
        private fun toneDigit(c: Char): Char? = when (c) {
            'ˉ' -> '1'; 'ˊ' -> '2'; 'ˇ' -> '3'; 'ˋ' -> '4'; '˙' -> '0'
            else -> null
        }

        /** Reduce a raw bopomofo buffer to its tone/space-stripped lookup key. */
        fun readingKey(reading: String): String =
            buildString { for (c in reading) if (c !in STRIP) append(c) }

        /**
         * Per-syllable tone constraints for [reading]: for each syllable, the tone
         * digit if it ended in an explicit tone mark, else `null` (a wildcard —
         * an un-toned syllable, e.g. still being typed or a bare 1st-tone entry,
         * constrains nothing). Spaces are ignored so both "ㄋㄧˇ ㄏㄠˇ" and
         * "ㄋㄧˇㄏㄠˇ" pattern to [3, 3].
         */
        private fun tonePattern(reading: String): List<Char?> {
            val compact = buildString { for (c in reading) if (c != ' ' && c != '\t') append(c) }
            if (compact.isEmpty()) return emptyList()
            return ZhuyinSyllable.segment(compact).map { syl -> toneDigit(syl.last()) }
        }

        /**
         * 1 if [sigSet] has a signature consistent with every explicitly-typed
         * tone in [pattern] (wildcards impose nothing), else 0. Only the first
         * `pattern.size` syllables are checked, so a prefix query still scores a
         * longer word by its leading syllables.
         */
        private fun scoreOf(pattern: List<Char?>, sigSet: Array<String>): Int {
            for (sig in sigSet) {
                var ok = true
                for (i in pattern.indices) {
                    val p = pattern[i] ?: continue
                    if (i >= sig.length || sig[i] != p) { ok = false; break }
                }
                if (ok) return 1
            }
            return 0
        }

        /**
         * Build from `key\tword1 word2 …` lines, optionally with a third
         * tab-separated column `sig1 sig2 …` parallel to the words (each `sigN`
         * a `/`-joined tone-signature set). Lines without the third column parse
         * with empty signature sets (tone-insensitive). Input is expected already
         * sorted by key; pass [presorted] = false to sort here.
         */
        fun fromLines(lines: Sequence<String>, presorted: Boolean = true): ZhuyinDictionary {
            val ks = ArrayList<String>()
            val vs = ArrayList<Array<String>>()
            val ss = ArrayList<Array<Array<String>>>()
            val noSig = emptyArray<String>()
            for (line in lines) {
                if (line.isEmpty()) continue
                val firstTab = line.indexOf('\t')
                if (firstTab <= 0) continue
                val secondTab = line.indexOf('\t', firstTab + 1)
                val key = line.substring(0, firstTab)
                val wordsStr = if (secondTab < 0) line.substring(firstTab + 1)
                else line.substring(firstTab + 1, secondTab)
                val words = wordsStr.split(' ').toTypedArray()
                val wordSigs = if (secondTab < 0) {
                    Array(words.size) { noSig }
                } else {
                    val sigStr = line.substring(secondTab + 1)
                    val groups = sigStr.split(' ')
                    Array(words.size) { idx ->
                        if (idx < groups.size && groups[idx].isNotEmpty())
                            groups[idx].split('/').toTypedArray()
                        else noSig
                    }
                }
                ks.add(key); vs.add(words); ss.add(wordSigs)
            }
            if (!presorted) {
                val idx = ks.indices.sortedBy { ks[it] }
                return ZhuyinDictionary(
                    Array(idx.size) { ks[idx[it]] },
                    Array(idx.size) { vs[idx[it]] },
                    Array(idx.size) { ss[idx[it]] },
                )
            }
            return ZhuyinDictionary(ks.toTypedArray(), vs.toTypedArray(), ss.toTypedArray())
        }
    }
}
