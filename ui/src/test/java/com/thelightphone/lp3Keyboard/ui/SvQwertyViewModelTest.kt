package com.thelightphone.lp3Keyboard.ui

import com.thelightphone.lp3Keyboard.ui.layout.SvQwerty
import com.thelightphone.lp3Keyboard.ui.viewmodel.Lp3RepeatableKeyboardCallback
import com.thelightphone.lp3Keyboard.ui.viewmodel.SvQwertyLp3KeyboardViewModel
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertSame
import org.junit.Test

class SvQwertyViewModelTest {

    private val callback = mockk<Lp3RepeatableKeyboardCallback>(relaxed = true)
    private val swipeCallback = mockk<Lp3KeyboardSwipeCallback<Unit>>(relaxed = true)

    private val vm = SvQwertyLp3KeyboardViewModel(
        passedCallback = callback,
        swipeCallback = swipeCallback,
    )

    @Test
    fun `swedish letters commit through the callback in lowercase`() {
        for (char in "åäö") {
            vm.onKeyPressed(char.code)
            vm.onKeyReleased(char.code)
            verify(exactly = 1) { callback.onKeyReleased(char.code) }
        }
        assertSame(SvQwerty.LowerCaseLayout, vm.layoutFlow.value)
    }

    @Test
    fun `shift swaps to the swedish uppercase layout and reverts after one letter`() {
        vm.onSpecialKeyPressed(SpecialKey.UpCase)
        vm.onSpecialKeyReleased(SpecialKey.UpCase)
        assertSame(SvQwerty.UpperCaseLayout, vm.layoutFlow.value)

        vm.onKeyPressed('Å'.code)
        vm.onKeyReleased('Å'.code)
        verify(exactly = 1) { callback.onKeyReleased('Å'.code) }
        assertSame(SvQwerty.LowerCaseLayout, vm.layoutFlow.value)
    }
}
