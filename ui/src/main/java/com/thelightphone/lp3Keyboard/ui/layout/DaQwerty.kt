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
import com.thelightphone.lp3Keyboard.ui.NarrowThirdRow
import com.thelightphone.lp3Keyboard.ui.R
import com.thelightphone.lp3Keyboard.ui.SecondRow
import com.thelightphone.lp3Keyboard.ui.SpecialKey

private val DaQwertySwipeConfig: SwipeConfig by lazy {
    object : Lp3KeyboardLayoutCapture("abcdefghijklmnopqrstuvwxyzæøå") {
        override fun report(code: Int, bounds: Rect) {
            val lower = code.toChar().lowercaseChar()
            if (lower !in letters) return
            // onGloballyPositioned fires on every layout pass; skip identical
            // writes so we don't churn the snapshot or re-fire boundsFlow.
            if (letterBounds[lower.code] == bounds) return
            letterBounds[lower.code] = bounds
        }
    }
}


/** The layouts for Danish QWERTY. æ comes before ø; Norwegian has them the other way round. */
object DaQwerty {
    object LowerCaseLayout : Layout {
        override val isRootLayout: Boolean
            get() = true

        override val swipeConfig: SwipeConfig
            get() = DaQwertySwipeConfig

        @Composable
        override fun ColumnScope.Render(
            options: KeyboardOptions,
            callback: Lp3KeyboardCallback
        ) {
            FirstRow("qwertyuiopå", callback, swipeConfig, options.enableKeyAnimation)
            SecondRow("asdfghjklæø", callback, swipeConfig, options.enableKeyAnimation)
            NarrowThirdRow("zxcvbnm", callback, swipeConfig, options) {
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
            get() = DaQwertySwipeConfig

        @Composable
        override fun ColumnScope.Render(
            options: KeyboardOptions,
            callback: Lp3KeyboardCallback
        ) {
            FirstRow("QWERTYUIOPÅ", callback, swipeConfig, options.enableKeyAnimation)
            SecondRow("ASDFGHJKLÆØ", callback, swipeConfig, options.enableKeyAnimation)
            NarrowThirdRow("ZXCVBNM", callback, swipeConfig, options) {
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
            get() = DaQwertySwipeConfig

        @Composable
        override fun ColumnScope.Render(
            options: KeyboardOptions,
            callback: Lp3KeyboardCallback
        ) {
            FirstRow("QWERTYUIOPÅ", callback, swipeConfig, options.enableKeyAnimation)
            SecondRow("ASDFGHJKLÆØ", callback, swipeConfig, options.enableKeyAnimation)
            NarrowThirdRow("ZXCVBNM", callback, swipeConfig, options) {
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
