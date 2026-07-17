package com.thelightphone.lp3Keyboard.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import com.thelightphone.lp3Keyboard.ui.layout.EnQwerty
import com.thelightphone.lp3Keyboard.ui.layout.Layout
import com.thelightphone.lp3Keyboard.ui.viewmodel.Lp3KeyboardViewModel
import com.thelightphone.lp3Keyboard.ui.viewmodel.defaultEmojis

open class Lp3RawKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {
    var displayEmojis: Boolean by mutableStateOf(false)
    var callback: Lp3KeyboardCallback? by mutableStateOf(null)
    var swipeCallback: Lp3KeyboardSwipeCallback<*>? by mutableStateOf(null)
    var displayReturn: Boolean by mutableStateOf(false)
    var displayVoice: Boolean by mutableStateOf(false)
    var enableKeyAnimation: Boolean by mutableStateOf(true)
    var swipeEnabled: Boolean by mutableStateOf(true)
    var emojis: List<Emoji>? by mutableStateOf(defaultEmojis)
    var layout: Layout by mutableStateOf(EnQwerty.LowerCaseLayout)
    var darkMode: Boolean by mutableStateOf(true)
    var handleHardwareKeyboardInput: Boolean by mutableStateOf(true)

    @Composable
    override fun Content() {
        val cb = callback ?: return
        Lp3KeyboardTheme(if (darkMode) DarkKeyboardColors else LightKeyboardColors) {
            Box(
                modifier = Modifier.then(
                    if (handleHardwareKeyboardInput) {
                        Modifier.hardwareKeyboardInput(cb)
                    } else {
                        Modifier
                    }
                )
            ) {
                Lp3Keyboard(
                    this@Lp3RawKeyboardView.layout,
                    KeyboardOptions(
                        emojis = if (displayEmojis) this@Lp3RawKeyboardView.emojis else emptyList(),
                        displayReturn = this@Lp3RawKeyboardView.displayReturn,
                        displayVoice = this@Lp3RawKeyboardView.displayVoice,
                        enableKeyAnimation = this@Lp3RawKeyboardView.enableKeyAnimation,
                        swipeEnabled = this@Lp3RawKeyboardView.swipeEnabled
                    ),
                    cb,
                    swipeCallback
                )
            }
        }
    }
}

class Lp3KeyboardView<T>(context: Context, private val viewModel: Lp3KeyboardViewModel<T>) :
    AbstractComposeView(context) {
    var darkMode: Boolean by mutableStateOf(true)
    var handleHardwareKeyboardInput: Boolean by mutableStateOf(true)

    @Composable
    override fun Content() {
        Lp3KeyboardTheme(if (darkMode) DarkKeyboardColors else LightKeyboardColors) {
            Lp3KeyboardWrapper(viewModel, handleHardwareKeyboardInput)
        }
    }
}
