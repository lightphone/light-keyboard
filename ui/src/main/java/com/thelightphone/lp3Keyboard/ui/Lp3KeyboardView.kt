package com.thelightphone.lp3Keyboard.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView

class Lp3RawKeyboardView(
    context: Context,
    private val callback: Lp3KeyboardCallback,
    options: KeyboardOptions = KeyboardOptions(emptyList(), true, true)
) :
    AbstractComposeView(context) {
    var displayClose: Boolean = options.displayClose
    var displayReturn: Boolean = options.displayReturn
    var emojis: List<Emoji>? = options.emojis

    var layout: Layout = LowerCaseLayout

    @Composable
    override fun Content() {
        Lp3Keyboard(
            this.layout,
            KeyboardOptions(
                emojis = this.emojis,
                displayClose = this.displayClose,
                displayReturn = this.displayReturn,
            ),
            callback
        )
    }
}

class Lp3KeyboardView(context: Context, private val viewModel: Lp3KeyboardViewModel) :
    AbstractComposeView(context) {

    @Composable
    override fun Content() {
        Lp3Keyboard(viewModel)
    }
}
