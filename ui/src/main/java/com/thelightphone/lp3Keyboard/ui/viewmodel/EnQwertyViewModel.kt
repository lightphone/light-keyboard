package com.thelightphone.lp3Keyboard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thelightphone.lp3Keyboard.ui.KeyboardOptions
import com.thelightphone.lp3Keyboard.ui.LayoutOptions
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardSwipeCallback
import com.thelightphone.lp3Keyboard.ui.SpecialKey
import com.thelightphone.lp3Keyboard.ui.SpecialKey.Close
import com.thelightphone.lp3Keyboard.ui.layout.Layout
import com.thelightphone.lp3Keyboard.ui.layout.EnQwerty
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class EnQwertyLp3KeyboardViewModel<SwipeResult>(
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
    initialLayout = EnQwerty.LowerCaseLayout,
    lowerCaseLayout = EnQwerty.LowerCaseLayout,
    upperCaseLayout = EnQwerty.UpperCaseLayout,
    capsLockedLayout = EnQwerty.CapsLockedLayout,
) { }
