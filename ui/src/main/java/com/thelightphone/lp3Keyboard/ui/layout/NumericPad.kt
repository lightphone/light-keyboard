package com.thelightphone.lp3Keyboard.ui.layout

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.thelightphone.lp3Keyboard.ui.DefaultRow
import com.thelightphone.lp3Keyboard.ui.Key
import com.thelightphone.lp3Keyboard.ui.KeyboardOptions
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardCallback
import com.thelightphone.lp3Keyboard.ui.R
import com.thelightphone.lp3Keyboard.ui.SpecialKey
import com.thelightphone.lp3Keyboard.ui.IconKey

/**
 * Numbers-only layout, arranged like the LP3 lockscreen passcode pad:
 * 1-2-3 / 4-5-6 / 7-8-9 / 0 centered, backspace bottom-right.
 *
 * Keys use the native dialpad's 80 dp pitch (240 px on the LP3). Rows are
 * 40 dp (not the standard 44 dp) so all four rows fit the fixed
 * LP3_KEYBOARD_HEIGHT_DP box: 4 top padding + 4 x 40 = 164 dp.
 */
object NumericPadLayout : Layout {
    override val isRootLayout: Boolean
        get() = true

    private const val KEY_WIDTH_DP = 80
    private const val ROW_HEIGHT_DP = 40

    @Composable
    override fun ColumnScope.Render(
        options: KeyboardOptions,
        callback: Lp3KeyboardCallback
    ) {
        DefaultRow(height = ROW_HEIGHT_DP.dp) {
            Key('1'.code, callback, null, options.enableKeyAnimation, width = KEY_WIDTH_DP.dp)
            Key('2'.code, callback, null, options.enableKeyAnimation, width = KEY_WIDTH_DP.dp)
            Key('3'.code, callback, null, options.enableKeyAnimation, width = KEY_WIDTH_DP.dp)
        }
        DefaultRow(height = ROW_HEIGHT_DP.dp) {
            Key('4'.code, callback, null, options.enableKeyAnimation, width = KEY_WIDTH_DP.dp)
            Key('5'.code, callback, null, options.enableKeyAnimation, width = KEY_WIDTH_DP.dp)
            Key('6'.code, callback, null, options.enableKeyAnimation, width = KEY_WIDTH_DP.dp)
        }
        DefaultRow(height = ROW_HEIGHT_DP.dp) {
            Key('7'.code, callback, null, options.enableKeyAnimation, width = KEY_WIDTH_DP.dp)
            Key('8'.code, callback, null, options.enableKeyAnimation, width = KEY_WIDTH_DP.dp)
            Key('9'.code, callback, null, options.enableKeyAnimation, width = KEY_WIDTH_DP.dp)
        }
        DefaultRow(height = ROW_HEIGHT_DP.dp) {
            Spacer(Modifier.width(KEY_WIDTH_DP.dp))
            Key('0'.code, callback, null, options.enableKeyAnimation, width = KEY_WIDTH_DP.dp)
            IconKey(
                R.drawable.back_lp3,
                SpecialKey.Backspace,
                callback,
                options.enableKeyAnimation,
                width = KEY_WIDTH_DP.dp,
                iconSize = DpSize(12.dp, 20.dp)
            )
        }
    }
}
