package com.thelightphone.lp3Keyboard.ui

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thelightphone.lp3Keyboard.ui.layout.EnQwerty
import com.thelightphone.lp3Keyboard.ui.layout.EnShared
import com.thelightphone.lp3Keyboard.ui.layout.Layout
import com.thelightphone.lp3Keyboard.ui.viewmodel.Lp3KeyboardViewModel
import com.thelightphone.lp3Keyboard.ui.viewmodel.defaultEmojis

/*
For using the keyboard outside LightOS. LightOS adds additional UI surrounding the keyboard
that technically controls it. For example, when the Emoji keyboard is showing, LightOS inserts a
"close" button at the bottom that sets the keyboard back to "letters" when pressed. The Wrapper
composables provide a place to re-create that behavior when using this as a system keyboard.
Eventually, we will replace the custom UI in LightOS with this, so we have a single source of truth
 */

@Composable
fun Lp3KeyboardWrapper(
    viewModel: Lp3KeyboardViewModel<*>,
    handleHardwareKeyboardInput: Boolean = true,
    remapKeyCode: ((KeyEvent) -> Int)? = ::lightOsRemap
) {
    val layout by viewModel.layoutFlow.collectAsState()
    val keyboardOptions by viewModel.keyboardOptionsFlow.collectAsState()
    val layoutOptions by viewModel.layoutOptionsFlow.collectAsState()
    Lp3KeyboardWrapper(
        layout,
        keyboardOptions,
        layoutOptions,
        viewModel,
        viewModel,
        handleHardwareKeyboardInput,
        remapKeyCode
    )
}

@Composable
fun Lp3KeyboardWrapper(
    layout: Layout,
    keyboardOptions: KeyboardOptions,
    layoutOptions: LayoutOptions,
    callback: Lp3KeyboardCallback,
    swipeCallback: Lp3KeyboardSwipeCallback<*>?,
    handleHardwareKeyboardInput: Boolean = true,
    remapKeyCode: ((KeyEvent) -> Int)? = ::lightOsRemap,
    additionalBottomHeight: Dp = 0.dp,
    bottomBar: (@Composable () -> Unit)? = null,
    onOverlayDismissed: (() -> Unit)? = null,
    overlay: (@Composable () -> Unit)? = null,
) {
    val colors = LocalKeyboardColors.current
    val additionalHeight = maxOf(additionalBottomHeight, 36.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(LP3_KEYBOARD_HEIGHT_DP.dp + additionalHeight)
            .background(colors.background)
            .then(
                if (handleHardwareKeyboardInput) {
                    Modifier.hardwareKeyboardInput(callback, remapKeyCode)
                } else {
                    Modifier
                }
            )
    ) {
        if (overlay != null) {
            Box(Modifier.fillMaxWidth().height(LP3_KEYBOARD_HEIGHT_DP.dp)) {
                overlay()
            }
        } else {
            Spacer(Modifier.height(10.dp))
            Lp3Keyboard(layout, keyboardOptions, callback, swipeCallback)
        }
        Row(
            Modifier.weight(1f).fillMaxWidth().background(colors.background),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            if (layoutOptions.displayCloseButton || overlay != null) {
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (onOverlayDismissed != null) {
                                onOverlayDismissed()
                            } else {
                                callback.onSpecialKeyPressed(SpecialKey.Close)
                                callback.onSpecialKeyReleased(SpecialKey.Close)
                            }
                        }
                        .padding(bottom = 10.dp, top = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.down_lp3),
                        "Close",
                        tint = colors.foreground,
                    )
                }
            } else if (bottomBar != null) {
                bottomBar()
            }
        }
    }
}

@Preview(name = "Wrapper", widthDp = (1080 / 3), heightDp = (1240 / 3))
@Composable
fun Lp3KeyboardWrapperPreview() {
    Lp3KeyboardTheme(DarkKeyboardColors) {
        Column(verticalArrangement = Arrangement.Bottom, modifier = Modifier.fillMaxSize()) {
            val keyboardOptions = KeyboardOptions(
                defaultEmojis,
                displayReturn = true,
                displayVoice = true,
                enableKeyAnimation = true,
                swipeEnabled = true
            )
            val layoutOptions = LayoutOptions(displayCloseButton = true)
            Lp3KeyboardWrapper(
                EnQwerty.UpperCaseLayout,
                keyboardOptions,
                layoutOptions,
                previewCallback,
                null
            )
        }
    }
}
