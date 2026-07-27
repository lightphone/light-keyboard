package com.thelightphone.lp3Keyboard.ui

import com.thelightphone.lp3Keyboard.ui.layout.ArShared
import com.thelightphone.lp3Keyboard.ui.layout.ArStandard
import com.thelightphone.lp3Keyboard.ui.viewmodel.ArStandardLp3KeyboardViewModel
import com.thelightphone.lp3Keyboard.ui.viewmodel.CapsMode
import com.thelightphone.lp3Keyboard.ui.viewmodel.Lp3RepeatableKeyboardCallback
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ArStandardLayoutTest {

    private val callback = mockk<Lp3RepeatableKeyboardCallback>(relaxed = true)
    private val swipeCallback = mockk<Lp3KeyboardSwipeCallback<Unit>>(relaxed = true)

    private val vm = ArStandardLp3KeyboardViewModel(
        passedCallback = callback,
        swipeCallback = swipeCallback,
    )

    private val rows = listOf(ArStandard.FIRST_ROW, ArStandard.SECOND_ROW, ArStandard.THIRD_ROW)

    @Test
    fun `the three rows hold every letter of the alphabet exactly once`() {
        val keys = rows.flatMap { it.toList() }
        assertEquals(
            "no letter may appear on two keys",
            keys.size,
            keys.toSet().size
        )
        assertEquals(ArStandard.ALPHABET.toSortedSet(), keys.toSortedSet())
    }

    @Test
    fun `rows fit the key grid`() {
        // 10 standard keys is the widest a row can be; the third row also carries backspace.
        assertEquals(10, ArStandard.FIRST_ROW.length)
        assertEquals(10, ArStandard.SECOND_ROW.length)
        assertEquals(8, ArStandard.THIRD_ROW.length)
    }

    @Test
    fun `swipe decoding is offered the whole alphabet`() {
        val capture = ArStandard.LettersLayout.swipeConfig as Lp3KeyboardLayoutCapture
        assertEquals(ArStandard.ALPHABET, capture.letters)
    }

    @Test
    fun `long-press alternates hang off keys that are actually on the keyboard`() {
        val onScreen = rows.flatMap { it.toList() }.map { it.code }.toSet()
        val missing = ArShared.extendedCharMapping.keys.filterNot { it in onScreen }
        assertTrue("no key on the layout for ${missing.map { it.toChar() }}", missing.isEmpty())
    }

    @Test
    fun `layer switching uses the Arabic number and symbol layouts`() {
        vm.onSpecialKeyReleased(SpecialKey.Numbers)
        assertSame(ArShared.NumberLayout, vm.layoutFlow.value)

        vm.onSpecialKeyReleased(SpecialKey.Symbols)
        assertSame(ArShared.SymbolsLayout, vm.layoutFlow.value)

        vm.onSpecialKeyReleased(SpecialKey.Letters)
        assertSame(ArStandard.LettersLayout, vm.layoutFlow.value)
    }

    @Test
    fun `caps is inert on a caseless layout`() {
        vm.onSpecialKeyReleased(SpecialKey.UpCase)
        assertEquals(CapsMode.Single, vm.capsMode)
        assertSame(ArStandard.LettersLayout, vm.layoutFlow.value)

        vm.onKeyPressed('ا'.code)
        vm.onKeyReleased('ا'.code)

        verify(exactly = 1) { callback.onKeyReleased('ا'.code) }
        assertEquals(CapsMode.Off, vm.capsMode)
        assertSame(ArStandard.LettersLayout, vm.layoutFlow.value)
    }
}
