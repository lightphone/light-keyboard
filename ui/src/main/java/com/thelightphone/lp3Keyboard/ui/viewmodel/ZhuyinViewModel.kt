package com.thelightphone.lp3Keyboard.ui.viewmodel

import com.thelightphone.lp3Keyboard.ui.KeyboardOptions
import com.thelightphone.lp3Keyboard.ui.LayoutOptions
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardSwipeCallback
import com.thelightphone.lp3Keyboard.ui.SpecialKey
import com.thelightphone.lp3Keyboard.ui.composer.ComposerCandidate
import com.thelightphone.lp3Keyboard.ui.composer.ComposerHost
import com.thelightphone.lp3Keyboard.ui.composer.ComposerState
import com.thelightphone.lp3Keyboard.ui.composer.ImeComposingActions
import com.thelightphone.lp3Keyboard.ui.layout.EnQwerty
import com.thelightphone.lp3Keyboard.ui.layout.Layout
import com.thelightphone.lp3Keyboard.ui.layout.ZhuyinLayout
import com.thelightphone.lp3Keyboard.ui.zhuyin.CandidateSource
import com.thelightphone.lp3Keyboard.ui.zhuyin.StubCandidateSource
import com.thelightphone.lp3Keyboard.ui.zhuyin.ZhuyinComposer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 注音 (bopomofo) keyboard.
 *
 * Bopomofo is a phonetic script: keys build up a *reading* which the user then
 * resolves to a Han character from the candidate bar, so key presses feed a
 * composition buffer instead of committing text directly. Everything else —
 * numbers, symbols, emoji, extended characters, key repeat — is inherited
 * unchanged from [EnBaseViewModel].
 *
 * The alphabet layouts stay English so the "EN" key round-trips to QWERTY and
 * back (long-press "123"), which is how CJK keyboards normally handle Latin
 * input without a separate keyboard switch.
 *
 * Candidate data comes from the injected [CandidateSource]; the default stub
 * carries a handful of readings so previews and tests need no assets.
 */
class ZhuyinLp3KeyboardViewModel<SwipeResult>(
    private val passedCallback: Lp3RepeatableKeyboardCallback,
    swipeCallback: Lp3KeyboardSwipeCallback<SwipeResult>? = null,
    private val haptic: () -> Unit = {},
    candidateSource: CandidateSource = StubCandidateSource(),
    optionsForLayout: (Layout) -> LayoutOptions = {
        LayoutOptions(
            displayCloseButton = true
        )
    },
    keyboardOptionsFlow: StateFlow<KeyboardOptions> = MutableStateFlow(
        KeyboardOptions(
            defaultEmojis,
            displayReturn = true,
            displayVoice = true,
            enableKeyAnimation = true,
            swipeEnabled = false
        )
    )
) : EnBaseViewModel<SwipeResult>(
    passedCallback = passedCallback,
    swipeCallback = swipeCallback,
    haptic = haptic,
    optionsForLayout = optionsForLayout,
    keyboardOptionsFlow = keyboardOptionsFlow,
    initialLayout = ZhuyinLayout,
    lowerCaseLayout = EnQwerty.LowerCaseLayout,
    upperCaseLayout = EnQwerty.UpperCaseLayout,
    capsLockedLayout = EnQwerty.CapsLockedLayout,
), ComposerHost {

    private val composer = ZhuyinComposer(candidateSource)
    private val _composerState = MutableStateFlow(ComposerState.EMPTY)
    override val composerStateFlow: StateFlow<ComposerState> = _composerState

    /** Pre-edit/commit round-trip lives in the IME; null when embedded elsewhere. */
    private val imeActions: ImeComposingActions?
        get() = passedCallback as? ImeComposingActions

    /** Push the current buffer to the candidate bar and the IME pre-edit region. */
    private fun syncComposer() {
        val snapshot = composer.snapshot()
        _composerState.value = snapshot
        imeActions?.onComposingChanged(snapshot.composing)
    }

    /** Abandon any in-progress composition (used when leaving [ZhuyinLayout]). */
    private fun resetComposer() {
        if (composer.isEmpty) return
        composer.clear()
        _composerState.value = ComposerState.EMPTY
        imeActions?.onComposingChanged("")
    }

    override fun setLayout(layout: Layout) {
        // Leaving 注音 drops any half-built composition rather than stranding an
        // underlined pre-edit region behind the new layout.
        if (layout != ZhuyinLayout) resetComposer()
        super.setLayout(layout)
    }

    override fun onCandidateSelected(candidate: ComposerCandidate) {
        haptic()
        imeActions?.onCommitCandidate(candidate.text)
        // Consume only the reading this candidate covered; any trailing syllables
        // stay composing so the user can keep picking (你 then 好, not 你好好).
        val next = composer.commit(candidate.consumed)
        _composerState.value = next
        imeActions?.onComposingChanged(next.composing)
    }

    override fun onKeyReleased(code: Int) {
        // Bopomofo and tone keys feed the composer instead of committing the
        // glyph; every other key (and every other layout) behaves normally.
        if (layoutFlow.value == ZhuyinLayout && ZhuyinComposer.isZhuyinSymbol(code)) {
            composer.append(code.toChar())
            syncComposer()
            return
        }
        super.onKeyReleased(code)
    }

    /** Set by the long-press that returns to 注音, so the release is swallowed. */
    private var consumeNumbersRelease = false

    override fun onSpecialKeyReleased(key: SpecialKey) {
        if (key == SpecialKey.Numbers && consumeNumbersRelease) {
            consumeNumbersRelease = false
            return
        }
        // While composing, backspace pops the bopomofo buffer; once it's empty
        // (or we're not composing) fall through to the normal field backspace.
        if (key == SpecialKey.Backspace &&
            layoutFlow.value == ZhuyinLayout &&
            composer.backspace()
        ) {
            syncComposer()
            return
        }
        super.onSpecialKeyReleased(key)
    }

    override fun onSpecialKeyLongPressed(key: SpecialKey) {
        // Long-press "123" to come back to 注音 from the English layouts: the
        // bottom row has no width budget for a dedicated toggle, and "EN" is
        // already the outbound half of the pair.
        if (key == SpecialKey.Numbers && layoutFlow.value != ZhuyinLayout) {
            haptic()
            setLayout(ZhuyinLayout)
            consumeNumbersRelease = true
            return
        }
        super.onSpecialKeyLongPressed(key)
    }
}
