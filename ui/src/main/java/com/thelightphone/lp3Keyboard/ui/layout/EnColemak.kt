package com.thelightphone.lp3Keyboard.ui.layout

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import com.thelightphone.lp3Keyboard.ui.FinalRow
import com.thelightphone.lp3Keyboard.ui.FirstRow
import com.thelightphone.lp3Keyboard.ui.ICON_KEY_WIDTH_DP
import com.thelightphone.lp3Keyboard.ui.IconKey
import com.thelightphone.lp3Keyboard.ui.KeyboardOptions
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardCallback
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardLayoutCapture
import com.thelightphone.lp3Keyboard.ui.MultiLabelKey
import com.thelightphone.lp3Keyboard.ui.R
import com.thelightphone.lp3Keyboard.ui.SecondRow
import com.thelightphone.lp3Keyboard.ui.SpecialKey
import com.thelightphone.lp3Keyboard.ui.ThirdRow

private val EnColemakSwipeConfig: SwipeConfig by lazy {
    object : Lp3KeyboardLayoutCapture("abcdefghijklmnopqrstuvwxyz") {
        override fun report(code: Int, bounds: Rect) {
            val lower = if (code in 'A'.code..'Z'.code) code + 32 else code
            if (lower !in 'a'.code..'z'.code) return
            // onGloballyPositioned fires on every layout pass; skip identical
            // writes so we don't churn the snapshot or re-fire boundsFlow.
            if (letterBounds[lower] == bounds) return
            letterBounds[lower] = bounds
        }
    }
}

/**
 * The layouts for English Colemak.
 *
 * "Colemak is a modern alternative to the QWERTY and Dvorak layouts, designed for efficient and
 * ergonomic touch typing in English."
 *
 * See https://colemak.com
 *
 * To keep the top row right-aligned to avoid a strangly skewed layout, the upper right key is
 * filled with `'` lower/caps and `"` shifted. This is where `;` is on Colemak, but as that's a
 * rarely used key, one of the most common symbols is used. For analysis and rational, see
 * https://github.com/lightphone/light-keyboard/pull/4#pullrequestreview-4675526031
 */
object EnColemak {
    object LowerCaseLayout : Layout {
        override val isRootLayout: Boolean
            get() = true

        override val swipeConfig: SwipeConfig
            get() = EnColemakSwipeConfig

        @Composable
        override fun ColumnScope.Render(
            options: KeyboardOptions,
            callback: Lp3KeyboardCallback
        ) {
            FirstRow("qwfpgjluy'", callback, swipeConfig, options.enableKeyAnimation)
            SecondRow("arstdhneio", callback, swipeConfig, options.enableKeyAnimation)
            ThirdRow("zxcvbkm", callback, swipeConfig, options) {
                IconKey(
                    R.drawable.up_lp3,
                    SpecialKey.UpCase,
                    callback,
                    options.enableKeyAnimation,
                    width = ICON_KEY_WIDTH_DP.dp,
                    modifier = Modifier.padding(12.dp).padding(bottom = 6.dp, end = 8.dp)
                )
            }
            FinalRow(options, callback) {
                MultiLabelKey("123", SpecialKey.Numbers, callback, options.enableKeyAnimation)
            }
        }
    }

    object CapsLockedLayout : Layout {
        override val isRootLayout: Boolean
            get() = true
        override val swipeConfig: SwipeConfig
            get() = EnColemakSwipeConfig

        @Composable
        override fun ColumnScope.Render(
            options: KeyboardOptions,
            callback: Lp3KeyboardCallback
        ) {
            FirstRow("QWFPGJLUY'", callback, swipeConfig, options.enableKeyAnimation)
            SecondRow("ARSTDHNEIO", callback, swipeConfig, options.enableKeyAnimation)
            ThirdRow("ZXCVBKM", callback, swipeConfig, options) {
                IconKey(
                    R.drawable.caps_lp3,
                    SpecialKey.DownCase,
                    callback,
                    options.enableKeyAnimation,
                    width = ICON_KEY_WIDTH_DP.dp,
                    modifier = Modifier.padding(9.dp).padding(bottom = 2.dp, end = 4.dp)
                )
            }
            FinalRow(options, callback) {
                MultiLabelKey("123", SpecialKey.Numbers, callback, options.enableKeyAnimation)
            }
        }
    }

    object UpperCaseLayout : Layout {
        override val isRootLayout: Boolean
            get() = true
        override val swipeConfig: SwipeConfig
            get() = EnColemakSwipeConfig

        @Composable
        override fun ColumnScope.Render(
            options: KeyboardOptions,
            callback: Lp3KeyboardCallback
        ) {
            FirstRow("QWFPGJLUY\"", callback, swipeConfig, options.enableKeyAnimation)
            SecondRow("ARSTDHNEIO", callback, swipeConfig, options.enableKeyAnimation)
            ThirdRow("ZXCVBKM", callback, swipeConfig, options) {
                IconKey(
                    R.drawable.down_lp3,
                    SpecialKey.DownCase,
                    callback,
                    options.enableKeyAnimation,
                    width = ICON_KEY_WIDTH_DP.dp,
                    modifier = Modifier.padding(12.dp).padding(bottom = 6.dp, end = 8.dp)
                )
            }
            FinalRow(options, callback) {
                MultiLabelKey("123", SpecialKey.Numbers, callback, options.enableKeyAnimation)
            }
        }
    }
}
