package com.thelightphone.lp3Keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class SpecialKey {
    Caps
}

interface Lp3KeyboardCallback {
    fun onKeyPressed(code: Int)
    fun onSpecialKeyPressed(key: SpecialKey)
    fun onKeyReleased(code: Int)
    fun onSpecialKeyReleased(key: SpecialKey)
    fun onKeyLongPressed(code: Int)
    fun onSpecialKeyLongPressed(key: SpecialKey)
}

private const val HEIGHT_DP = 180
private const val STANDARD_KEY_WIDTH_DP = 30.5
private const val STANDARD_KEY_TEXT_SP = 24

@Composable
fun Lp3Keyboard(viewModel: Lp3KeyboardViewModel) {
    val layout by viewModel.layoutFlow.collectAsState()
    val options by viewModel.optionsFlow.collectAsState()
    Lp3Keyboard(layout, options, viewModel)
}

@Composable
fun Lp3Keyboard(layout: Layout, options: KeyboardOptions, callback: Lp3KeyboardCallback) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(HEIGHT_DP.dp)
            .background(Color.Black)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(vertical = 24.dp)
        ) {
            with(layout) { Render(options, callback) }
        }
    }
}

@Composable
fun RowScope.Key(
    char: Char,
    callback: Lp3KeyboardCallback,
    override: SpecialKey? = null
) {
    val code = char.code

    val onPressed = override
        ?.let { { callback.onSpecialKeyPressed(it) } }
        ?: { callback.onKeyPressed(code)}

    val onReleased = override
        ?.let { { callback.onSpecialKeyReleased(it) } }
        ?: { callback.onKeyReleased(code)}

    val onLongPressed = override
        ?.let { { callback.onSpecialKeyLongPressed(it) } }
        ?: { callback.onKeyLongPressed(code)}

    Box(
        modifier = Modifier
            .width(STANDARD_KEY_WIDTH_DP.dp)
            .fillMaxHeight()
            .pointerInput(char) {
                detectTapGestures(
                    onPress = {
                        onPressed()
                        tryAwaitRelease()
                        onReleased()
                    },
                    onLongPress = { onLongPressed() },
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = char.toString(),
            color = Color.White,
            fontFamily = akkuratFamily,
            fontWeight = FontWeight.Normal,
            fontSize = STANDARD_KEY_TEXT_SP.sp
        )
    }
}

typealias Emoji = Int

data class KeyboardOptions(
    val emojis: List<Emoji>?,
    val displayClose: Boolean,
    val displayReturn: Boolean
)

sealed interface Layout {
    @Composable
    fun ColumnScope.Render(options: KeyboardOptions, callback: Lp3KeyboardCallback)
}

object LowerCaseLayout : Layout, AlphabetLayout(
    listOf(
        "qwertyuiop",
        "asdfghjkl",
        "zxcvbnm"
    )
)

object UpperCaseLayout : Layout, AlphabetLayout(
    listOf(
        "QWERTYUIOP",
        "ASDFGHJKL",
        "ZXCVBNM"
    )
)

open class AlphabetLayout(private val letters: List<String>) {

    @Composable
    fun ColumnScope.Render(options: KeyboardOptions, callback: Lp3KeyboardCallback) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.Center,
        ) {
            for (char in letters[0]) {
                Key(char, callback)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.Center,
        ) {
            for (char in letters[1]) {
                Key(char, callback)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.Center,
        ) {
            // temporary shift/caps lock key
            Key('^', callback, SpecialKey.Caps)
            for (char in letters[2]) {
                Key(char, callback)
            }
        }
    }
}

@Preview(widthDp = (1080 / 3), heightDp = (1240 / 3))
@Composable
fun Lp3KeyboardPreview() {
    val callback = object : Lp3KeyboardCallback {
        override fun onKeyPressed(code: Int) = Unit
        override fun onSpecialKeyPressed(key: SpecialKey) = Unit
        override fun onKeyReleased(code: Int) = Unit
        override fun onSpecialKeyReleased(key: SpecialKey) = Unit
        override fun onKeyLongPressed(code: Int) = Unit
        override fun onSpecialKeyLongPressed(key: SpecialKey) = Unit
    }
    Column(verticalArrangement = Arrangement.Bottom, modifier = Modifier.fillMaxSize()) {
        val options = KeyboardOptions(emptyList(), true, true)
        Lp3Keyboard(LowerCaseLayout, options, callback)
    }
}