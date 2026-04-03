package com.thelightphone.lp3Keyboard.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView

open class Lp3RawKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {
    var callback: Lp3KeyboardCallback? by mutableStateOf(null)
    var displayClose: Boolean by mutableStateOf(true)
    var displayReturn: Boolean by mutableStateOf(true)
    var displayVoice: Boolean by mutableStateOf(true)
    var emojis: List<Emoji>? by mutableStateOf(emptyList())
    var layout: Layout by mutableStateOf(LowerCaseLayout)

    @Composable
    override fun Content() {
        val cb = callback ?: return
        Lp3Keyboard(
            this.layout,
            KeyboardOptions(
                emojis = this.emojis,
                displayClose = this.displayClose,
                displayReturn = this.displayReturn,
                displayVoice = this.displayVoice
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
