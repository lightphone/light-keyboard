package com.thelightphone.lp3Keyboard.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.text.iterator

enum class SpecialKey {
    UpCase,
    DownCase,
    Backspace,
    Space,
    Letters,
    Numbers,
    Symbols,
    Emojis,
    Submit,
    Close,
    Voice,
    Return
}

interface Lp3KeyboardCallback {
    fun onKeyPressed(code: Int)
    fun onSpecialKeyPressed(key: SpecialKey)
    fun onKeyReleased(code: Int)
    fun onSpecialKeyReleased(key: SpecialKey)
    fun onKeyLongPressed(code: Int)
    fun onSpecialKeyLongPressed(key: SpecialKey)
}

const val LP3_KEYBOARD_HEIGHT_DP = 164
const val STANDARD_KEY_WIDTH_DP = 34
const val ICON_KEY_WIDTH_DP = STANDARD_KEY_WIDTH_DP + 14
const val MEDIUM_KEY_WIDTH_DP = STANDARD_KEY_WIDTH_DP + 8
const val STANDARD_ROW_HEIGHT_DP = 42
const val STANDARD_KEY_TEXT_SP = 24

@Composable
fun Lp3Keyboard(layout: Layout, options: KeyboardOptions, callback: Lp3KeyboardCallback) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(LP3_KEYBOARD_HEIGHT_DP.dp)
            .background(LocalKeyboardColors.current.background)
    ) {
        Column(Modifier.fillMaxSize().padding(top = 4.dp).align(Alignment.Center)) {
            with(layout) { Render(options, callback) }
        }
    }
}

@Composable
fun RowScope.IconKey(
    @DrawableRes drawable: Int,
    key: SpecialKey,
    callback: Lp3KeyboardCallback,
    modifier: Modifier = Modifier,
    width: Dp = STANDARD_KEY_WIDTH_DP.dp
) {
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .pointerInput(key) {
                detectTapGestures(
                    onPress = {
                        callback.onSpecialKeyPressed(key)
                        tryAwaitRelease()
                        callback.onSpecialKeyReleased(key)
                    },
                    onLongPress = { callback.onSpecialKeyLongPressed(key) },
                )
            }
            .then(modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(painterResource(drawable), contentDescription = "TODO", tint = LocalKeyboardColors.current.foreground)
    }
}


@Composable
fun RowScope.SpaceBar(callback: Lp3KeyboardCallback, width: Dp) {
    Box(
        Modifier
            .fillMaxHeight()
            .width(width)
            .padding(bottom = 6.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        callback.onSpecialKeyPressed(SpecialKey.Space)
                        tryAwaitRelease()
                        callback.onSpecialKeyReleased(SpecialKey.Space)
                    },
                    onLongPress = { callback.onSpecialKeyLongPressed(SpecialKey.Space) },
                )
            }
    ) {
        Box(
            Modifier
                .height(2.dp)
                .background(LocalKeyboardColors.current.foreground)
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun RowScope.Key(
    char: Char,
    callback: Lp3KeyboardCallback,
    override: SpecialKey? = null
) = Key(char.code, callback, override)

@Composable
fun RowScope.Key(
    code: Int,
    callback: Lp3KeyboardCallback,
    override: SpecialKey? = null,
    width: Dp = STANDARD_KEY_WIDTH_DP.dp
) {
    var pressed by remember { mutableStateOf(false) }

    val onPressed = override
        ?.let { { callback.onSpecialKeyPressed(it) } }
        ?: { callback.onKeyPressed(code) }

    val onReleased = override
        ?.let { { callback.onSpecialKeyReleased(it) } }
        ?: { callback.onKeyReleased(code) }

    val onLongPressed = override
        ?.let { { callback.onSpecialKeyLongPressed(it) } }
        ?: { callback.onKeyLongPressed(code) }

    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .pointerInput(code) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        onPressed()
                        tryAwaitRelease()
                        pressed = false
                        onReleased()
                    },
                    onLongPress = { onLongPressed() },
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val fontSize = if (pressed) (STANDARD_KEY_TEXT_SP + 6).sp else STANDARD_KEY_TEXT_SP.sp
        val offsetY = if (pressed) (-12).dp else 0.dp
        Text(
            text = buildString { appendCodePoint(code) },
            color = LocalKeyboardColors.current.foreground,
            fontFamily = akkuratFamily,
            fontWeight = FontWeight.Normal,
            fontSize = fontSize,
            modifier = Modifier.offset(y = offsetY)
        )
    }
}

@Composable
fun RowScope.MultiLabelKey(
    labelText: String,
    key: SpecialKey,
    callback: Lp3KeyboardCallback
) {
    Box(
        modifier = Modifier
            .width(ICON_KEY_WIDTH_DP.dp)
            .fillMaxHeight()
            .pointerInput(labelText) {
                detectTapGestures(
                    onPress = {
                        callback.onSpecialKeyPressed(key)
                        tryAwaitRelease()
                        callback.onSpecialKeyReleased(key)
                    },
                    onLongPress = { callback.onSpecialKeyLongPressed(key) },
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = labelText,
            color = LocalKeyboardColors.current.foreground,
            fontFamily = akkuratFamily,
            fontWeight = FontWeight.Normal,
            letterSpacing = 2.sp,
            fontSize = 16.sp
        )
    }
}

typealias Emoji = Int

data class KeyboardOptions(
    val emojis: List<Emoji>?,
    val displayClose: Boolean,
    val displayReturn: Boolean,
    val displayVoice: Boolean
)

@Composable
fun ColumnScope.FirstRow(characters: String, callback: Lp3KeyboardCallback) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(STANDARD_ROW_HEIGHT_DP.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        for (char in characters) {
            Key(char, callback)
        }
    }
}

@Composable
fun ColumnScope.SecondRow(characters: String, callback: Lp3KeyboardCallback) {
    // same style as first row on all keyboards
    FirstRow(characters, callback)
}

@Composable
fun ColumnScope.ThirdRow(
    characters: String,
    callback: Lp3KeyboardCallback,
    leftButton: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(STANDARD_ROW_HEIGHT_DP.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        leftButton()
        if (characters.length == 5) {
            // currently this row only has 5 or 7 chars, so add some space if there are 5
            Spacer(Modifier.width(MEDIUM_KEY_WIDTH_DP.dp))
        }
        for (char in characters) {
            Key(char, callback)
        }
        if (characters.length == 5) {
            Spacer(Modifier.width(STANDARD_KEY_WIDTH_DP.dp))
        }
        IconKey(
            R.drawable.back_lp3,
            SpecialKey.Backspace,
            callback,
            width = ICON_KEY_WIDTH_DP.dp,
            modifier = Modifier.padding(10.dp).padding(start = 8.dp, bottom = 6.dp)
        )
    }
}

@Composable
fun ColumnScope.FinalRow(
    options: KeyboardOptions,
    callback: Lp3KeyboardCallback,
    leftButton: @Composable RowScope.() -> Unit
) {
    // TODO take in left-most button
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .height((STANDARD_ROW_HEIGHT_DP - 20).dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        val iconKeyWidth = STANDARD_KEY_WIDTH_DP + 12
        leftButton()
        if (!options.emojis.isNullOrEmpty()) {
            IconKey(
                R.drawable.smile,
                SpecialKey.Emojis,
                callback,
                width = iconKeyWidth.dp,
                modifier = Modifier.padding(horizontal = 6.dp).padding(end = 14.dp)
            )
        } else {
            Spacer(Modifier.width(iconKeyWidth.dp))
        }
        SpaceBar(callback, 155.dp)
        if (options.displayReturn) {
            IconKey(
                R.drawable.return_lp3,
                SpecialKey.Return,
                callback,
                width = iconKeyWidth.dp,
                modifier = Modifier.padding(top = 10.dp, start = 22.dp, end = 0.dp)
            )
        } else {
            Spacer(Modifier.width(iconKeyWidth.dp))
        }

        if (options.displayVoice) {
            IconKey(
                R.drawable.microphone_lp3,
                SpecialKey.Voice,
                callback,
                width = iconKeyWidth.dp,
                modifier = Modifier.padding(top = 2.dp, start = 14.dp, end = 4.dp)
            )
        } else {
            Spacer(Modifier.width(iconKeyWidth.dp))
        }
    }
}

private val previewCallback = object : Lp3KeyboardCallback {
    override fun onKeyPressed(code: Int) = Unit
    override fun onSpecialKeyPressed(key: SpecialKey) = Unit
    override fun onKeyReleased(code: Int) = Unit
    override fun onSpecialKeyReleased(key: SpecialKey) = Unit
    override fun onKeyLongPressed(code: Int) = Unit
    override fun onSpecialKeyLongPressed(key: SpecialKey) = Unit
}

@Preview(name = "Dark", widthDp = (1080 / 3), heightDp = (1240 / 3))
@Composable
fun Lp3KeyboardDarkPreview() {
    Lp3KeyboardTheme(DarkKeyboardColors) {
        Column(verticalArrangement = Arrangement.Bottom, modifier = Modifier.fillMaxSize()) {
            val options = KeyboardOptions(defaultEmojis, true, true, true)
            Lp3KeyboardExtended(EmojiLayout, options, previewCallback)
        }
    }
}

@Preview(name = "Light", widthDp = (1080 / 3), heightDp = (1240 / 3))
@Composable
fun Lp3KeyboardLightPreview() {
    Lp3KeyboardTheme(LightKeyboardColors) {
        Column(verticalArrangement = Arrangement.Bottom, modifier = Modifier.fillMaxSize()) {
            val options = KeyboardOptions(defaultEmojis, true, true, true)
            Lp3KeyboardExtended(UpperCaseLayout, options, previewCallback)
        }
    }
}