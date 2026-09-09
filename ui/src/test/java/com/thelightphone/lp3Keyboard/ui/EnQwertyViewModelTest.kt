package com.thelightphone.lp3Keyboard.ui

import com.thelightphone.lp3Keyboard.ui.layout.EnQwerty
import com.thelightphone.lp3Keyboard.ui.layout.EnShared
import com.thelightphone.lp3Keyboard.ui.viewmodel.CapsMode
import com.thelightphone.lp3Keyboard.ui.viewmodel.EnQwertyLp3KeyboardViewModel
import com.thelightphone.lp3Keyboard.ui.viewmodel.Lp3RepeatableKeyboardCallback
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EnQwertyViewModelTest {

    private val callback = mockk<Lp3RepeatableKeyboardCallback>(relaxed = true)
    private val swipeCallback = mockk<Lp3KeyboardSwipeCallback<Unit>>(relaxed = true)

    private val vm = EnQwertyLp3KeyboardViewModel(
        passedCallback = callback,
        swipeCallback = swipeCallback,
    )

    @Before
    fun setUp() {
        // onKeyLongPressed launches a coroutine on viewModelScope, which needs a Main
        // dispatcher installed even for the ones that complete without suspending.
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun tapShift() = vm.apply{
        onSpecialKeyPressed(SpecialKey.UpCase)
        onSpecialKeyReleased(SpecialKey.UpCase)
    }

    @Test
    fun `onKeyPressed does not swap layout mid-gesture in one-shot caps`() {
        tapShift()
        assertEquals(CapsMode.Single, vm.capsMode)
        assertSame(EnQwerty.UpperCaseLayout, vm.layoutFlow.value)

        vm.onKeyPressed('Q'.code)
        assertSame(
            "onKeyPressed must not swap layoutFlow while a key is held down",
            EnQwerty.UpperCaseLayout,
            vm.layoutFlow.value
        )
    }

    @Test
    fun `single-shift then letter commits the capital and reverts to lowercase`() {
        tapShift()

        // Full press -> release gesture on the capital key.
        vm.onKeyPressed('Q'.code)
        vm.onKeyReleased('Q'.code)

        // The release is what commits the character downstream in the IME.
        verify(exactly = 1) { callback.onKeyReleased('Q'.code) }
        assertEquals(CapsMode.Off, vm.capsMode)
        assertSame(EnQwerty.LowerCaseLayout, vm.layoutFlow.value)
    }

    @Test
    fun `onKeyCancelled on the long-pressed key does not dismiss the extended char keyboard`() {
        // Long-pressing a key with accent variants opens the extended char keyboard.
        vm.onKeyLongPressed('a'.code)
        assertTrue(vm.layoutFlow.value is EnShared.ExtendedCharKeyboard)

        // The layout swap can move the key out from under the finger, which the touch
        // tracker reports as a cancel on that same key code. That should be swallowed,
        // not treated as a reason to dismiss the layout we just opened.
        vm.onKeyCancelled('a'.code)
        assertTrue(
            "extended char keyboard should stay open after a cancel on the key that opened it",
            vm.layoutFlow.value is EnShared.ExtendedCharKeyboard
        )
    }

    @Test
    fun `onKeyCancelled on an unrelated key still dismisses the extended char keyboard`() {
        vm.onKeyLongPressed('a'.code)
        assertTrue(vm.layoutFlow.value is EnShared.ExtendedCharKeyboard)

        // A cancel for a different key (e.g. a tap-cancel on one of the accent options)
        // should still auto-dismiss back to the previous layout.
        vm.onKeyCancelled('à'.code)
        assertSame(EnQwerty.LowerCaseLayout, vm.layoutFlow.value)
    }

    @Test
    fun `apostrophe on symbols layout returns to letters after release`() {
        vm.onSpecialKeyReleased(SpecialKey.Symbols)
        assertSame(EnShared.SymbolsLayout, vm.layoutFlow.value)

        vm.onKeyPressed('\''.code)
        vm.onKeyReleased('\''.code)

        assertSame(EnQwerty.LowerCaseLayout, vm.layoutFlow.value)
        verify(exactly = 1) { callback.onKeyReleased('\''.code) }
    }

    @Test
    fun `apostrophe on numbers layout returns to letters after release`() {
        vm.onSpecialKeyReleased(SpecialKey.Numbers)
        assertSame(EnShared.NumberLayout, vm.layoutFlow.value)

        vm.onKeyPressed('\''.code)
        vm.onKeyReleased('\''.code)

        assertSame(EnQwerty.LowerCaseLayout, vm.layoutFlow.value)
    }

    @Test
    fun `other keys on symbols layout do not auto-return to letters`() {
        vm.onSpecialKeyReleased(SpecialKey.Symbols)
        assertSame(EnShared.SymbolsLayout, vm.layoutFlow.value)

        vm.onKeyPressed('#'.code)
        vm.onKeyReleased('#'.code)

        assertSame(EnShared.SymbolsLayout, vm.layoutFlow.value)
    }

    @Test
    fun `apostrophe while on letters layout does not change layout`() {
        assertSame(EnQwerty.LowerCaseLayout, vm.layoutFlow.value)

        vm.onKeyPressed('\''.code)
        vm.onKeyReleased('\''.code)

        assertSame(EnQwerty.LowerCaseLayout, vm.layoutFlow.value)
    }

    @Test
    fun `space returns to letters from symbols layout and still forwards to callback`() {
        vm.onSpecialKeyReleased(SpecialKey.Symbols)
        assertSame(EnShared.SymbolsLayout, vm.layoutFlow.value)

        vm.onSpecialKeyReleased(SpecialKey.Space)

        assertSame(EnQwerty.LowerCaseLayout, vm.layoutFlow.value)
        verify(exactly = 1) { callback.onSpecialKeyReleased(SpecialKey.Space) }
    }

    @Test
    fun `return key returns to letters from numbers layout and still forwards to callback`() {
        vm.onSpecialKeyReleased(SpecialKey.Numbers)
        assertSame(EnShared.NumberLayout, vm.layoutFlow.value)

        vm.onSpecialKeyReleased(SpecialKey.Return)

        assertSame(EnQwerty.LowerCaseLayout, vm.layoutFlow.value)
        verify(exactly = 1) { callback.onSpecialKeyReleased(SpecialKey.Return) }
    }

    @Test
    fun `space on letters layout does not change layout but still forwards to callback`() {
        assertSame(EnQwerty.LowerCaseLayout, vm.layoutFlow.value)

        vm.onSpecialKeyReleased(SpecialKey.Space)

        assertSame(EnQwerty.LowerCaseLayout, vm.layoutFlow.value)
        verify(exactly = 1) { callback.onSpecialKeyReleased(SpecialKey.Space) }
    }

    @Test
    fun `space on symbols layout restores upper case when single-shifted before switching`() {
        tapShift()
        vm.onSpecialKeyReleased(SpecialKey.Symbols)
        assertSame(EnShared.SymbolsLayout, vm.layoutFlow.value)

        vm.onSpecialKeyReleased(SpecialKey.Space)

        // caps mode carries through the symbols screen, so space should restore uppercase.
        assertSame(EnQwerty.UpperCaseLayout, vm.layoutFlow.value)
    }
}
