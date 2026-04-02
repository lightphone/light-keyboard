package com.thelightphone.lp3keyboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thelightphone.lp3Keyboard.ui.CapsMode
import com.thelightphone.lp3Keyboard.ui.KeyboardOptions
import com.thelightphone.lp3Keyboard.ui.Layout
import com.thelightphone.lp3Keyboard.ui.LowerCaseLayout
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardCallback
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardViewModel
import com.thelightphone.lp3Keyboard.ui.SpecialKey
import com.thelightphone.lp3Keyboard.ui.UpperCaseLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface Lp3RepeatableKeyboardCallback : Lp3KeyboardCallback {
    fun onKeyRepeated(code: Int)
    fun onSpecialKeyRepeated(specialKey: SpecialKey)
}

class DefaultLp3KeyboardViewModel(private val delegateCallback: Lp3RepeatableKeyboardCallback) : ViewModel(),
    Lp3KeyboardViewModel {
    override val layoutFlow: MutableStateFlow<Layout> = MutableStateFlow(LowerCaseLayout)
    override val optionsFlow: StateFlow<KeyboardOptions> = MutableStateFlow(
        KeyboardOptions(
            emptyList(),
            true,
            true
        )
    )

    companion object {
        private const val REPEAT_INTERVAL_MS = 200L
    }

    private val heldSpecialKeys = mutableMapOf<SpecialKey, Job>()
    private val heldKeys = mutableMapOf<Int, Job>()

    var capsMode: CapsMode = CapsMode.Off
        private set

    private fun applyLayout() {
        layoutFlow.value = when (capsMode) {
            CapsMode.Off -> LowerCaseLayout
            CapsMode.Single -> UpperCaseLayout(capsLocked = false)
            CapsMode.Locked -> UpperCaseLayout(capsLocked = true)
        }
    }

    override fun onKeyPressed(code: Int) = delegateCallback.onKeyPressed(code)

    override fun onSpecialKeyPressed(key: SpecialKey) {
        delegateCallback.onSpecialKeyPressed(key)
    }

    override fun onKeyReleased(code: Int) {
        heldKeys.remove(code)?.cancel()
        delegateCallback.onKeyReleased(code)
    }

    override fun onSpecialKeyReleased(key: SpecialKey) {
        val repeatJob = heldSpecialKeys.remove(key)
        repeatJob?.cancel()
        when (key) {
            SpecialKey.Caps -> {
                // if we were long-pressing, swallow the release
                if (repeatJob != null) return
                capsMode = when (capsMode) {
                    CapsMode.Off -> CapsMode.Single
                    CapsMode.Single, CapsMode.Locked -> CapsMode.Off
                }
                applyLayout()
            }
            else -> {}
        }
        delegateCallback.onSpecialKeyReleased(key)
    }

    /** Called by IME after each character to handle system-requested caps. */
    fun setCapsMode(enabled: Boolean) {
        if (capsMode == CapsMode.Locked) return
        capsMode = if (enabled) CapsMode.Single else CapsMode.Off
        applyLayout()
    }

    override fun onKeyLongPressed(code: Int) {
        delegateCallback.onKeyLongPressed(code)
        heldKeys[code]?.cancel()
        heldKeys[code] = viewModelScope.launch {
            while (true) {
                delay(REPEAT_INTERVAL_MS)
                delegateCallback.onKeyRepeated(code)
            }
        }
    }

    override fun onSpecialKeyLongPressed(key: SpecialKey) {
        when (key) {
            SpecialKey.Caps -> {
                capsMode = CapsMode.Locked
                applyLayout()
            }
            else -> {}
        }
        delegateCallback.onSpecialKeyLongPressed(key)
        heldSpecialKeys[key]?.cancel()
        heldSpecialKeys[key] = viewModelScope.launch {
            while (true) {
                delay(REPEAT_INTERVAL_MS)
                delegateCallback.onSpecialKeyRepeated(key)
            }
        }
    }
}