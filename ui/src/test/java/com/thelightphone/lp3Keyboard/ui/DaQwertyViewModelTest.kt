package com.thelightphone.lp3Keyboard.ui

import com.thelightphone.lp3Keyboard.ui.layout.DaQwerty
import com.thelightphone.lp3Keyboard.ui.viewmodel.DaQwertyLp3KeyboardViewModel
import com.thelightphone.lp3Keyboard.ui.viewmodel.Lp3RepeatableKeyboardCallback
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertSame
import org.junit.Test

class DaQwertyViewModelTest {

    private val callback = mockk<Lp3RepeatableKeyboardCallback>(relaxed = true)
    private val swipeCallback = mockk<Lp3KeyboardSwipeCallback<Unit>>(relaxed = true)

    private val vm = DaQwertyLp3KeyboardViewModel(
        passedCallback = callback,
        swipeCallback = swipeCallback,
    )

    @Test
    fun `danish letters commit through the callback in lowercase`() {
        for (char in "æøå") {
            vm.onKeyPressed(char.code)
            vm.onKeyReleased(char.code)
            verify(exactly = 1) { callback.onKeyReleased(char.code) }
        }
        assertSame(DaQwerty.LowerCaseLayout, vm.layoutFlow.value)
    }

    @Test
    fun `shift swaps to the danish uppercase layout and reverts after one letter`() {
        vm.onSpecialKeyPressed(SpecialKey.UpCase)
        vm.onSpecialKeyReleased(SpecialKey.UpCase)
        assertSame(DaQwerty.UpperCaseLayout, vm.layoutFlow.value)

        vm.onKeyPressed('Æ'.code)
        vm.onKeyReleased('Æ'.code)
        verify(exactly = 1) { callback.onKeyReleased('Æ'.code) }
        assertSame(DaQwerty.LowerCaseLayout, vm.layoutFlow.value)
    }
}
