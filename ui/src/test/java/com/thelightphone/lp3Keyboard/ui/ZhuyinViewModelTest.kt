package com.thelightphone.lp3Keyboard.ui

import com.thelightphone.lp3Keyboard.ui.composer.ImeComposingActions
import com.thelightphone.lp3Keyboard.ui.layout.EnQwerty
import com.thelightphone.lp3Keyboard.ui.layout.EnShared
import com.thelightphone.lp3Keyboard.ui.layout.ZhuyinLayout
import com.thelightphone.lp3Keyboard.ui.viewmodel.Lp3RepeatableKeyboardCallback
import com.thelightphone.lp3Keyboard.ui.viewmodel.ZhuyinLp3KeyboardViewModel
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The composer wiring: bopomofo keys must build a reading instead of committing
 * glyphs, and the English half of the keyboard must behave exactly as it does
 * on the other layouts.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ZhuyinViewModelTest {

    /** Records what the IME would have been told to do. */
    private class FakeIme : Lp3RepeatableKeyboardCallback, ImeComposingActions {
        val committed = mutableListOf<CharSequence>()
        val composing = mutableListOf<CharSequence>()
        val released = mutableListOf<Int>()

        override fun onComposingChanged(composing: CharSequence) {
            this.composing += composing
        }

        override fun onCommitCandidate(text: CharSequence) {
            committed += text
        }

        override fun onKeyReleased(code: Int) {
            released += code
        }

        override fun onKeyPressed(code: Int) {}
        override fun onSpecialKeyPressed(key: SpecialKey) {}
        override fun onSpecialKeyReleased(key: SpecialKey) {}
        override fun onKeyLongPressed(code: Int) {}
        override fun onSpecialKeyLongPressed(key: SpecialKey) {}
        override fun onSubmitWord(word: CharSequence) {}
        override fun onKeyRepeated(code: Int) {}
        override fun onSpecialKeyRepeated(key: SpecialKey) {}
    }

    private val ime = FakeIme()
    private val swipeCallback = mockk<Lp3KeyboardSwipeCallback<Unit>>(relaxed = true)

    private val vm = ZhuyinLp3KeyboardViewModel(
        passedCallback = ime,
        swipeCallback = swipeCallback,
    )

    @Before
    fun setUp() {
        // Long-press paths in the base view model launch on viewModelScope.
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun type(reading: String) = reading.forEach { vm.onKeyReleased(it.code) }

    @Test
    fun `starts on the bopomofo layout`() {
        assertSame(ZhuyinLayout, vm.layoutFlow.value)
    }

    @Test
    fun `bopomofo keys compose instead of committing`() {
        type("ㄋㄧˇ")

        // Nothing reached the field directly...
        assertTrue("bopomofo keys must not commit glyphs", ime.released.isEmpty())
        // ...it went to the pre-edit region and the candidate bar instead.
        assertEquals("ㄋㄧˇ", vm.composerStateFlow.value.composing)
        assertTrue(vm.composerStateFlow.value.isActive)
        assertEquals("ㄋㄧˇ", ime.composing.last())
        assertEquals("你", vm.composerStateFlow.value.candidates.first().text)
    }

    @Test
    fun `selecting a candidate commits it and consumes only its reading`() {
        type("ㄋㄧˇㄏㄠˇ")
        val state = vm.composerStateFlow.value
        // Whole phrase and first syllable are both offered.
        assertTrue(state.candidates.any { it.text == "你好" })
        val first = state.candidates.first { it.text == "你" }

        vm.onCandidateSelected(first)

        assertEquals(listOf<CharSequence>("你"), ime.committed)
        // 好 keeps composing rather than being dropped or re-committed.
        assertEquals("ㄏㄠˇ", vm.composerStateFlow.value.composing)
    }

    @Test
    fun `backspace pops the buffer while composing then falls through`() {
        type("ㄋㄧˇ")

        vm.onSpecialKeyReleased(SpecialKey.Backspace)
        assertEquals("ㄋㄧ", vm.composerStateFlow.value.composing)

        vm.onSpecialKeyReleased(SpecialKey.Backspace)
        vm.onSpecialKeyReleased(SpecialKey.Backspace)
        assertFalse(vm.composerStateFlow.value.isActive)

        // Buffer empty: the next backspace is the field's, not the composer's.
        vm.onSpecialKeyReleased(SpecialKey.Backspace)
        assertFalse(vm.composerStateFlow.value.isActive)
    }

    @Test
    fun `EN switches to English and abandons the composition`() {
        type("ㄋㄧˇ")

        vm.onSpecialKeyReleased(SpecialKey.Letters)

        assertSame(EnQwerty.LowerCaseLayout, vm.layoutFlow.value)
        assertFalse(vm.composerStateFlow.value.isActive)
        assertEquals("", ime.composing.last())
    }

    @Test
    fun `English keys behave normally once switched`() {
        vm.onSpecialKeyReleased(SpecialKey.Letters)

        vm.onKeyReleased('q'.code)

        assertEquals(listOf('q'.code), ime.released)
        assertFalse(vm.composerStateFlow.value.isActive)
    }

    @Test
    fun `long-pressing 123 on the English layout returns to bopomofo`() {
        vm.onSpecialKeyReleased(SpecialKey.Letters)
        assertSame(EnQwerty.LowerCaseLayout, vm.layoutFlow.value)

        vm.onSpecialKeyLongPressed(SpecialKey.Numbers)
        assertSame(ZhuyinLayout, vm.layoutFlow.value)

        // The release that follows the long-press must not also open numbers.
        vm.onSpecialKeyReleased(SpecialKey.Numbers)
        assertSame(ZhuyinLayout, vm.layoutFlow.value)
    }

    @Test
    fun `123 still opens the number layout on a normal tap`() {
        vm.onSpecialKeyReleased(SpecialKey.Numbers)
        assertSame(EnShared.NumberLayout, vm.layoutFlow.value)
    }
}
