package com.thelightphone.lp3Keyboard.ui

import com.thelightphone.lp3Keyboard.ui.layout.DaQwerty
import com.thelightphone.lp3Keyboard.ui.layout.FiQwerty
import com.thelightphone.lp3Keyboard.ui.layout.LayoutRegistryItem
import com.thelightphone.lp3Keyboard.ui.layout.NoQwerty
import com.thelightphone.lp3Keyboard.ui.layout.SvQwerty
import com.thelightphone.lp3Keyboard.ui.layout.buildRootViewModel
import com.thelightphone.lp3Keyboard.ui.viewmodel.Lp3RepeatableKeyboardCallback
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class LayoutRegistryItemTest {

    private val callback = mockk<Lp3RepeatableKeyboardCallback>(relaxed = true)
    private val swipeCallback = mockk<Lp3KeyboardSwipeCallback<Unit>>(relaxed = true)

    @Test
    fun `layout ids are unique`() {
        val ids = LayoutRegistryItem.entries.map { it.uniqueId }
        assertEquals(ids.distinct(), ids)
    }

    @Test
    fun `nordic entries build view models with their own layouts`() {
        val layouts = mapOf(
            LayoutRegistryItem.SvQwerty to (SvQwerty.LowerCaseLayout to SvQwerty.UpperCaseLayout),
            LayoutRegistryItem.FiQwerty to (FiQwerty.LowerCaseLayout to FiQwerty.UpperCaseLayout),
            LayoutRegistryItem.DaQwerty to (DaQwerty.LowerCaseLayout to DaQwerty.UpperCaseLayout),
            LayoutRegistryItem.NoQwerty to (NoQwerty.LowerCaseLayout to NoQwerty.UpperCaseLayout),
        )
        for ((item, expected) in layouts) {
            val vm = item.buildRootViewModel(callback, swipeCallback)
            assertSame(item.name, expected.first, vm.layoutFlow.value)

            vm.onSpecialKeyPressed(SpecialKey.UpCase)
            vm.onSpecialKeyReleased(SpecialKey.UpCase)
            assertSame(item.name, expected.second, vm.layoutFlow.value)
        }
    }
}
