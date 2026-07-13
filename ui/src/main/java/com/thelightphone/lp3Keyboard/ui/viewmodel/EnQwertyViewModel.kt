package com.thelightphone.lp3Keyboard.ui.viewmodel

import com.thelightphone.lp3Keyboard.ui.LayoutOptions
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardSwipeCallback
import com.thelightphone.lp3Keyboard.ui.layout.EnQwerty
import com.thelightphone.lp3Keyboard.ui.layout.Layout

class EnQwertyLp3KeyboardViewModel<SwipeResult>(
    passedCallback: Lp3RepeatableKeyboardCallback,
    swipeCallback: Lp3KeyboardSwipeCallback<SwipeResult>,
    haptic: () -> Unit = {},
    optionsForLayout: (Layout) -> LayoutOptions = {
        LayoutOptions(
            displayCloseButton = true
        )
    },
) : EnBaseViewModel<SwipeResult>(
    passedCallback = passedCallback,
    swipeCallback = swipeCallback,
    haptic = haptic,
    optionsForLayout = optionsForLayout,
    initialLayout = EnQwerty.LowerCaseLayout,
    lowerCaseLayout = EnQwerty.LowerCaseLayout,
    upperCaseLayout = EnQwerty.UpperCaseLayout,
    capsLockedLayout = EnQwerty.CapsLockedLayout,
)
