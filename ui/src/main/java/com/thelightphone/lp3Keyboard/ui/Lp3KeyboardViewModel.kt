package com.thelightphone.lp3Keyboard.ui

import kotlinx.coroutines.flow.StateFlow

interface Lp3KeyboardViewModel : Lp3KeyboardCallback {
    val layoutFlow: StateFlow<Layout>
    val optionsFlow: StateFlow<KeyboardOptions>
}

enum class CapsMode { Off, Single, Locked }
