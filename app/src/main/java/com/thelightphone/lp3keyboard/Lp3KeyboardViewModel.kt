package com.thelightphone.lp3keyboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thelightphone.lp3Keyboard.ui.CapsLockedLayout
import com.thelightphone.lp3Keyboard.ui.CapsMode
import com.thelightphone.lp3Keyboard.ui.EmojiLayout
import com.thelightphone.lp3Keyboard.ui.KeyboardOptions
import com.thelightphone.lp3Keyboard.ui.Layout
import com.thelightphone.lp3Keyboard.ui.LowerCaseLayout
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardCallback
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardViewModel
import com.thelightphone.lp3Keyboard.ui.NumberLayout
import com.thelightphone.lp3Keyboard.ui.ExtendedCharKeyboard
import com.thelightphone.lp3Keyboard.ui.SpecialKey
import com.thelightphone.lp3Keyboard.ui.SymbolsLayout
import com.thelightphone.lp3Keyboard.ui.UpperCaseLayout
import com.thelightphone.lp3Keyboard.ui.defaultEmojis
import com.thelightphone.lp3Keyboard.ui.extendedCharMapping
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface Lp3RepeatableKeyboardCallback : Lp3KeyboardCallback {
    fun onKeyRepeated(code: Int)
    fun onSpecialKeyRepeated(specialKey: SpecialKey)
}

class DefaultLp3KeyboardViewModel(
    private val delegateCallback: Lp3RepeatableKeyboardCallback,
    private val haptic: () -> Unit = {}
) : ViewModel(),
    Lp3KeyboardViewModel {
    var previousLayout: Layout? = null
        private set

    override val layoutFlow: MutableStateFlow<Layout> = MutableStateFlow(LowerCaseLayout)

    private fun setLayout(layout: Layout) {
        previousLayout = layoutFlow.value
        layoutFlow.value = layout
    }
    override val optionsFlow: StateFlow<KeyboardOptions> = MutableStateFlow(
        KeyboardOptions(
            defaultEmojis,
            true,
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

    private fun showAlphabetLayout() {
        setLayout(when (capsMode) {
            CapsMode.Off -> LowerCaseLayout
            CapsMode.Single -> UpperCaseLayout
            CapsMode.Locked -> CapsLockedLayout
        })
    }

    override fun onKeyPressed(code: Int) {
        haptic()
        delegateCallback.onKeyPressed(code)
    }

    override fun onSpecialKeyPressed(key: SpecialKey) {
        haptic()
        delegateCallback.onSpecialKeyPressed(key)
    }

    override fun onKeyReleased(code: Int) {
        heldKeys.remove(code)?.cancel()
        // auto-dismiss when a special key is typed
        if (layoutFlow.value is ExtendedCharKeyboard) {
            setLayout(previousLayout ?: LowerCaseLayout)
        }
        delegateCallback.onKeyReleased(code)
    }

    override fun onSpecialKeyReleased(key: SpecialKey) {
        val repeatJob = heldSpecialKeys.remove(key)
        repeatJob?.cancel()
        when (key) {
            SpecialKey.UpCase, SpecialKey.DownCase -> {
                // if we were long-pressing, swallow the release
                if (repeatJob != null) return
                capsMode = when (capsMode) {
                    CapsMode.Off -> CapsMode.Single
                    CapsMode.Single, CapsMode.Locked -> CapsMode.Off
                }
                showAlphabetLayout()
            }
            SpecialKey.Numbers -> {
                setLayout(NumberLayout)
            }
            SpecialKey.Letters, SpecialKey.Close -> {
                showAlphabetLayout()
            }
            SpecialKey.Symbols -> {
                setLayout(SymbolsLayout)
            }
            SpecialKey.Emojis -> {
                setLayout(EmojiLayout)
            }
            else -> {/*TODO*/}
        }
        delegateCallback.onSpecialKeyReleased(key)
    }

    /** Called by IME after each character to handle system-requested caps. */
    fun setCapsMode(enabled: Boolean) {
        if (capsMode == CapsMode.Locked) return
        capsMode = if (enabled) CapsMode.Single else CapsMode.Off
        when(layoutFlow.value) {
            // only update the layout if we were already showing letters
            LowerCaseLayout, UpperCaseLayout, CapsLockedLayout -> showAlphabetLayout()
            else -> {}
        }
    }

    override fun onKeyLongPressed(code: Int) {
        heldKeys[code]?.cancel()
        if (extendedCharMapping.containsKey(code)) {
            haptic()
            setLayout(ExtendedCharKeyboard(code))
            return
        }
        delegateCallback.onKeyLongPressed(code)
        heldKeys[code] = viewModelScope.launch {
            while (true) {
                delay(REPEAT_INTERVAL_MS)
                delegateCallback.onKeyRepeated(code)
            }
        }
    }

    override fun onSpecialKeyLongPressed(key: SpecialKey) {
        val allowRepeats = when (key) {
            SpecialKey.UpCase, SpecialKey.DownCase -> {
                capsMode = if (capsMode == CapsMode.Locked) CapsMode.Off else CapsMode.Locked
                showAlphabetLayout()
                // don't allow repeats since we switched layouts and the original button is gone
                false
            }
            else -> true
        }
        haptic()
        delegateCallback.onSpecialKeyLongPressed(key)
        if (allowRepeats) {
            heldSpecialKeys[key]?.cancel()
            heldSpecialKeys[key] = viewModelScope.launch {
                while (true) {
                    delay(REPEAT_INTERVAL_MS)
                    delegateCallback.onSpecialKeyRepeated(key)
                }
            }
        }
    }
}