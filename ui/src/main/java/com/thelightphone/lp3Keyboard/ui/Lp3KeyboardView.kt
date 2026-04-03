package com.thelightphone.lp3Keyboard.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView

open class Lp3RawKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {
    var callback: Lp3KeyboardCallback? = null
    var displayClose: Boolean = true
    var displayReturn: Boolean = true
    var emojis: List<Emoji>? = emptyList()
    var layout: Layout = LowerCaseLayout

    @Composable
    override fun Content() {
        val cb = callback ?: return
        Lp3Keyboard(
            this.layout,
            KeyboardOptions(
                emojis = this.emojis,
                displayClose = this.displayClose,
                displayReturn = this.displayReturn,
            ),
            cb
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
