package com.thelightphone.lp3Keyboard.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

sealed interface Layout {
    @Composable
    fun ColumnScope.Render(options: KeyboardOptions, callback: Lp3KeyboardCallback)
}

object LowerCaseLayout : Layout {
    @Composable
    override fun ColumnScope.Render(
        options: KeyboardOptions,
        callback: Lp3KeyboardCallback
    ) {
        FirstRow("qwertyuiop", callback)
        SecondRow("asdfghjkl", callback)
        ThirdRow("zxcvbnm", callback) {
            IconKey(
                R.drawable.up_lp3,
                SpecialKey.UpCase,
                callback,
                width = ICON_KEY_WIDTH_DP.dp,
                modifier = Modifier.padding(12.dp).padding(bottom = 6.dp, end = 8.dp)
            )
        }
        FinalRow(options, callback) {
            MultiLabelKey("123", SpecialKey.Numbers, callback)
        }
    }
}

object CapsLockedLayout : Layout {
    @Composable
    override fun ColumnScope.Render(
        options: KeyboardOptions,
        callback: Lp3KeyboardCallback
    ) {
        FirstRow("QWERTYUIOP", callback)
        SecondRow("ASDFGHJKL", callback)
        ThirdRow("ZXCVBNM", callback) {
            IconKey(
                R.drawable.caps_lp3,
                SpecialKey.DownCase,
                callback,
                width = ICON_KEY_WIDTH_DP.dp,
                modifier = Modifier.padding(9.dp).padding(bottom = 2.dp, end = 4.dp)
            )
        }
        FinalRow(options, callback) {
            MultiLabelKey("123", SpecialKey.Numbers, callback)
        }
    }
}

object UpperCaseLayout : Layout {
    @Composable
    override fun ColumnScope.Render(
        options: KeyboardOptions,
        callback: Lp3KeyboardCallback
    ) {
        FirstRow("QWERTYUIOP", callback)
        SecondRow("ASDFGHJKL", callback)
        ThirdRow("ZXCVBNM", callback) {
            IconKey(
                R.drawable.down_lp3,
                SpecialKey.DownCase,
                callback,
                width = ICON_KEY_WIDTH_DP.dp,
                modifier = Modifier.padding(12.dp).padding(bottom = 6.dp, end = 8.dp)
            )
        }
        FinalRow(options, callback) {
            MultiLabelKey("123", SpecialKey.Numbers, callback)
        }
    }
}

object NumberLayout : Layout {
    @Composable
    override fun ColumnScope.Render(
        options: KeyboardOptions,
        callback: Lp3KeyboardCallback
    ) {
        FirstRow("1234567890", callback)
        SecondRow("-/:;()$&@\"", callback)
        ThirdRow(".,?!'", callback) {
            MultiLabelKey("#+=", SpecialKey.Symbols, callback)
        }
        FinalRow(options, callback) {
            MultiLabelKey("ABC", SpecialKey.Letters, callback)
        }
    }
}

object SymbolsLayout : Layout {
    @Composable
    override fun ColumnScope.Render(
        options: KeyboardOptions,
        callback: Lp3KeyboardCallback
    ) {
        FirstRow("[]{}#%^*+=", callback)
        SecondRow("_\\|~<>€£¥", callback)
        ThirdRow(".,?!'", callback) {
            MultiLabelKey("123", SpecialKey.Numbers, callback)
        }
        FinalRow(options, callback) {
            MultiLabelKey("ABC", SpecialKey.Letters, callback)
        }
    }
}

object EmojiLayout : Layout {
    @Composable
    override fun ColumnScope.Render(
        options: KeyboardOptions,
        callback: Lp3KeyboardCallback
    ) {
        // current layout supports 3 rows of 8
        val emojiRows = options.emojis?.chunked(8)?.take(3) ?: return
        for (row in emojiRows) {
            DefaultRow {
                for (emoji in row) {
                    Key(emoji, callback, width = MEDIUM_KEY_WIDTH_DP.dp)
                }
            }
        }
    }
}

class ExtendedCharKeyboard(rootCode: Int) : Layout {
    private val rows = extendedCharMapping[rootCode]
    @Composable
    override fun ColumnScope.Render(
        options: KeyboardOptions,
        callback: Lp3KeyboardCallback
    ) {
        rows?.forEach { rowKeys ->
            DefaultRow {
                for (char in rowKeys) {
                    Key(char.code, callback, width = MEDIUM_KEY_WIDTH_DP.dp)
                }
            }
        }
    }
}

val extendedCharMapping = mapOf(
    'A'.code to listOf(
        listOf('À', 'Á', 'Â', 'Ä', 'Æ'),
        listOf('Ã', 'Å', 'Ā', 'Ă', 'Ą'),
    ),
    'a'.code to listOf(
        listOf('à', 'á', 'â', 'ä', 'æ'),
        listOf('ã', 'å', 'ā', 'ă', 'ą'),
    ),
    'C'.code to listOf(
        listOf('Ç', 'Ć', 'Č'),
    ),
    'c'.code to listOf(
        listOf('ç', 'ć', 'č'),
    ),
    'E'.code to listOf(
        listOf('È', 'É', 'Ê', 'Ë', 'Ē', 'Ė', 'Ę'),
    ),
    'e'.code to listOf(
        listOf('è', 'é', 'ê', 'ë', 'ē', 'ė', 'ę'),
    ),
    'I'.code to listOf(
        listOf('Î', 'Ï', 'Í', 'Ī', 'Į', 'Ì'),
    ),
    'i'.code to listOf(
        listOf('î', 'ï', 'í', 'ī', 'į', 'ì'),
    ),
    'L'.code to listOf(
        listOf('Ł'),
    ),
    'l'.code to listOf(
        listOf('ł'),
    ),
    'N'.code to listOf(
        listOf('Ñ', 'Ń'),
    ),
    'n'.code to listOf(
        listOf('ñ', 'ń'),
    ),
    'O'.code to listOf(
        listOf('Ô', 'Ö', 'Ò', 'Ó', 'Œ', 'Ø', 'Ō', 'Õ'),
    ),
    'o'.code to listOf(
        listOf('ô', 'ö', 'ò', 'ó', 'œ', 'ø', 'ō', 'õ'),
    ),
    'S'.code to listOf(
        listOf('ẞ', 'Ś', 'Š'),
    ),
    's'.code to listOf(
        listOf('ß', 'ś', 'š'),
    ),
    'U'.code to listOf(
        listOf('Û', 'Ü', 'Ù', 'Ú', 'Ū'),
    ),
    'u'.code to listOf(
        listOf('û', 'ü', 'ù', 'ú', 'ū'),
    ),
    'Y'.code to listOf(
        listOf('Ÿ'),
    ),
    'y'.code to listOf(
        listOf('ÿ'),
    ),
    'Z'.code to listOf(
        listOf('Ž', 'Ź', 'Ż'),
    ),
    'z'.code to listOf(
        listOf('ž', 'ź', 'ż'),
    ),
)