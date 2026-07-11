package com.thelightphone.lp3Keyboard.ui.viewmodel

import com.thelightphone.lp3Keyboard.ui.LayoutOptions
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardSwipeCallback
import com.thelightphone.lp3Keyboard.ui.layout.EnColemak
import com.thelightphone.lp3Keyboard.ui.layout.Layout

class EnColemakLp3KeyboardViewModel<SwipeResult>(
    private val passedCallback: Lp3RepeatableKeyboardCallback,
    private val swipeCallback: Lp3KeyboardSwipeCallback<SwipeResult>,
    private val haptic: () -> Unit = {},
    private val optionsForLayout: (Layout) -> LayoutOptions = {
        LayoutOptions(
            displayCloseButton = true
        )
    },
) : EnBaseViewModel<SwipeResult>(
    passedCallback = passedCallback,
    swipeCallback = swipeCallback,
    haptic = haptic,
    optionsForLayout = optionsForLayout,
    initialLayout = EnColemak.LowerCaseLayout,
    lowerCaseLayout = EnColemak.LowerCaseLayout,
    upperCaseLayout = EnColemak.UpperCaseLayout,
    capsLockedLayout = EnColemak.CapsLockedLayout,
) { }
