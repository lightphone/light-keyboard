package com.thelightphone.lp3Keyboard.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

val akkuratFamily = FontFamily(
    Font(R.font.akkuratll_light, FontWeight.Light),
    Font(R.font.akkuratpro_bold, FontWeight.Bold),
    Font(R.font.akkuratll_regular, FontWeight.Normal),
)

@Immutable
data class Lp3KeyboardColors(
    val background: Color,
    val foreground: Color,
)

val DarkKeyboardColors = Lp3KeyboardColors(
    background = Color.Black,
    foreground = Color.White,
)

val LightKeyboardColors = Lp3KeyboardColors(
    background = Color.White,
    foreground = Color.Black,
)

val LocalKeyboardColors = staticCompositionLocalOf { DarkKeyboardColors }

@Composable
fun Lp3KeyboardTheme(
    colors: Lp3KeyboardColors = DarkKeyboardColors,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalKeyboardColors provides colors) {
        content()
    }
}