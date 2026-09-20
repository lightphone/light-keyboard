package com.thelightphone.lp3Keyboard.ui.layout

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.thelightphone.lp3Keyboard.ui.DefaultRow
import com.thelightphone.lp3Keyboard.ui.FinalRow
import com.thelightphone.lp3Keyboard.ui.ICON_KEY_WIDTH_DP
import com.thelightphone.lp3Keyboard.ui.IconKey
import com.thelightphone.lp3Keyboard.ui.Key
import com.thelightphone.lp3Keyboard.ui.KeyboardOptions
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardCallback
import com.thelightphone.lp3Keyboard.ui.MultiLabelKey
import com.thelightphone.lp3Keyboard.ui.R
import com.thelightphone.lp3Keyboard.ui.SpecialKey

// Standard MOE bopomofo layout (the default on most Windows/macOS/phone 注音
// IMEs), on the same four QWERTY-shaped rows as EnQwerty.kt so it's familiar to
// Taiwanese typists:
//   number row (11): ㄅㄉˇˋㄓˊ˙ㄚㄞㄢㄦ
//   qwertyuiop  (10): ㄆㄊㄍㄐㄔㄗㄧㄛㄟㄣ
//   asdfghjkl    (9): ㄇㄋㄎㄑㄕㄖㄨㄜㄠ
//   zxcvbnm+;,./(11): ㄈㄌㄏㄒㄘㄙㄩㄤㄝㄡㄥ
// Symbol/tone keys feed the composer (see ZhuyinLp3KeyboardViewModel) rather
// than committing directly; the "EN" key switches to the English alphabet
// layout, and long-pressing "123" there comes back here.
object ZhuyinLayout : Layout {
    override val isRootLayout: Boolean
        get() = true

    @Composable
    override fun ColumnScope.Render(
        options: KeyboardOptions,
        callback: Lp3KeyboardCallback
    ) {
        DefaultRow {
            for (char in "ㄅㄉˇˋㄓˊ˙ㄚㄞㄢㄦ") {
                Key(char, callback, swipeConfig = null, enableKeyAnimation = options.enableKeyAnimation)
            }
        }
        DefaultRow {
            for (char in "ㄆㄊㄍㄐㄔㄗㄧㄛㄟㄣ") {
                Key(char, callback, swipeConfig = null, enableKeyAnimation = options.enableKeyAnimation)
            }
        }
        DefaultRow {
            for (char in "ㄇㄋㄎㄑㄕㄖㄨㄜㄠ") {
                Key(char, callback, swipeConfig = null, enableKeyAnimation = options.enableKeyAnimation)
            }
        }
        DefaultRow {
            for (char in "ㄈㄌㄏㄒㄘㄙㄩㄤㄝㄡㄥ") {
                Key(char, callback, swipeConfig = null, enableKeyAnimation = options.enableKeyAnimation)
            }
            IconKey(
                R.drawable.back_lp3,
                SpecialKey.Backspace,
                callback,
                options.enableKeyAnimation,
                width = ICON_KEY_WIDTH_DP.dp
            )
        }
        FinalRow(options, callback) {
            // Round-trips to the English alphabet layout, mirroring how
            // NumberLayout/SymbolsLayout use SpecialKey.Letters to return to
            // the lower/upper/caps-locked layouts.
            MultiLabelKey("EN", SpecialKey.Letters, callback, options.enableKeyAnimation)
        }
    }
}
