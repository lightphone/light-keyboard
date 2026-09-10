package com.thelightphone.lp3Keyboard.ui.viewmodel

import com.thelightphone.lp3Keyboard.ui.KeyboardOptions
import com.thelightphone.lp3Keyboard.ui.LayoutOptions
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardSwipeCallback
import com.thelightphone.lp3Keyboard.ui.layout.Layout
import com.thelightphone.lp3Keyboard.ui.layout.NumericPadLayout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NumericLp3KeyboardViewModel<SwipeResult>(
    passedCallback: Lp3RepeatableKeyboardCallback,
    swipeCallback: Lp3KeyboardSwipeCallback<SwipeResult>? = null,
    haptic: () -> Unit = {},
    optionsForLayout: (Layout) -> LayoutOptions = {
        // No close chevron — the pad has no sub-layouts to fall back to and
        // the IME dismisses via back. (Embedded consumers pass their own.)
        LayoutOptions(displayCloseButton = false)
    },
    keyboardOptionsFlow: StateFlow<KeyboardOptions> = MutableStateFlow(
        KeyboardOptions(
            defaultEmojis,
            displayReturn = true,
            displayVoice = true,
            enableKeyAnimation = true,
            swipeEnabled = false
        )
    )
) : EnBaseViewModel<SwipeResult>(
    passedCallback = passedCallback,
    swipeCallback = swipeCallback,
    haptic = haptic,
    optionsForLayout = optionsForLayout,
    keyboardOptionsFlow = keyboardOptionsFlow,
    // digits have no case — every case slot shows the same layout
    initialLayout = NumericPadLayout,
    lowerCaseLayout = NumericPadLayout,
    upperCaseLayout = NumericPadLayout,
    capsLockedLayout = NumericPadLayout,
)
