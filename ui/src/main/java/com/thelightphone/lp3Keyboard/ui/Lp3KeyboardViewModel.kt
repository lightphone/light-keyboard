package com.thelightphone.lp3Keyboard.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface Lp3KeyboardViewModel : Lp3KeyboardCallback {
    val layoutFlow: StateFlow<Layout>
    val optionsFlow: StateFlow<KeyboardOptions>
}

enum class CapsMode { Off, Single, Locked }

class DefaultLp3KeyboardViewModel(private val delegateCallback: Lp3KeyboardCallback) : ViewModel(),
    Lp3KeyboardViewModel {
    override val layoutFlow: MutableStateFlow<Layout> = MutableStateFlow(LowerCaseLayout)
    override val optionsFlow: StateFlow<KeyboardOptions> = MutableStateFlow(
        KeyboardOptions(
            emptyList(),
            true,
            true
        )
    )

    private val longPressedSpecialKeys = mutableSetOf<SpecialKey>()

    var capsMode: CapsMode = CapsMode.Off
        private set

    private fun applyLayout() {
        layoutFlow.value = if (capsMode == CapsMode.Off) LowerCaseLayout else UpperCaseLayout
    }

    override fun onKeyPressed(code: Int) = delegateCallback.onKeyPressed(code)

    override fun onSpecialKeyPressed(key: SpecialKey) {
        delegateCallback.onSpecialKeyPressed(key)
    }

    override fun onKeyReleased(code: Int) = delegateCallback.onKeyReleased(code)

    override fun onSpecialKeyReleased(key: SpecialKey) {
        val wasLongPressed = longPressedSpecialKeys.remove(key)
        when (key) {
            SpecialKey.Caps -> {
                if (wasLongPressed) return
                capsMode = when (capsMode) {
                    CapsMode.Off -> CapsMode.Single
                    CapsMode.Single, CapsMode.Locked -> CapsMode.Off
                }
                applyLayout()
            }
        }
        delegateCallback.onSpecialKeyReleased(key)
    }

    /** Called by IME after each character to handle system-requested caps. */
    fun setCapsMode(enabled: Boolean) {
        if (capsMode == CapsMode.Locked) return
        capsMode = if (enabled) CapsMode.Single else CapsMode.Off
        applyLayout()
    }

    override fun onKeyLongPressed(code: Int) = delegateCallback.onKeyLongPressed(code)

    override fun onSpecialKeyLongPressed(key: SpecialKey) {
        longPressedSpecialKeys.add(key)
        when (key) {
            SpecialKey.Caps -> {
                capsMode = CapsMode.Locked
                applyLayout()
            }
        }
        delegateCallback.onSpecialKeyLongPressed(key)
    }
}