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
    var displayEmojis: Boolean by mutableStateOf(false)
    var callback: Lp3KeyboardCallback? by mutableStateOf(null)
    var displayClose: Boolean by mutableStateOf(false)
    var displayReturn: Boolean by mutableStateOf(false)
    var displayVoice: Boolean by mutableStateOf(false)
    var emojis: List<Emoji>? by mutableStateOf(defaultEmojis)
    var layout: Layout by mutableStateOf(LowerCaseLayout)
    var darkMode: Boolean by mutableStateOf(true)

    @Composable
    override fun Content() {
        val cb = callback ?: return
        Lp3KeyboardTheme(if (darkMode) DarkKeyboardColors else LightKeyboardColors) {
            Lp3Keyboard(
                this@Lp3RawKeyboardView.layout,
                KeyboardOptions(
                    emojis = if (displayEmojis) this@Lp3RawKeyboardView.emojis else emptyList(),
                    displayClose = this@Lp3RawKeyboardView.displayClose,
                    displayReturn = this@Lp3RawKeyboardView.displayReturn,
                    displayVoice = this@Lp3RawKeyboardView.displayVoice
                ),
                cb
            )
        }
    }
}

class Lp3KeyboardView(context: Context, private val viewModel: Lp3KeyboardViewModel) :
    AbstractComposeView(context) {
    var darkMode: Boolean by mutableStateOf(true)

    @Composable
    override fun Content() {
        Lp3KeyboardTheme(if (darkMode) DarkKeyboardColors else LightKeyboardColors) {
            Lp3KeyboardWrapper(viewModel)
        }
    }
}
