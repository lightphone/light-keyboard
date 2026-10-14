package com.thelightphone.lp3Keyboard.ui

import com.thelightphone.lp3Keyboard.ui.layout.FiQwerty
import com.thelightphone.lp3Keyboard.ui.viewmodel.FiQwertyLp3KeyboardViewModel
import com.thelightphone.lp3Keyboard.ui.viewmodel.Lp3RepeatableKeyboardCallback
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertSame
import org.junit.Test

class FiQwertyViewModelTest {

    private val callback = mockk<Lp3RepeatableKeyboardCallback>(relaxed = true)
    private val swipeCallback = mockk<Lp3KeyboardSwipeCallback<Unit>>(relaxed = true)

    private val vm = FiQwertyLp3KeyboardViewModel(
        passedCallback = callback,
        swipeCallback = swipeCallback,
    )

    @Test
    fun `finnish letters commit through the callback in lowercase`() {
        for (char in "åäö") {
            vm.onKeyPressed(char.code)
            vm.onKeyReleased(char.code)
            verify(exactly = 1) { callback.onKeyReleased(char.code) }
        }
        assertSame(FiQwerty.LowerCaseLayout, vm.layoutFlow.value)
    }

    @Test
    fun `shift swaps to the finnish uppercase layout and reverts after one letter`() {
        vm.onSpecialKeyPressed(SpecialKey.UpCase)
        vm.onSpecialKeyReleased(SpecialKey.UpCase)
        assertSame(FiQwerty.UpperCaseLayout, vm.layoutFlow.value)

        vm.onKeyPressed('Ä'.code)
        vm.onKeyReleased('Ä'.code)
        verify(exactly = 1) { callback.onKeyReleased('Ä'.code) }
        assertSame(FiQwerty.LowerCaseLayout, vm.layoutFlow.value)
    }
}
