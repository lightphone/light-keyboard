package com.thelightphone.lp3Keyboard.ui.layout

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.thelightphone.lp3Keyboard.ui.FinalRow
import com.thelightphone.lp3Keyboard.ui.FirstRow
import com.thelightphone.lp3Keyboard.ui.KeyboardOptions
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardCallback
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardLayoutCapture
import com.thelightphone.lp3Keyboard.ui.MultiLabelKey
import com.thelightphone.lp3Keyboard.ui.SecondRow
import com.thelightphone.lp3Keyboard.ui.SpecialKey
import com.thelightphone.lp3Keyboard.ui.ThirdRow

private val ArStandardSwipeConfig: SwipeConfig by lazy {
    object : Lp3KeyboardLayoutCapture(ArStandard.ALPHABET) {
        private val codes = letters.map { it.code }.toSet()

        override fun report(code: Int, bounds: Rect) {
            if (code !in codes) return
            // onGloballyPositioned fires on every layout pass; skip identical
            // writes so we don't churn the snapshot or re-fire boundsFlow.
            if (letterBounds[code] == bounds) return
            letterBounds[code] = bounds
        }
    }
}

/**
 * The layout for standard Arabic.
 *
 * Rows are written in logical order — the first character of each string is the *rightmost* key,
 * matching how the row reads on screen, since the layout renders right-to-left.
 *
 * The standard (Windows/iOS) Arabic layout needs 12/11/10 keys per row. We only have room for 10
 * at the LP3's key width, so the first two rows are the standard rows truncated to 10, which keeps
 * every one of those 20 keys in its usual spot, and the letters that fall off the ends (ج د ط, plus
 * ذ from the backtick key) join ر و ز ظ on the third row. Arabic is caseless, so the shift slot is
 * free and the third row holds 8 letters instead of 7.
 *
 * Hamza forms and the other non-alphabet characters — أ إ آ ء ٱ ة ى ئ ؤ — are on long-press,
 * see [ArShared.extendedCharMapping].
 */
object ArStandard {
    const val FIRST_ROW = "ضصثقفغعهخح"
    const val SECOND_ROW = "شسيبلاتنمك"
    const val THIRD_ROW = "روزظذطدج"

    /** The 28 letters of the Arabic alphabet, in alphabetical order. */
    const val ALPHABET = "ابتثجحخدذرزسشصضطظعغفقكلمنهوي"

    object LettersLayout : Layout {
        override val isRootLayout: Boolean
            get() = true

        override val swipeConfig: SwipeConfig
            get() = ArStandardSwipeConfig

        override val layoutDirection: LayoutDirection
            get() = LayoutDirection.Rtl

        @Composable
        override fun ColumnScope.Render(
            options: KeyboardOptions,
            callback: Lp3KeyboardCallback
        ) {
            FirstRow(FIRST_ROW, callback, swipeConfig, options.enableKeyAnimation)
            SecondRow(SECOND_ROW, callback, swipeConfig, options.enableKeyAnimation)
            // No shift key — the slot opposite backspace stays empty.
            ThirdRow(THIRD_ROW, callback, swipeConfig, options) {}
            // The utility row stays left-to-right: Android draws its own hide-keyboard chevron
            // in the bottom-left corner and IME switcher in the bottom-right, and mirroring
            // this row parks our mic and 123 keys right on top of them.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                FinalRow(options, callback) {
                    MultiLabelKey("123", SpecialKey.Numbers, callback, options.enableKeyAnimation)
                }
            }
        }
    }
}
