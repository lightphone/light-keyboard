package com.thelightphone.lp3Keyboard.ui.composer

import kotlinx.coroutines.flow.StateFlow

/**
 * The seam between a composing input method and the rest of the keyboard.
 *
 * Nothing here is specific to one script: any layout that builds text from a
 * phonetic reading and offers candidates for it (bopomofo, kana, pinyin, …)
 * can implement these and reuse the candidate bar and the service-side
 * InputConnection plumbing unchanged. The script-specific parts — how a
 * reading is spelled, segmented and looked up — stay in that layout's own
 * package.
 */

/**
 * A single tappable candidate: the [text] to commit and how many characters of
 * the raw composing buffer picking it consumes ([consumed]). For a single
 * syllable that's the whole buffer; for a segmented multi-syllable buffer a
 * first-syllable candidate consumes only that syllable and leaves the rest
 * composing, which is what lets the user build phrases the dictionary doesn't
 * know by committing one reading at a time.
 */
data class ComposerCandidate(val text: String, val consumed: Int)

/**
 * Immutable snapshot the UI renders: the raw reading the user is building
 * ([composing], shown as underlined pre-edit text in the field) and the ranked
 * [candidates] for it (shown in the candidate bar).
 */
data class ComposerState(
    val composing: String,
    val candidates: List<ComposerCandidate>,
) {
    /** Whether a composition is in progress — drives whether the bar shows. */
    val isActive: Boolean get() = composing.isNotEmpty()

    companion object {
        val EMPTY = ComposerState("", emptyList())

        /**
         * Preview/test helper: build a state whose candidates each consume the
         * whole [composing] buffer (the common single-syllable case).
         */
        fun of(composing: String, texts: List<String>): ComposerState =
            ComposerState(composing, texts.map { ComposerCandidate(it, composing.length) })
    }
}

/**
 * Implemented by a view model that supports composition, so the candidate-bar
 * UI can observe state and report taps without knowing the concrete view model
 * type.
 */
interface ComposerHost {
    val composerStateFlow: StateFlow<ComposerState>

    /** User tapped a candidate in the bar. */
    fun onCandidateSelected(candidate: ComposerCandidate)
}

/**
 * Implemented by the InputMethodService. The view model drives the actual
 * InputConnection pre-edit / commit through this, so composition state stays in
 * the view model and only the IC calls live in the service.
 */
interface ImeComposingActions {
    /** Set (or, on empty text, finish) the underlined composing region. */
    fun onComposingChanged(composing: CharSequence)

    /** Commit a chosen candidate and end the current composition. */
    fun onCommitCandidate(text: CharSequence)
}
