package com.thelightphone.lp3Keyboard.ui

import com.thelightphone.lp3Keyboard.ui.layout.EnQwerty
import com.thelightphone.lp3Keyboard.ui.viewmodel.CapsMode
import com.thelightphone.lp3Keyboard.ui.viewmodel.EnQwertyLp3KeyboardViewModel
import com.thelightphone.lp3Keyboard.ui.viewmodel.Lp3RepeatableKeyboardCallback
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class EnQwertyViewModelTest {

    private val callback = mockk<Lp3RepeatableKeyboardCallback>(relaxed = true)
    private val swipeCallback = mockk<Lp3KeyboardSwipeCallback<Unit>>(relaxed = true)

    private val vm = EnQwertyLp3KeyboardViewModel(
        passedCallback = callback,
        swipeCallback = swipeCallback,
    )

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
}
