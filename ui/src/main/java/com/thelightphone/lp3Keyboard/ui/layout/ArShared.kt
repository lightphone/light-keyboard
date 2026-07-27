package com.thelightphone.lp3Keyboard.ui.layout

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.thelightphone.lp3Keyboard.ui.FinalRow
import com.thelightphone.lp3Keyboard.ui.FirstRow
import com.thelightphone.lp3Keyboard.ui.KeyboardOptions
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardCallback
import com.thelightphone.lp3Keyboard.ui.MultiLabelKey
import com.thelightphone.lp3Keyboard.ui.SecondRow
import com.thelightphone.lp3Keyboard.ui.SpecialKey
import com.thelightphone.lp3Keyboard.ui.ThirdRow

/**
 * Layouts and data generally shared across Arabic keyboards.
 *
 * These mirror [EnShared], with Arabic punctuation (، ؛ ؟) in place of the Latin equivalents.
 * Digits stay Western (1234567890) — they're what phone numbers, codes, and most modern Arabic
 * text use, and Arabic-Indic digits break numeric fields in a lot of apps.
 *
 * Rows that must read left-to-right — digits, paired symbols, and the utility row, which stays
 * put so it doesn't collide with the system's corner buttons — override the RTL direction.
 */
object ArShared {
    /** Label for the key that returns to the letters layout, the Arabic analogue of "ABC". */
    const val LETTERS_LABEL = "أبج"

    object NumberLayout : Layout {
        override val layoutDirection: LayoutDirection
            get() = LayoutDirection.Rtl

        @Composable
        override fun ColumnScope.Render(
            options: KeyboardOptions,
            callback: Lp3KeyboardCallback
        ) {
            // Digits and paired symbols read left-to-right even inside Arabic text, so only the
            // surrounding chrome mirrors.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                FirstRow("1234567890", callback, swipeConfig, options.enableKeyAnimation)
                SecondRow("-/:؛()\$&@\"", callback, swipeConfig, options.enableKeyAnimation)
            }
            ThirdRow(".،؟!'", callback, swipeConfig, options) {
                MultiLabelKey("#+=", SpecialKey.Symbols, callback, options.enableKeyAnimation)
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                FinalRow(options, callback) {
                    MultiLabelKey(
                        LETTERS_LABEL,
                        SpecialKey.Letters,
                        callback,
                        options.enableKeyAnimation
                    )
                }
            }
        }
    }

    object SymbolsLayout : Layout {
        override val layoutDirection: LayoutDirection
            get() = LayoutDirection.Rtl

        @Composable
        override fun ColumnScope.Render(
            options: KeyboardOptions,
            callback: Lp3KeyboardCallback
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                FirstRow("[]{}#%^*+=", callback, swipeConfig, options.enableKeyAnimation)
                SecondRow("_\\|~<>€£¥", callback, swipeConfig, options.enableKeyAnimation)
            }
            ThirdRow(".،؟!'", callback, swipeConfig, options) {
                MultiLabelKey("123", SpecialKey.Numbers, callback, options.enableKeyAnimation)
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                FinalRow(options, callback) {
                    MultiLabelKey(
                        LETTERS_LABEL,
                        SpecialKey.Letters,
                        callback,
                        options.enableKeyAnimation
                    )
                }
            }
        }
    }

    /**
     * Long-press alternates. Everything Arabic needs that isn't one of the 28 letters lives here:
     * the hamza forms on ا, ta marbuta on ه and ت, alef maqsura on ي.
     */
    val extendedCharMapping = mapOf(
        'ا'.code to listOf(
            listOf('أ', 'إ', 'آ', 'ء', 'ٱ'),
        ),
        'ه'.code to listOf(
            listOf('ة'),
        ),
        'ت'.code to listOf(
            listOf('ة'),
        ),
        'ي'.code to listOf(
            listOf('ى', 'ئ'),
        ),
        'و'.code to listOf(
            listOf('ؤ'),
        ),
    )
}
