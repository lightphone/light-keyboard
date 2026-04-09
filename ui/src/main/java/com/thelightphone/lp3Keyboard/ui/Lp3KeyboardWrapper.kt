package com.thelightphone.lp3Keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/*
For using the keyboard outside LightOS. LightOS adds additional UI surrounding the keyboard
that technically controls it. For example, when the Emoji keyboard is showing, LightOS inserts a
"close" button at the bottom that sets the keyboard back to "letters" when pressed. The Wrapper
composables provide a place to re-create that behavior when using this as a system keyboard.
Eventually, we will replace the custom UI in LightOS with this, so we have a single source of truth
 */

@Composable
fun Lp3KeyboardWrapper(viewModel: Lp3KeyboardViewModel) {
    val layout by viewModel.layoutFlow.collectAsState()
    val options by viewModel.optionsFlow.collectAsState()
    Lp3KeyboardWrapper(layout, options, viewModel)
}

@Composable
fun Lp3KeyboardWrapper(layout: Layout, options: KeyboardOptions, callback: Lp3KeyboardCallback) {
    val colors = LocalKeyboardColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height((LP3_KEYBOARD_HEIGHT_DP + 36).dp)
            .background(colors.background)
            .padding(top = 10.dp)
    ) {
        Lp3Keyboard(layout, options, callback)
        Row(
            Modifier.weight(1f).fillMaxWidth().background(colors.background),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { callback.onSpecialKeyReleased(SpecialKey.Close) },
                contentPadding = PaddingValues(bottom = 14.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = colors.foreground,
                )
            ) {
                Icon(
                    painterResource(R.drawable.down_lp3),
                    "Close"
                )
            }
        }
    }
}